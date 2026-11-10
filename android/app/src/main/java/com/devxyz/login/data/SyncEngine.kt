package com.devxyz.login.data

import com.devxyz.login.model.Folder
import com.devxyz.login.model.Profile
import com.devxyz.login.model.Store
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

/** Outcome of one sync pass. [store] is the merged store to persist. */
data class SyncReport(
    val store: Store,
    val pulled: Int = 0,
    val pushed: Int = 0,
    val failures: Int = 0,
    val authExpired: Boolean = false,
    val message: String = ""
) {
    val fullySynced: Boolean get() = failures == 0 && !authExpired
}

/**
 * Profile/folder sync against the DevxyzLogin server (v4).
 *
 * Merge rules (documented, deterministic — local data is never lost):
 *
 *  FOLDERS — server folders are created locally when unknown (matched via
 *  Store.folderServerIds); server renames win locally; local folders with
 *  no server counterpart are uploaded (POST /api/folders). Folder deletes
 *  are NOT propagated in v4 (no tombstone on either side to do it safely).
 *
 *  PROFILES — matched by Profile.serverId:
 *   - Local profile is CLEAN (dirty=false): the server copy wins,
 *     including its trash state — except per-device fields, which are
 *     always kept local: run `status`, `lastProxyCheck`.
 *   - Local profile is DIRTY: the local copy wins; it is pushed in the
 *     push phase (restoring it server-side first if the server has it
 *     trashed — that is exactly a local Restore).
 *   - Server profile unknown locally: imported (unless it is trashed on
 *     the server — deleted elsewhere, nothing to import).
 *   - Local profile whose serverId is GONE from the server (workspace
 *     reset / ephemeral store recycled): serverId is cleared and the
 *     profile is re-uploaded as new — re-uploading beats deleting the
 *     user's local data on suspicion.
 *   - New local profiles (serverId == null, not trashed) are uploaded
 *     with POST; the server assigns the id we keep as serverId. Trashed
 *     never-uploaded profiles stay local-only.
 *   - Locally trashed profiles with a serverId are soft-deleted on the
 *     server (DELETE) once; the local trash copy is kept (Restore
 *     profiles parity) with dirty cleared.
 *
 *  Uploads strip the client-local keys (serverId, dirty, lastProxyCheck)
 *  and the local id, translate folderIds into server folder ids, and
 *  force status="ready" (run state is per-device). The server PATCH is a
 *  deep-merge whose arrays replace wholesale, so a full-profile payload
 *  is exact.
 *
 *  Any failed call keeps the profile's dirty flag set; the next sync
 *  (launch / login / next mutation) retries. Sync never blocks the UI
 *  and never deletes local data.
 */
object SyncEngine {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val LOCAL_ONLY_KEYS = listOf("serverId", "dirty", "lastProxyCheck")

    suspend fun synchronize(api: ServerApi, session: AuthSession, store: Store): SyncReport {
        val token = session.token
        val ws = session.workspaceId
        var failures = 0
        var pulled = 0
        var pushed = 0

        var folders = store.folders
        var folderMap = store.folderServerIds
        var profiles = store.profiles

        fun snapshot() = store.copy(
            profiles = profiles, folders = folders, folderServerIds = folderMap
        )
        fun abort(authExpired: Boolean, message: String) = SyncReport(
            store = snapshot(), pulled = pulled, pushed = pushed,
            failures = failures + 1, authExpired = authExpired, message = message
        )

        // ---------------- folders ----------------
        when (val fr = api.listFoldersRaw(token, ws)) {
            is ApiResult.Failure -> {
                if (fr.kind == ApiResult.Kind.AUTH) return abort(true, fr.message)
                failures++ // keep going with the existing map; profiles still sync
            }
            is ApiResult.Success -> {
                val serverFolders = fr.data
                val mergedFolders = folders.toMutableList()
                val mergedMap = folderMap.toMutableMap()
                // server → local (create unknown, apply renames)
                for ((sid, sname) in serverFolders) {
                    val localId = mergedMap.entries.firstOrNull { it.value == sid }?.key
                    if (localId != null) {
                        val idx = mergedFolders.indexOfFirst { it.id == localId }
                        if (idx >= 0 && mergedFolders[idx].name != sname) {
                            mergedFolders[idx] = mergedFolders[idx].copy(name = sname)
                        }
                    } else {
                        val f = Folder(id = ProfileStore.newId(), name = sname)
                        mergedFolders += f
                        mergedMap[f.id] = sid
                    }
                }
                folders = mergedFolders
                folderMap = mergedMap
                // local → server (upload unmapped folders)
                for (f in folders) {
                    if (folderMap[f.id] == null) {
                        when (val cr = api.createFolder(token, ws, f.name)) {
                            is ApiResult.Success ->
                                folderMap = folderMap + (f.id to cr.data.first)
                            is ApiResult.Failure -> {
                                if (cr.kind == ApiResult.Kind.AUTH) return abort(true, cr.message)
                                failures++
                            }
                        }
                    }
                }
            }
        }

        // ---------------- pull profiles ----------------
        val activeRes = api.listProfilesRaw(token, ws, deleted = false)
        if (activeRes is ApiResult.Failure) {
            return if (activeRes.kind == ApiResult.Kind.AUTH) abort(true, activeRes.message)
            else abort(false, activeRes.message)
        }
        val trashRes = api.listProfilesRaw(token, ws, deleted = true)
        if (trashRes is ApiResult.Failure) {
            return if (trashRes.kind == ApiResult.Kind.AUTH) abort(true, trashRes.message)
            else abort(false, trashRes.message)
        }
        val activeObjs = (activeRes as ApiResult.Success).data
        val trashObjs = (trashRes as ApiResult.Success).data
        val trashIds = trashObjs.mapNotNull { it.str("id") }.toSet()
        val serverIds = (activeObjs + trashObjs).mapNotNull { it.str("id") }.toSet()

        fun localFolderId(serverFolderId: String): String? =
            folderMap.entries.firstOrNull { it.value == serverFolderId }?.key

        val merged = profiles.toMutableList()
        for (obj in activeObjs + trashObjs) {
            val sp = decode(obj) ?: continue
            val sid = sp.id
            val inTrash = sid in trashIds
            val idx = merged.indexOfFirst { it.serverId == sid }
            if (idx >= 0) {
                val local = merged[idx]
                if (!local.dirty) {
                    val next = sp.copy(
                        id = local.id,
                        serverId = sid,
                        dirty = false,
                        status = local.status,
                        lastProxyCheck = local.lastProxyCheck,
                        folderIds = sp.folderIds.mapNotNull { localFolderId(it) },
                        deletedAt = if (inTrash) sp.deletedAt ?: ProfileStore.nowIso() else null
                    )
                    if (next != local) {
                        merged[idx] = next
                        pulled++
                    }
                }
                // dirty local wins here; the push phase below reconciles it
            } else if (!inTrash) {
                merged += sp.copy(
                    id = ProfileStore.newId(),
                    serverId = sid,
                    dirty = false,
                    status = "ready",
                    lastProxyCheck = null,
                    folderIds = sp.folderIds.mapNotNull { localFolderId(it) }
                )
                pulled++
            }
        }
        // serverId vanished server-side → treat as never uploaded (re-upload)
        for (i in merged.indices) {
            val p = merged[i]
            val sid = p.serverId
            if (sid != null && sid !in serverIds) {
                merged[i] = p.copy(serverId = null, dirty = true)
            }
        }
        profiles = merged

        // ---------------- push profiles ----------------
        val pushedProfiles = profiles.toMutableList()
        for (i in pushedProfiles.indices) {
            var p = pushedProfiles[i]
            val sid = p.serverId
            when {
                p.deletedAt != null -> {
                    if (sid != null && sid !in trashIds) {
                        when (val dr = api.deleteProfile(token, ws, sid)) {
                            is ApiResult.Success -> {
                                p = p.copy(dirty = false); pushed++
                            }
                            is ApiResult.Failure -> {
                                if (dr.kind == ApiResult.Kind.AUTH) {
                                    profiles = pushedProfiles
                                    return abort(true, dr.message)
                                }
                                failures++; p = p.copy(dirty = true)
                            }
                        }
                    } else if (sid != null && p.dirty) {
                        p = p.copy(dirty = false) // already trashed on the server
                    }
                }
                sid == null -> {
                    when (val cr = api.createProfileRaw(token, ws, uploadPayload(p, folderMap))) {
                        is ApiResult.Success -> {
                            val newSid = cr.data.str("id")
                            if (newSid.isNullOrBlank()) {
                                failures++; p = p.copy(dirty = true)
                            } else {
                                p = p.copy(serverId = newSid, dirty = false); pushed++
                            }
                        }
                        is ApiResult.Failure -> {
                            if (cr.kind == ApiResult.Kind.AUTH) {
                                profiles = pushedProfiles
                                return abort(true, cr.message)
                            }
                            failures++; p = p.copy(dirty = true)
                        }
                    }
                }
                p.dirty -> {
                    if (sid in trashIds) {
                        // local Restore: un-trash on the server first
                        when (val rr = api.restoreProfile(token, ws, sid)) {
                            is ApiResult.Success -> pushed++
                            is ApiResult.Failure -> {
                                if (rr.kind == ApiResult.Kind.AUTH) {
                                    profiles = pushedProfiles
                                    return abort(true, rr.message)
                                }
                                failures++
                            }
                        }
                    }
                    when (val pr = api.patchProfileRaw(token, ws, sid, uploadPayload(p, folderMap))) {
                        is ApiResult.Success -> {
                            p = p.copy(dirty = false); pushed++
                        }
                        is ApiResult.Failure -> {
                            if (pr.kind == ApiResult.Kind.AUTH) {
                                profiles = pushedProfiles
                                return abort(true, pr.message)
                            }
                            failures++ // dirty stays set → retried next sync
                        }
                    }
                }
            }
            pushedProfiles[i] = p
        }
        profiles = pushedProfiles

        return SyncReport(
            store = snapshot(),
            pulled = pulled,
            pushed = pushed,
            failures = failures,
            message = if (failures > 0) "Some changes could not be synced" else ""
        )
    }

    private fun decode(obj: JsonObject): Profile? = try {
        json.decodeFromJsonElement(Profile.serializer(), obj)
    } catch (e: Exception) {
        null
    }

    /** Full-profile wire payload with local-only bookkeeping stripped. */
    private fun uploadPayload(p: Profile, folderMap: Map<String, String>): JsonObject {
        val wire = p.copy(
            status = "ready", // run state is per-device
            folderIds = p.folderIds.mapNotNull { folderMap[it] }
        )
        val obj = json.encodeToJsonElement(Profile.serializer(), wire).jsonObject
        val out = obj.toMutableMap()
        LOCAL_ONLY_KEYS.forEach { out.remove(it) }
        out.remove("id")
        return JsonObject(out)
    }

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull
}
