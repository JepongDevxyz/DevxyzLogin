package com.devxyz.login.data

import com.devxyz.login.model.ProxyCheckInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Typed result for every server call. The distinction matters in the UI:
 * a NETWORK failure means the device could not reach the server at all
 * (offline / DNS / timeout) — it is never reported as a server error,
 * and a server rejection is never reported as "offline".
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Failure(
        val kind: Kind,
        val message: String,
        val httpCode: Int = 0
    ) : ApiResult<Nothing>()

    enum class Kind { NETWORK, SERVER, AUTH, CLIENT }
}

/** A live login session against the DevxyzLogin server. */
data class AuthSession(
    val token: String,
    val userId: String,
    val email: String,
    val workspaceId: String?,
    val workspaceName: String?
)

/** Built-in proxy catalog summary (GET /api/proxies/catalog). */
data class CatalogInfo(
    val builtinAvailable: Boolean,
    /** type key → GB balance when the server reports one, else null. */
    val availableGb: Map<String, Double?>
)

/** API-token metadata (GET /api/tokens) — the raw token is never in here. */
@Serializable
data class ApiTokenMeta(
    val id: String,
    val name: String,
    val prefix: String,
    val workspaceId: String? = null,
    val createdAt: String = "",
    val lastUsedAt: String? = null
)

/**
 * Minimal client for the LIVE DevxyzLogin server
 * (https://devxyzlogin-server.vercel.app — contract: server/README.md +
 * server/app.js). HttpURLConnection + kotlinx-serialization only; no new
 * dependencies. Every data route on the server requires auth, including
 * POST /api/proxies/check, so all calls except register/login carry the
 * session Bearer token (plus X-Workspace-Id when a workspace is known).
 */
class ServerApi(private val baseUrl: String = BASE_URL) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // ------------------------------------------------------------------
    // auth
    suspend fun register(email: String, password: String, name: String): ApiResult<AuthSession> {
        val body = buildJsonObject {
            put("email", email)
            put("password", password)
            // The current server ignores `name` (accounts are email-only);
            // sent anyway so a future server version can pick it up.
            put("name", name)
        }
        return when (val r = request("POST", "/api/auth/register", null, null, body)) {
            is ApiResult.Success -> parseSession(r.data)
            is ApiResult.Failure -> r
        }
    }

    suspend fun login(email: String, password: String): ApiResult<AuthSession> {
        val body = buildJsonObject {
            put("email", email)
            put("password", password)
        }
        return when (val r = request("POST", "/api/auth/login", null, null, body)) {
            is ApiResult.Success -> parseSession(r.data)
            is ApiResult.Failure -> r
        }
    }

    suspend fun me(token: String): ApiResult<AuthSession> =
        when (val r = request("GET", "/api/auth/me", token, null, null)) {
            is ApiResult.Success -> {
                val user = r.data["user"]?.jsonObject
                val ws = r.data["workspace"] as? JsonObject
                ApiResult.Success(
                    AuthSession(
                        token = token,
                        userId = user?.str("id") ?: "",
                        email = user?.str("email") ?: "",
                        workspaceId = ws?.str("id"),
                        workspaceName = ws?.str("name")
                    )
                )
            }
            is ApiResult.Failure -> r
        }

    // ------------------------------------------------------------------
    // profiles / folders (raw JSON — SyncEngine owns the schema mapping)
    suspend fun listProfilesRaw(
        token: String, workspaceId: String?, deleted: Boolean
    ): ApiResult<List<JsonObject>> {
        val path = if (deleted) "/api/profiles?deleted=1" else "/api/profiles"
        return when (val r = request("GET", path, token, workspaceId, null)) {
            is ApiResult.Success -> ApiResult.Success(
                (r.data["profiles"]?.jsonArray ?: emptyList()).map { it.jsonObject }
            )
            is ApiResult.Failure -> r
        }
    }

    suspend fun createProfileRaw(
        token: String, workspaceId: String?, body: JsonObject
    ): ApiResult<JsonObject> =
        when (val r = request("POST", "/api/profiles", token, workspaceId, body)) {
            is ApiResult.Success -> ApiResult.Success(r.data["profile"]?.jsonObject ?: r.data)
            is ApiResult.Failure -> r
        }

    suspend fun patchProfileRaw(
        token: String, workspaceId: String?, serverId: String, body: JsonObject
    ): ApiResult<JsonObject> =
        when (val r = request("PATCH", "/api/profiles/$serverId", token, workspaceId, body)) {
            is ApiResult.Success -> ApiResult.Success(r.data["profile"]?.jsonObject ?: r.data)
            is ApiResult.Failure -> r
        }

    suspend fun deleteProfile(token: String, workspaceId: String?, serverId: String): ApiResult<Unit> =
        when (val r = request("DELETE", "/api/profiles/$serverId", token, workspaceId, null)) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Failure -> r
        }

    suspend fun restoreProfile(token: String, workspaceId: String?, serverId: String): ApiResult<Unit> =
        when (val r = request("POST", "/api/profiles/$serverId/restore", token, workspaceId, null)) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Failure -> r
        }

    /** Returns (serverId, name) pairs. */
    suspend fun listFoldersRaw(token: String, workspaceId: String?): ApiResult<List<Pair<String, String>>> =
        when (val r = request("GET", "/api/folders", token, workspaceId, null)) {
            is ApiResult.Success -> ApiResult.Success(
                (r.data["folders"]?.jsonArray ?: emptyList()).map {
                    val o = it.jsonObject
                    (o.str("id") ?: "") to (o.str("name") ?: "Folder")
                }.filter { it.first.isNotBlank() }
            )
            is ApiResult.Failure -> r
        }

    suspend fun createFolder(token: String, workspaceId: String?, name: String): ApiResult<Pair<String, String>> {
        val body = buildJsonObject { put("name", name) }
        return when (val r = request("POST", "/api/folders", token, workspaceId, body)) {
            is ApiResult.Success -> {
                val f = r.data["folder"]?.jsonObject
                val id = f?.str("id")
                if (id.isNullOrBlank()) ApiResult.Failure(ApiResult.Kind.SERVER, "Unexpected server response")
                else ApiResult.Success(id to (f.str("name") ?: name))
            }
            is ApiResult.Failure -> r
        }
    }

    // ------------------------------------------------------------------
    // proxies
    /**
     * REAL proxy check. The server requires auth for this route (verified
     * in app.js: everything below /api/auth passes through authenticate()),
     * so a session token is mandatory — callers must be logged in.
     * A check-level failure (server answered {ok:false, error}) comes back
     * as a CLIENT failure carrying the checker's own error message.
     */
    suspend fun checkProxy(
        token: String,
        type: String,
        host: String,
        port: Int,
        username: String,
        password: String
    ): ApiResult<ProxyCheckInfo> {
        val body = buildJsonObject {
            put("type", type)
            put("host", host)
            put("port", port)
            put("username", username)
            put("password", password)
        }
        return when (val r = request("POST", "/api/proxies/check", token, null, body, readTimeoutMs = 35_000)) {
            is ApiResult.Success -> {
                val d = r.data
                if ((d["ok"] as? JsonPrimitive)?.booleanOrNull == true) {
                    ApiResult.Success(
                        ProxyCheckInfo(
                            ip = d.str("ip") ?: "",
                            country = d.str("country") ?: "",
                            countryCode = d.str("countryCode") ?: "",
                            city = d.str("city") ?: "",
                            timezone = d.str("timezone") ?: "",
                            latencyMs = d.longOr("latencyMs", 0),
                            checkedAt = ProfileStore.nowIso()
                        )
                    )
                } else {
                    ApiResult.Failure(
                        ApiResult.Kind.CLIENT,
                        d.str("error") ?: "Proxy check failed",
                        httpCode = 200
                    )
                }
            }
            is ApiResult.Failure -> r
        }
    }

    /**
     * Ask the server for a built-in proxy (POST /api/proxies/builtin).
     * Returns the created shared-proxy object ({id, name, proxy:{...}}).
     * Until a residential provider is plugged into the server this
     * honestly fails with 501 "no residential provider configured".
     */
    suspend fun builtinProxy(
        token: String, workspaceId: String?, country: String, type: String,
        region: String = "", city: String = ""
    ): ApiResult<JsonObject> {
        val body = buildJsonObject {
            put("country", country)
            put("type", type)
            // v6: the region/city PREFERENCE is stored on the server's
            // assignment record; it is never the measured exit (the
            // exit city comes from checkProxy afterwards).
            if (region.isNotBlank()) put("region", region)
            if (city.isNotBlank()) put("city", city)
        }
        return when (val r = request("POST", "/api/proxies/builtin", token, workspaceId, body)) {
            is ApiResult.Success -> ApiResult.Success(r.data["proxy"]?.jsonObject ?: r.data)
            is ApiResult.Failure -> r
        }
    }

    /**
     * Built-in proxy catalog (GET /api/proxies/catalog): whether ANY
     * provider is configured on the server ([CatalogInfo.builtinAvailable])
     * and, per type key ("residential" | "mobile" | "datacenter"), the
     * provider-declared GB balance from the entry's `gbAvailable` field
     * (v6; `availableGb` is accepted as a legacy alias). The value is
     * null while no provider declares one — the UI then shows the
     * honest "0.00 GB available" / no-provider state, never an
     * invented number.
     */
    suspend fun catalog(token: String): ApiResult<CatalogInfo> =
        when (val r = request("GET", "/api/proxies/catalog", token, null, null)) {
            is ApiResult.Success -> {
                val available = (r.data["builtinAvailable"] as? JsonPrimitive)?.booleanOrNull ?: false
                val gb = mutableMapOf<String, Double?>()
                (r.data["catalog"] as? kotlinx.serialization.json.JsonArray)?.forEach { el ->
                    val o = el as? JsonObject ?: return@forEach
                    val type = o.str("type") ?: return@forEach
                    gb[type] = (o["gbAvailable"] as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull()
                        ?: (o["availableGb"] as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull()
                }
                ApiResult.Success(CatalogInfo(builtinAvailable = available, availableGb = gb))
            }
            is ApiResult.Failure -> r
        }

    // ------------------------------------------------------------------
    // API tokens
    suspend fun listTokens(token: String, workspaceId: String?): ApiResult<List<ApiTokenMeta>> =
        when (val r = request("GET", "/api/tokens", token, workspaceId, null)) {
            is ApiResult.Success -> try {
                ApiResult.Success(
                    json.decodeFromJsonElement(
                        kotlinx.serialization.builtins.ListSerializer(ApiTokenMeta.serializer()),
                        r.data["tokens"] ?: kotlinx.serialization.json.JsonArray(emptyList())
                    )
                )
            } catch (e: Exception) {
                ApiResult.Failure(ApiResult.Kind.SERVER, "Unexpected server response")
            }
            is ApiResult.Failure -> r
        }

    /** Returns (metadata, rawToken) — the raw token exists ONLY in this response. */
    suspend fun createToken(
        token: String, workspaceId: String?, name: String
    ): ApiResult<Pair<ApiTokenMeta, String>> {
        val body = buildJsonObject { put("name", name) }
        return when (val r = request("POST", "/api/tokens", token, workspaceId, body)) {
            is ApiResult.Success -> try {
                val meta = json.decodeFromJsonElement(
                    ApiTokenMeta.serializer(),
                    r.data["apiToken"]?.jsonObject ?: r.data
                )
                val raw = r.data.str("token") ?: ""
                if (raw.isBlank()) ApiResult.Failure(ApiResult.Kind.SERVER, "Unexpected server response")
                else ApiResult.Success(meta to raw)
            } catch (e: Exception) {
                ApiResult.Failure(ApiResult.Kind.SERVER, "Unexpected server response")
            }
            is ApiResult.Failure -> r
        }
    }

    suspend fun deleteToken(token: String, workspaceId: String?, id: String): ApiResult<Unit> =
        when (val r = request("DELETE", "/api/tokens/$id", token, workspaceId, null)) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Failure -> r
        }

    // ------------------------------------------------------------------
    private fun parseSession(data: JsonObject): ApiResult<AuthSession> {
        val token = data.str("token")
        val user = data["user"]?.jsonObject
        val ws = data["workspace"] as? JsonObject
        return if (token.isNullOrBlank() || user == null) {
            ApiResult.Failure(ApiResult.Kind.SERVER, "Unexpected server response")
        } else {
            ApiResult.Success(
                AuthSession(
                    token = token,
                    userId = user.str("id") ?: "",
                    email = user.str("email") ?: "",
                    workspaceId = ws?.str("id"),
                    workspaceName = ws?.str("name")
                )
            )
        }
    }

    private suspend fun request(
        method: String,
        path: String,
        token: String?,
        workspaceId: String?,
        body: JsonObject?,
        readTimeoutMs: Int = 25_000
    ): ApiResult<JsonObject> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 15_000
                readTimeout = readTimeoutMs
                doInput = true
                if (token != null) setRequestProperty("Authorization", "Bearer $token")
                if (workspaceId != null) setRequestProperty("X-Workspace-Id", workspaceId)
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
            }
            if (body != null) {
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            val parsed = try {
                if (text.isBlank()) JsonObject(emptyMap())
                else json.parseToJsonElement(text).jsonObject
            } catch (e: Exception) {
                null
            }
            when {
                code in 200..299 && parsed != null -> ApiResult.Success(parsed)
                code in 200..299 -> ApiResult.Failure(
                    ApiResult.Kind.SERVER, "Unexpected server response", code
                )
                else -> {
                    val serverMsg = parsed?.str("error")
                    val kind = when {
                        code == 401 || code == 403 -> ApiResult.Kind.AUTH
                        code in 400..499 -> ApiResult.Kind.CLIENT
                        else -> ApiResult.Kind.SERVER
                    }
                    ApiResult.Failure(
                        kind,
                        serverMsg ?: when (kind) {
                            ApiResult.Kind.AUTH -> "Session expired — please log in again"
                            ApiResult.Kind.SERVER -> "Server error ($code) — try again later"
                            else -> "Request failed ($code)"
                        },
                        code
                    )
                }
            }
        } catch (e: IOException) {
            ApiResult.Failure(
                ApiResult.Kind.NETWORK,
                "Can't reach the server — check your connection"
            )
        } catch (e: Exception) {
            ApiResult.Failure(ApiResult.Kind.SERVER, "Unexpected error: ${e.message ?: "unknown"}")
        } finally {
            conn?.disconnect()
        }
    }

    companion object {
        const val BASE_URL = "https://devxyzlogin-server.vercel.app"
    }
}

private fun JsonObject.str(key: String): String? =
    (this[key] as? JsonPrimitive)?.contentOrNull

private fun JsonObject.longOr(key: String, fallback: Long): Long =
    (this[key] as? JsonPrimitive)?.contentOrNull?.toLongOrNull() ?: fallback
