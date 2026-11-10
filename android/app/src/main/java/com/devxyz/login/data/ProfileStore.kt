package com.devxyz.login.data

import android.content.Context
import com.devxyz.login.model.FingerprintGenerator
import com.devxyz.login.model.Folder
import com.devxyz.login.model.Profile
import com.devxyz.login.model.Store
import com.devxyz.login.model.Workspace
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import java.util.UUID

/**
 * Local persistence: the whole [Store] as one JSON file (filesDir/store.json),
 * plus a slot map (filesDir/slots.json) assigning each profile a permanent
 * browser-process slot (1..3) — see SHARED_SCHEMA.md isolation notes.
 * The browser processes read the same files (same app UID).
 */
class ProfileStore(context: Context) {

    private val appContext = context.applicationContext
    private val storeFile = File(appContext.filesDir, "store.json")
    private val slotsFile = File(appContext.filesDir, "slots.json")

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun load(): Store {
        val loaded = try {
            if (storeFile.exists()) json.decodeFromString<Store>(storeFile.readText())
            else Store()
        } catch (e: Exception) {
            Store()
        }
        if (loaded.profiles.isNotEmpty() || storeFile.exists()) return loaded
        // First run: seed 1 workspace, 1 folder, 1 sample profile.
        val now = nowIso()
        val folder = Folder(id = newId(), name = "Social")
        val sample = Profile(
            id = newId(),
            name = "Sample profile",
            folderIds = listOf(folder.id),
            tags = listOf("sample"),
            status = "ready",
            notes = "Seeded sample — edit or delete me.",
            os = "android",
            fingerprint = FingerprintGenerator.generate("android", seed = 42L),
            createdAt = now,
            updatedAt = now
        )
        val seeded = Store(
            profiles = listOf(sample),
            folders = listOf(folder),
            workspaces = listOf(Workspace(id = newId(), name = "My workspace"))
        )
        save(seeded)
        return seeded
    }

    fun save(store: Store) {
        storeFile.writeText(json.encodeToString(store))
    }

    /** Read-only load for browser processes (no seeding side effects). */
    fun loadReadOnly(): Store = try {
        if (storeFile.exists()) json.decodeFromString<Store>(storeFile.readText()) else Store()
    } catch (e: Exception) {
        Store()
    }

    fun findProfile(store: Store, id: String): Profile? =
        store.profiles.firstOrNull { it.id == id }

    /**
     * Permanent browser slot (1..3) for a profile: least-used slot wins on
     * first launch, then the assignment never changes so the profile's
     * WebView data directory (cookies/storage) stays the same across runs.
     */
    fun slotFor(profileId: String): Int {
        val map = loadSlots()
        map[profileId]?.let { return it }
        val counts = intArrayOf(0, 0, 0, 0) // index by slot 1..3
        map.values.forEach { if (it in 1..3) counts[it]++ }
        var best = 1
        for (s in 2..3) if (counts[s] < counts[best]) best = s
        map[profileId] = best
        saveSlots(map)
        return best
    }

    private fun loadSlots(): MutableMap<String, Int> = try {
        if (slotsFile.exists()) json.decodeFromString<Map<String, Int>>(slotsFile.readText()).toMutableMap()
        else mutableMapOf()
    } catch (e: Exception) {
        mutableMapOf()
    }

    private fun saveSlots(map: Map<String, Int>) {
        slotsFile.writeText(json.encodeToString(map))
    }

    companion object {
        fun newId(): String = UUID.randomUUID().toString()
        fun nowIso(): String = Instant.now().toString()
    }
}
