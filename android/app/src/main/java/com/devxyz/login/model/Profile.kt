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
    val doNotTrack: Boolean = false
)

@Serializable
data class MediaDevices(
    val videoInputs: Int = 1,
    val audioInputs: Int = 1,
    val audioOutputs: Int = 1
)

@Serializable
data class HardwareInfo(
    /** "off" | "block" | "noise" */
    val canvasMode: String = "noise",
    val webglVendor: String = "",
    val webglRenderer: String = "",
    val webglNoise: Boolean = true,
    val audioNoise: Boolean = true,
    val clientRectsNoise: Boolean = true
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
    val password: String = ""
) {
    val isSet: Boolean get() = mode != "none" && host.isNotBlank() && port > 0
    /** Short label for the profile list, e.g. "SOCKS5 • 1.2.3.4:1080" or "No proxy". */
    val listLabel: String
        get() = if (!isSet) "No proxy"
        else "${type.uppercase()} • $host:$port"
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
    val createdAt: String = "",
    val updatedAt: String = "",
    val deletedAt: String? = null
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
    val proxy: ProxyConfig
)

/** Whole local store, persisted as one JSON file (store.json). */
@Serializable
data class Store(
    val profiles: List<Profile> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val workspaces: List<Workspace> = emptyList(),
    val proxies: List<SharedProxy> = emptyList(),
    val members: List<String> = emptyList()
)

/** OS options shown in the Overview picker (GoLogin parity). */
object OsOptions {
    val all: List<Pair<String, String>> = listOf(
        "windows10" to "Windows 10",
        "windows11" to "Windows 11",
        "macIntel" to "Mac (Intel)",
        "macArm" to "Mac (ARM)",
        "linux" to "Linux",
        "android" to "Android"
    )

    fun label(os: String): String = all.firstOrNull { it.first == os }?.second ?: os
}
