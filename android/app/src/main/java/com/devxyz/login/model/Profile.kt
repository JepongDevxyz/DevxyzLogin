package com.devxyz.login.model

import kotlinx.serialization.Serializable

/**
 * Shared DevxyzLogin profile schema — see ~/workspace/devxyzlogin/SHARED_SCHEMA.md.
 * Field names MUST stay identical to the web dashboard track.
 */

@Serializable
data class NavigatorInfo(
    val userAgent: String = "",
    val browserVersion: String = "",
    val screenResolution: String = "1920x1080",
    val languages: List<String> = listOf("en-US", "en"),
    val platform: String = "Win32",
    val cpuCores: Int = 8,
    val ram: Int = 8,
    val doNotTrack: Boolean = false,
    /** v3 (additive): Languages "Based on IP" toggle (capture §4.7) — in v2
     *  this was wrongly tied to the timezone auto flag. */
    val languagesAuto: Boolean = true
)

@Serializable
data class MediaDevices(
    val videoInputs: Int = 1,
    val audioInputs: Int = 1,
    val audioOutputs: Int = 1
)

@Serializable
data class HardwareInfo(
    /** "off" | "block" | "noise" — GoLogin editor default is Off (capture §4.7) */
    val canvasMode: String = "off",
    val webglVendor: String = "",
    val webglRenderer: String = "",
    /** WebGL Image noise — GoLogin editor default Off */
    val webglNoise: Boolean = false,
    /** Audio Context noise — GoLogin editor default Noise */
    val audioNoise: Boolean = true,
    /** Client Rects noise — GoLogin editor default Off */
    val clientRectsNoise: Boolean = false,
    /** WebGL Metadata: true = "Mask", false = "Real" (GoLogin default Mask) */
    val webglMetadataMask: Boolean = true
)

@Serializable
data class TimezoneInfo(
    val auto: Boolean = true,
    val zone: String = "America/New_York"
)

@Serializable
data class GeolocationInfo(
    /** "prompt" | "allow" | "block" */
    val mode: String = "prompt",
    val auto: Boolean = true,
    val lat: Double = 0.0,
    val lon: Double = 0.0
)

@Serializable
data class Fingerprint(
    val navigator: NavigatorInfo = NavigatorInfo(),
    val fontsMasking: Boolean = true,
    /** v3 (additive): "Mask Media Devices" toggle (capture §4.7, default ON). */
    val mediaMasking: Boolean = true,
    val mediaDevices: MediaDevices = MediaDevices(),
    val hardware: HardwareInfo = HardwareInfo(),
    /** "basedOnIp" | "off" | "disabled" */
    val webrtcMode: String = "basedOnIp",
    val timezone: TimezoneInfo = TimezoneInfo(),
    val geolocation: GeolocationInfo = GeolocationInfo()
)

@Serializable
data class ProxyConfig(
    /** "none" | "devxyz" | "custom" */
    val mode: String = "none",
    /** "http" | "https" | "socks4" | "socks5" */
    val type: String = "http",
    val host: String = "",
    val port: Int = 0,
    val username: String = "",
    val password: String = "",
    /** v3 (additive, same names as web v3): built-in proxy country (ISO
     *  alpha-2, capture default France) and kind — "Residential" (default) |
     *  "Mobile" | "Data Center". Only meaningful when mode == "devxyz". */
    val buyCountry: String = "FR",
    val buyType: String = "Residential",
    /** v3 (additive): "Buy dedicated IP" segment selected (capture §4.2). */
    val dedicated: Boolean = false,
    /** v3 (additive): "Change IP URL for mobile proxy" (capture §4.2). */
    val changeIpUrl: String = ""
) {
    val isSet: Boolean get() = mode != "none" && host.isNotBlank() && port > 0
    /** Short label for the profile list, e.g. "SOCKS5 • 1.2.3.4:1080" or "No proxy". */
    val listLabel: String
        get() = if (!isSet) "No proxy"
        else "${type.uppercase()} • $host:$port"
}

/**
 * Browser behaviour options (GoLogin Advanced → Storage / Other sections,
 * capture §4.7). Additive in v2 — defaults match the GoLogin new-profile
 * editor so v1 store.json files load unchanged.
 */
@Serializable
data class BrowserOptions(
    /** "newTab" | "previouslyOpened" | "specified" */
    val startUrl: String = "previouslyOpened",
    val startPages: List<String> = emptyList(),
    val customDns: String = "",
    val launchArguments: String = "",
    val googleServices: Boolean = true,
    val disableImageLoading: Boolean = false,
    val disableVideoAutoplay: Boolean = false,
    val saveBookmarks: Boolean = true,
    val saveHistory: Boolean = true,
    val savePasswords: Boolean = true,
    val localStorage: Boolean = true,
    val extensionStorage: Boolean = true,
    val indexedDb: Boolean = true,
    val vulnerablePlugins: Boolean = true,
    val systemExtensions: Boolean = false,
    /** v3 (additive): quick-settings Storage rows (capture §5) — "Save tabs"
     *  and "Allow install extensions", both ON in GoLogin. */
    val saveTabs: Boolean = true,
    val allowInstallExtensions: Boolean = true
)

/**
 * Result of a REAL server-side proxy check (POST /api/proxies/check) —
 * measured exit data, never invented. Stored on the profile so the
 * quick-settings sheet can show the resolved timezone / place for
 * "Based on IP" rows (v4: closes the v3 residual). Client-local
 * bookkeeping: stripped from sync uploads (see SyncEngine).
 */
@Serializable
data class ProxyCheckInfo(
    val ip: String = "",
    val country: String = "",
    val countryCode: String = "",
    val city: String = "",
    val timezone: String = "",
    val latencyMs: Long = 0,
    val checkedAt: String = ""
) {
    /** "City, Country" (either part may be blank). */
    val place: String get() = listOf(city, country).filter { it.isNotBlank() }.joinToString(", ")
}

@Serializable
data class Profile(
    val id: String,
    val name: String = "New profile",
    val folderIds: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    /** "ready" | "running" */
    val status: String = "ready",
    val notes: String = "",
    /** windows10 | windows11 | macIntel | macArm | linux | android — locked after creation */
    val os: String = "android",
    val fingerprint: Fingerprint = Fingerprint(),
    val proxy: ProxyConfig = ProxyConfig(),
    val extensions: List<String> = emptyList(),
    /** Raw lines: "folder::name::url" | "name::url" | "url" */
    val bookmarks: List<String> = emptyList(),
    /** v2 (additive): browser behaviour options — see [BrowserOptions]. */
    val browser: BrowserOptions = BrowserOptions(),
    /** v2 (additive): imported cookies, raw text as pasted (Cookies tab parity). */
    val cookies: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    val deletedAt: String? = null,
    /**
     * v4 (additive, CLIENT-LOCAL sync bookkeeping — never sent to the
     * server): the profile's id on the DevxyzLogin server once uploaded;
     * null = not on the server yet. `dirty` = local edits not yet pushed.
     * Sync uploads strip both fields (and [lastProxyCheck]) so the shared
     * schema on the wire stays exactly SHARED_SCHEMA.md.
     */
    val serverId: String? = null,
    val dirty: Boolean = false,
    /** v4 (additive, client-local): last real proxy-check result. */
    val lastProxyCheck: ProxyCheckInfo? = null
)

@Serializable
data class Folder(
    val id: String,
    val name: String
)

@Serializable
data class Workspace(
    val id: String,
    val name: String
)

@Serializable
data class SharedProxy(
    val id: String,
    val name: String,
    val proxy: ProxyConfig,
    /** v3 (additive, web v3 parity): country name + flag for the proxies table. */
    val country: String = "",
    val flag: String = ""
)

/** Whole local store, persisted as one JSON file (store.json). */
@Serializable
data class Store(
    val profiles: List<Profile> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val workspaces: List<Workspace> = emptyList(),
    val proxies: List<SharedProxy> = emptyList(),
    val members: List<String> = emptyList(),
    /**
     * v4 (additive, client-local): local folder id → server folder id,
     * maintained by SyncEngine so profile folderIds can be translated
     * between the local and server id spaces during sync.
     */
    val folderServerIds: Map<String, String> = emptyMap()
)

/** OS options shown in the Overview picker — exact GoLogin labels and order (capture §4.1). */
object OsOptions {
    val all: List<Pair<String, String>> = listOf(
        "windows10" to "Windows 10",
        "windows11" to "Windows 11",
        "macIntel" to "Mac Intel",
        "macArm" to "Mac ARM",
        "linux" to "Linux",
        "android" to "Android"
    )

    fun label(os: String): String = all.firstOrNull { it.first == os }?.second ?: os
}
