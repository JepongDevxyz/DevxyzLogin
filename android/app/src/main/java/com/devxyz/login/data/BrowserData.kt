package com.devxyz.login.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * v2.5.0 — persistent browser data for the Orbita-style profile browser.
 *
 * Two stores, modelled on [ProfileStore]:
 *  - [BrowserDataStore]: ONE JSON file per browser slot
 *    (filesDir/browser_slot_<slot>.json), because each slot is a separate
 *    WebView data directory — its history/bookmarks/downloads belong to
 *    that slot's storage, exactly like the cookies do. Holds the browsing
 *    history, star-bookmarks, download records, NTP shortcuts, recently
 *    closed tabs and the open-tab list used by "restore tabs on launch".
 *    Private (incognito) tabs are NEVER written to any of these lists.
 *  - [BrowserPrefs]: browser-wide settings in SharedPreferences
 *    ("devxyz_browser_prefs") — search engine, address-bar position,
 *    appearance, WebSettings toggles, NTP section toggles and the
 *    per-slot session-start timestamps behind the session timer pill.
 */

@Serializable
data class BrowserHistoryEntry(
    val title: String = "",
    val url: String = "",
    val visitedAt: Long = 0L
)

@Serializable
data class BrowserBookmark(
    val title: String = "",
    val url: String = "",
    val addedAt: Long = 0L
)

@Serializable
data class BrowserDownload(
    /** DownloadManager id (-1 until enqueued). */
    val dmId: Long = -1L,
    val fileName: String = "",
    val url: String = "",
    /** "downloading" | "complete" | "failed" */
    val status: String = "downloading",
    val totalBytes: Long = 0L,
    val startedAt: Long = 0L
)

@Serializable
data class BrowserShortcut(
    val title: String = "",
    val url: String = ""
)

@Serializable
data class ClosedTabEntry(
    val title: String = "",
    val url: String = "",
    val closedAt: Long = 0L
)

@Serializable
data class SavedTab(
    val url: String = "",
    val title: String = "",
    val group: String? = null
)

@Serializable
data class BrowserSlotData(
    val history: List<BrowserHistoryEntry> = emptyList(),
    val bookmarks: List<BrowserBookmark> = emptyList(),
    val downloads: List<BrowserDownload> = emptyList(),
    /** null = never seeded (first run seeds dashboard + start page). */
    val shortcuts: List<BrowserShortcut>? = null,
    val recentClosed: List<ClosedTabEntry> = emptyList(),
    val savedTabs: List<SavedTab> = emptyList(),
    val savedActiveIndex: Int = 0
)

class BrowserDataStore(context: Context, private val slot: Int) {

    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, "browser_slot_$slot.json")

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun load(): BrowserSlotData = try {
        if (file.exists()) json.decodeFromString<BrowserSlotData>(file.readText())
        else BrowserSlotData()
    } catch (e: Exception) {
        BrowserSlotData()
    }

    fun save(data: BrowserSlotData) {
        try {
            file.writeText(json.encodeToString(data))
        } catch (e: Exception) { /* best effort — browser data is rebuildable */ }
    }
}

/** Search engines the browser can really resolve queries with. */
data class SearchEngine(
    val id: String,
    val name: String,
    /** Prefix a URL-encoded query is appended to. */
    val searchPrefix: String,
    val homeUrl: String
)

object SearchEngines {
    val GOOGLE = SearchEngine(
        "google", "Google",
        "https://www.google.com/search?q=",
        "https://www.google.com"
    )
    val DUCKDUCKGO = SearchEngine(
        "duckduckgo", "DuckDuckGo",
        "https://duckduckgo.com/?q=",
        "https://duckduckgo.com"
    )
    val BING = SearchEngine(
        "bing", "Bing",
        "https://www.bing.com/search?q=",
        "https://www.bing.com"
    )
    val ALL = listOf(GOOGLE, DUCKDUCKGO, BING)

    fun byId(id: String): SearchEngine = ALL.firstOrNull { it.id == id } ?: GOOGLE

    /** Google AI Mode (udm=50) — a real Google Search route. */
    fun aiModeUrl(query: String): String =
        "https://www.google.com/search?udm=50&q=" + java.net.URLEncoder.encode(query, "UTF-8")
}

/** Browser-wide settings (SharedPreferences — same app UID, all processes). */
class BrowserPrefs(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("devxyz_browser_prefs", Context.MODE_PRIVATE)

    var searchEngineId: String
        get() = prefs.getString("search_engine", "google") ?: "google"
        set(value) { prefs.edit().putString("search_engine", value).apply() }

    /** "top" | "bottom" */
    var addressBarPosition: String
        get() = prefs.getString("address_bar", "top") ?: "top"
        set(value) { prefs.edit().putString("address_bar", value).apply() }

    /** "system" | "light" | "dark" — browser chrome appearance. */
    var appearance: String
        get() = prefs.getString("appearance", "system") ?: "system"
        set(value) { prefs.edit().putString("appearance", value).apply() }

    var textZoom: Int
        get() = prefs.getInt("text_zoom", 100)
        set(value) { prefs.edit().putInt("text_zoom", value).apply() }

    var javaScriptEnabled: Boolean
        get() = prefs.getBoolean("javascript", true)
        set(value) { prefs.edit().putBoolean("javascript", value).apply() }

    var blockThirdPartyCookies: Boolean
        get() = prefs.getBoolean("block_third_party_cookies", false)
        set(value) { prefs.edit().putBoolean("block_third_party_cookies", value).apply() }

    var locationEnabled: Boolean
        get() = prefs.getBoolean("location_access", false)
        set(value) { prefs.edit().putBoolean("location_access", value).apply() }

    var restoreTabs: Boolean
        get() = prefs.getBoolean("restore_tabs", true)
        set(value) { prefs.edit().putBoolean("restore_tabs", value).apply() }

    /** Homepage = the New Tab page; OFF makes new tabs open the engine home. */
    var homepageEnabled: Boolean
        get() = prefs.getBoolean("homepage_ntp", true)
        set(value) { prefs.edit().putBoolean("homepage_ntp", value).apply() }

    // ---- Customize new tab page toggles ----
    var showShortcuts: Boolean
        get() = prefs.getBoolean("ntp_shortcuts", true)
        set(value) { prefs.edit().putBoolean("ntp_shortcuts", value).apply() }

    var showTips: Boolean
        get() = prefs.getBoolean("ntp_tips", true)
        set(value) { prefs.edit().putBoolean("ntp_tips", value).apply() }

    var showStories: Boolean
        get() = prefs.getBoolean("ntp_stories", true)
        set(value) { prefs.edit().putBoolean("ntp_stories", value).apply() }

    // ---- Session timer (per slot) ----
    fun sessionStart(slot: Int): Long = prefs.getLong("session_start_$slot", 0L)

    fun setSessionStart(slot: Int, startMs: Long) {
        prefs.edit().putLong("session_start_$slot", startMs).apply()
    }

    fun clearSessionStart(slot: Int) {
        prefs.edit().remove("session_start_$slot").apply()
    }
}
