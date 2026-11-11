package com.devxyz.login.browser

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognizerIntent
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.core.content.ContextCompat
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import com.devxyz.login.MainActivity
import com.devxyz.login.R
import com.devxyz.login.data.BrowserBookmark
import com.devxyz.login.data.BrowserDataStore
import com.devxyz.login.data.BrowserDownload
import com.devxyz.login.data.BrowserHistoryEntry
import com.devxyz.login.data.BrowserPrefs
import com.devxyz.login.data.BrowserShortcut
import com.devxyz.login.data.BrowserSlotData
import com.devxyz.login.data.ClosedTabEntry
import com.devxyz.login.data.ProfileStore
import com.devxyz.login.data.SavedTab
import com.devxyz.login.data.SearchEngine
import com.devxyz.login.data.SearchEngines
import com.devxyz.login.data.SessionStore
import com.devxyz.login.model.DefaultBookmarks
import com.devxyz.login.model.Profile
import com.devxyz.login.ui.AppBottomSheet
import com.devxyz.login.ui.BrandGreen
import com.devxyz.login.ui.BrandGreenLight
import com.devxyz.login.ui.ConfirmSheet
import com.devxyz.login.ui.DangerRed
import com.devxyz.login.ui.DevxyzTheme
import com.devxyz.login.ui.LocalDarkTheme
import com.devxyz.login.ui.SheetTitle
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Internal URL of the New Tab page. */
const val NTP_URL = "devxyz://newtab"

/** Full-screen panels reachable from the ⋮ menu (v2.5.0). */
private enum class BrowserPanel { HISTORY, DOWNLOADS, BOOKMARKS, RECENT, SETTINGS }

// ---- Orbita warm palette (ORBITA_BROWSER_SPEC.md, measured from her
// recording): dark values are the real Orbita browns; light values are
// the white/warm-gray equivalents. Picked via LocalDarkTheme in the
// Orbita*() helpers further down.
private val OrbitaBgDark = Color(0xFF190F0E)
private val OrbitaPillDark = Color(0xFF46302A)
private val OrbitaCircleDark = Color(0xFF261B18)
private val OrbitaPanelDark = Color(0xFF423532)
private val OrbitaRowDark = Color(0xFF4E4340)
private val OrbitaText2Dark = Color(0xFFC9B4AC)
private val OrbitaBgLight = Color(0xFFFFFFFF)
private val OrbitaPillLight = Color(0xFFF3ECE9)
private val OrbitaCircleLight = Color(0xFFF8F3F0)
private val OrbitaPanelLight = Color(0xFFFFFFFF)
private val OrbitaRowLight = Color(0xFFF8F3F0)
private val OrbitaText2Light = Color(0xFF7D6D66)

/** One real browser tab: its own WebView inside the profile's process/data-dir. */
class BrowserTab(val id: Int) {
    var webView: WebView? = null
    var url by mutableStateOf(NTP_URL)
    var title by mutableStateOf("New Tab")
    var desktop by mutableStateOf(false)
    var canBack by mutableStateOf(false)
    var canForward by mutableStateOf(false)
    /** v2.5.0: tab-group label shown as a header in the tab switcher. */
    var group by mutableStateOf<String?>(null)
    /** v2.5.0: private (incognito) tab — never written to history,
     *  bookmarks, recent-closed or the restore list. */
    var private by mutableStateOf(false)
    var favicon by mutableStateOf<Bitmap?>(null)
}

/**
 * Orbita-style browser window for ONE profile slot (v2.5.0).
 *
 * GoLogin's Orbita is a Chromium fork; v2.5.0 mirrors the REAL Orbita
 * Android UI captured from the user's own recording
 * (ORBITA_BROWSER_SPEC.md): warm-brown chrome, logo tile + address pill
 * + live session-timer pill + tab-count square + ⋮, Chromium-shaped New
 * Tab page (engine wordmark, search pill, AI Mode / Incognito pills,
 * shortcut circles, tips carousel, stories placeholder), the ⋮ menu
 * (icon row + History/Downloads/Bookmarks/Recent tabs/Settings/…), and
 * a Chromium-layout Settings screen. Every control is wired to a REAL
 * handler in this app — nothing is a decorative stub.
 *
 * ISOLATION MODEL (unchanged): the activity runs in a dedicated process
 * (:p1/:p2/:p3); before ANY WebView exists in that process we call
 * WebView.setDataDirectorySuffix("slot_N"). Profiles are permanently
 * assigned a slot; profiles sharing a slot share storage — the real
 * WebView limit, documented rather than hidden. Per-slot browser data
 * (history/bookmarks/downloads/shortcuts) lives in [BrowserDataStore];
 * browser-wide settings live in [BrowserPrefs].
 */
abstract class BrowserActivityBase(private val slot: Int) : ComponentActivity() {

    private var profile: Profile? = null
    private var apiMode: Boolean = false

    private val tabs = mutableStateListOf<BrowserTab>()
    private var activeTabId by mutableStateOf(0)
    private var loadProgress by mutableStateOf(0)
    private var proxyReady by mutableStateOf(false)
    private var containerRef: FrameLayout? = null
    private var nextTabId = 1

    // v2.5.0 stores — loaded in onCreate before setContent.
    private lateinit var dataStore: BrowserDataStore
    private lateinit var browserPrefs: BrowserPrefs
    private val historyList = mutableStateListOf<BrowserHistoryEntry>()
    private val bookmarkList = mutableStateListOf<BrowserBookmark>()
    private val downloadList = mutableStateListOf<BrowserDownload>()
    private val shortcutList = mutableStateListOf<BrowserShortcut>()
    private val recentClosedList = mutableStateListOf<ClosedTabEntry>()

    // Session timer (real elapsed time since this profile run started).
    private var sessionStartMs: Long = 0L
    private var elapsedSec by mutableStateOf(0L)

    // Browser-chrome appearance ("system" | "light" | "dark").
    private var appearanceMode by mutableStateOf("system")

    // Transient UI state
    private var showSwitcher by mutableStateOf(false)
    private var menuOpen by mutableStateOf(false)
    private var findOpen by mutableStateOf(false)
    private var findQuery by mutableStateOf("")
    private var findResult by mutableStateOf("")
    private var panel by mutableStateOf<BrowserPanel?>(null)
    private var settingsRoute by mutableStateOf("root")
    private var showConsole by mutableStateOf(false)
    private var showStopConfirm by mutableStateOf(false)
    private var showSiteInfo by mutableStateOf(false)
    private var showProfileInfo by mutableStateOf(false)
    private var showGroupSheet by mutableStateOf(false)
    private var showAddShortcut by mutableStateOf(false)
    private var showCustomize by mutableStateOf(false)
    private var showClearDataConfirm by mutableStateOf(false)
    private var showHelp by mutableStateOf(false)
    /** v2.3.1: the active tab's main-frame load died on a proxy error
     *  (ERR_PROXY_*) — BrowserContent shows our own error state for it. */
    private var proxyError by mutableStateOf(false)

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.getIntExtra(EXTRA_SLOT, -1) == slot) finish()
        }
    }

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshDownloadStatuses()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // MUST run before any WebView is created in this process.
        try {
            WebView.setDataDirectorySuffix("slot_$slot")
        } catch (e: IllegalStateException) {
            // A WebView already existed in this process — isolation for this
            // slot is degraded; continue rather than crash.
        }

        val profileId = intent.getStringExtra(EXTRA_PROFILE_ID)
        apiMode = intent.getBooleanExtra(EXTRA_API_MODE, false)
        val store = ProfileStore(this)
        val loaded = profileId?.let { store.findProfile(store.loadReadOnly(), it) }
        if (loaded == null) {
            Toast.makeText(this, "Profile not found", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        profile = loaded

        // v2.5.0: per-slot browser data + browser-wide settings.
        dataStore = BrowserDataStore(this, slot)
        browserPrefs = BrowserPrefs(this)
        appearanceMode = browserPrefs.appearance
        val slotData = dataStore.load()
        historyList.addAll(slotData.history)
        bookmarkList.addAll(slotData.bookmarks)
        downloadList.addAll(slotData.downloads)
        recentClosedList.addAll(slotData.recentClosed)
        if (slotData.shortcuts == null) {
            // First run in this slot: seed the NTP shortcuts with our web
            // dashboard + the profile's start page (spec defaults).
            val startPage = loaded.browser.startPages.firstOrNull() ?: START_URL
            shortcutList.add(BrowserShortcut("Dashboard", WEB_DASHBOARD_URL))
            shortcutList.add(BrowserShortcut(hostLabel(startPage), startPage))
            persistSlotData()
        } else {
            shortcutList.addAll(slotData.shortcuts)
        }

        // Session timer: start (or keep) this run's start timestamp.
        sessionStartMs = browserPrefs.sessionStart(slot)
        if (sessionStartMs <= 0L) {
            sessionStartMs = System.currentTimeMillis()
            browserPrefs.setSessionStart(slot, sessionStartMs)
        }

        ContextCompat.registerReceiver(
            this, stopReceiver, IntentFilter(ACTION_STOP_SLOT),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        ContextCompat.registerReceiver(
            this, downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        setContent {
            DevxyzTheme(themeMode = appearanceMode) {
                BrowserContent()
            }
        }

        applyProxy(loaded) {
            proxyReady = true
            if (tabs.isEmpty()) {
                val saved = if (browserPrefs.restoreTabs) slotData.savedTabs else emptyList()
                if (saved.isNotEmpty()) {
                    saved.forEach { st ->
                        createTab(st.url, group = st.group)
                    }
                    val idx = slotData.savedActiveIndex.coerceIn(0, tabs.lastIndex)
                    tabs.getOrNull(idx)?.let { switchTab(it) }
                } else {
                    val start = when {
                        loaded.browser.startUrl == "specified" &&
                            loaded.browser.startPages.isNotEmpty() ->
                            loaded.browser.startPages.first()
                        else -> NTP_URL
                    }
                    createTab(start)
                }
            }
            if (apiMode) showConsole = true
        }
    }

    // ------------------------------------------------------------- settings

    private fun engine(): SearchEngine = SearchEngines.byId(browserPrefs.searchEngineId)

    /** Re-apply the WebSettings-backed browser settings to every live tab. */
    private fun applyBrowserSettingsToAll() {
        val cm = CookieManager.getInstance()
        tabs.forEach { tab ->
            val wv = tab.webView ?: return@forEach
            wv.settings.javaScriptEnabled = browserPrefs.javaScriptEnabled
            wv.settings.textZoom = browserPrefs.textZoom
            wv.settings.setGeolocationEnabled(browserPrefs.locationEnabled)
            cm.setAcceptThirdPartyCookies(wv, !browserPrefs.blockThirdPartyCookies)
        }
        cm.flush()
    }

    // ------------------------------------------------------------ persistence

    /** Write the whole per-slot data file from the in-memory lists. */
    private fun persistSlotData() {
        if (!::dataStore.isInitialized) return
        val savedTabs = if (browserPrefs.restoreTabs) {
            tabs.filter { !it.private }.map { SavedTab(it.url, it.title, it.group) }
        } else {
            emptyList()
        }
        val activeIdx = tabs.filter { !it.private }
            .indexOfFirst { it.id == activeTabId }.coerceAtLeast(0)
        dataStore.save(
            BrowserSlotData(
                history = historyList.toList(),
                bookmarks = bookmarkList.toList(),
                downloads = downloadList.toList(),
                shortcuts = shortcutList.toList(),
                recentClosed = recentClosedList.toList(),
                savedTabs = savedTabs,
                savedActiveIndex = activeIdx
            )
        )
    }

    private fun recordHistory(tab: BrowserTab, title: String, url: String) {
        if (tab.private) return
        if (profile?.browser?.saveHistory == false) return
        if (!url.startsWith("http")) return
        if (historyList.firstOrNull()?.url == url) return
        historyList.add(0, BrowserHistoryEntry(title, url, System.currentTimeMillis()))
        if (historyList.size > 500) {
            val trimmed = historyList.take(500)
            historyList.clear()
            historyList.addAll(trimmed)
        }
        persistSlotData()
    }

    // ------------------------------------------------------------------ tabs

    private fun activeTab(): BrowserTab? =
        tabs.firstOrNull { it.id == activeTabId } ?: tabs.firstOrNull()

    /** Where a "new tab" really lands (Homepage setting: NTP or engine home). */
    private fun effectiveNewTabUrl(): String =
        if (browserPrefs.homepageEnabled) NTP_URL else engine().homeUrl

    private fun createTab(
        initialUrl: String,
        group: String? = null,
        private: Boolean = false
    ): BrowserTab {
        val startUrl = if (initialUrl == NTP_URL) effectiveNewTabUrl() else initialUrl
        val tab = BrowserTab(nextTabId++)
        tab.group = group
        tab.private = private
        val wv = WebView(this)
        tab.webView = wv
        configureWebView(wv, tab)
        tab.url = startUrl
        tabs.add(tab)
        activeTabId = tab.id
        if (startUrl != NTP_URL) wv.loadUrl(startUrl)
        syncContainer()
        persistSlotData()
        return tab
    }

    private fun closeTab(tab: BrowserTab) {
        // Recently-closed restore list (never for private tabs / the NTP).
        if (!tab.private && tab.url != NTP_URL && tab.url.startsWith("http")) {
            recentClosedList.add(
                0,
                ClosedTabEntry(tab.title, tab.url, System.currentTimeMillis())
            )
            if (recentClosedList.size > 25) {
                val trimmed = recentClosedList.take(25)
                recentClosedList.clear()
                recentClosedList.addAll(trimmed)
            }
        }
        val wasActive = tab.id == activeTabId
        tab.webView?.let { wv ->
            (wv.parent as? ViewGroup)?.removeView(wv)
            if (tab.private) {
                // Private tab teardown: its (shared-cache) entries and its
                // back/forward list go with it. NOTE: WebView's cookie jar
                // is per data-directory, so cookies cannot be wiped for
                // one tab without logging out the whole slot — history is
                // the guarantee we make (see the NTP private chip).
                wv.clearCache(true)
                wv.clearHistory()
            }
            wv.destroy()
        }
        tabs.remove(tab)
        if (tabs.isEmpty()) {
            createTab(NTP_URL)
        } else if (wasActive) {
            activeTabId = tabs.last().id
        }
        syncContainer()
        persistSlotData()
    }

    private fun switchTab(tab: BrowserTab) {
        activeTabId = tab.id
        loadProgress = 0
        syncContainer()
        persistSlotData()
    }

    /** Keep the FrameLayout's children == the live tab WebViews; only the active one is visible. */
    private fun syncContainer() {
        val container = containerRef ?: return
        val active = activeTab()
        val live = tabs.mapNotNull { it.webView }
        for (i in container.childCount - 1 downTo 0) {
            val child = container.getChildAt(i)
            if (child !in live) container.removeView(child)
        }
        live.forEach { wv ->
            if (wv.parent == null) {
                container.addView(
                    wv,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
            }
        }
        tabs.forEach { tab ->
            tab.webView?.visibility =
                if (tab.id == active?.id && tab.url != NTP_URL) View.VISIBLE else View.GONE
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView(wv: WebView, tab: BrowserTab) {
        val p = profile ?: return
        val script = FingerprintScript.build(p)
        with(wv.settings) {
            javaScriptEnabled = browserPrefs.javaScriptEnabled
            domStorageEnabled = p.browser.localStorage
            databaseEnabled = true
            cacheMode = if (tab.private) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
            loadsImagesAutomatically = !p.browser.disableImageLoading
            userAgentString = p.fingerprint.navigator.userAgent
            useWideViewPort = true
            loadWithOverviewMode = true
            textZoom = browserPrefs.textZoom
            setGeolocationEnabled(browserPrefs.locationEnabled)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = false
            }
        }
        CookieManager.getInstance()
            .setAcceptThirdPartyCookies(wv, !browserPrefs.blockThirdPartyCookies)
        wv.setFindListener { _, numberOfMatches, isDoneCounting ->
            if (isDoneCounting && tab.id == activeTabId) {
                findResult = if (findQuery.isBlank()) "" else "$numberOfMatches matches"
            }
        }
        wv.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            startDownload(url, userAgent, contentDisposition, mimeType)
        }
        wv.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (tab.id == activeTabId) loadProgress = newProgress
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (!title.isNullOrBlank()) tab.title = title
            }

            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                if (icon != null) tab.favicon = icon
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: android.webkit.GeolocationPermissions.Callback?
            ) {
                // Real gate: the browser Settings → Site settings location
                // toggle decides whether pages may use location at all.
                callback?.invoke(origin, browserPrefs.locationEnabled, false)
            }
        }
        wv.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (url != null) tab.url = url
                // Inject as early as WebView lets us (document start is not
                // available; there is a small race with page scripts — a
                // known WebView limit vs. a real Chromium fork).
                view?.evaluateJavascript(script, null)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url != null) {
                    tab.url = url
                    val t = view?.title?.takeIf { it.isNotBlank() } ?: url
                    tab.title = t
                    recordHistory(tab, t, url)
                    persistSlotData()
                }
                tab.canBack = view?.canGoBack() == true
                tab.canForward = view?.canGoForward() == true
                if (tab.id == activeTabId) loadProgress = 100
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                // v2.3.1: a dead proxy used to surface as Chromium's raw
                // "net::ERR_PROXY_CONNECTION_FAILED" page on EVERY load.
                // Detect main-frame proxy failures and let BrowserContent
                // show our own error state (with a proxy-free escape).
                if (request?.isForMainFrame == true) {
                    // WebViewClient has no proxy-connection errorCode (the
                    // proxy-auth constant is a different failure); the
                    // Chromium net error string is the reliable signal.
                    val desc = error?.description?.toString().orEmpty()
                    if (desc.contains("ERR_PROXY")) {
                        proxyError = true
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------- navigation

    private fun resolveInput(raw: String): String {
        val q = raw.trim()
        if (q.isEmpty() || q == NTP_URL) return NTP_URL
        if (q.startsWith("http://") || q.startsWith("https://")) return q
        if (!q.contains(" ") && q.contains(".")) return "https://$q"
        return engine().searchPrefix + URLEncoder.encode(q, "UTF-8")
    }

    private fun navigateActive(raw: String) {
        // A fresh manual navigation dismisses a stale proxy-error state;
        // if the proxy is still dead it will simply be raised again.
        proxyError = false
        val tab = activeTab() ?: return
        val url = resolveInput(raw)
        if (url == NTP_URL) {
            if (!browserPrefs.homepageEnabled) {
                val home = engine().homeUrl
                tab.url = home
                tab.webView?.loadUrl(home)
                syncContainer()
                return
            }
            tab.url = NTP_URL
            tab.title = "New Tab"
            syncContainer()
            return
        }
        tab.url = url
        tab.webView?.loadUrl(url)
        syncContainer()
    }

    private fun setDesktop(tab: BrowserTab, desktop: Boolean) {
        tab.desktop = desktop
        val wv = tab.webView ?: return
        wv.settings.userAgentString =
            if (desktop) DESKTOP_UA else profile?.fingerprint?.navigator?.userAgent
        wv.settings.useWideViewPort = desktop
        wv.settings.loadWithOverviewMode = desktop
        if (tab.url != NTP_URL) wv.reload()
    }

    private fun stopProfile() {
        try {
            val store = ProfileStore(this)
            val s = store.loadReadOnly()
            store.save(
                s.copy(profiles = s.profiles.map {
                    if (it.id == profile?.id) it.copy(status = "ready") else it
                })
            )
        } catch (e: Exception) { /* best effort — list can still Stop us */ }
        if (::browserPrefs.isInitialized) browserPrefs.clearSessionStart(slot)
        finish()
    }

    // --------------------------------------------------------------- downloads

    /** Real download path: DownloadManager into the app Downloads folder,
     *  carrying the profile's UA + the WebView cookie jar, recorded in the
     *  per-slot downloads store (started → complete/failed). */
    private fun startDownload(
        url: String?,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?
    ) {
        if (url.isNullOrBlank() || !url.startsWith("http")) {
            Toast.makeText(this, "This link can't be downloaded", Toast.LENGTH_SHORT).show()
            return
        }
        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType) ?: "download"
        try {
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(fileName)
                .setDescription(url)
                .setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, fileName)
            if (!userAgent.isNullOrBlank()) {
                request.addRequestHeader("User-Agent", userAgent)
            }
            CookieManager.getInstance().getCookie(url)?.let {
                request.addRequestHeader("Cookie", it)
            }
            val dmId = dm.enqueue(request)
            downloadList.add(
                0,
                BrowserDownload(
                    dmId = dmId,
                    fileName = fileName,
                    url = url,
                    status = "downloading",
                    startedAt = System.currentTimeMillis()
                )
            )
            persistSlotData()
            Toast.makeText(this, "Downloading $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /** Reconcile in-flight records with DownloadManager's real state. */
    private fun refreshDownloadStatuses() {
        if (!::dataStore.isInitialized) return
        val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        var changed = false
        for (i in downloadList.indices) {
            val rec = downloadList[i]
            if (rec.dmId < 0 || rec.status != "downloading") continue
            try {
                dm.query(DownloadManager.Query().setFilterById(rec.dmId))?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val status = cursor.getInt(
                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                        )
                        val total = cursor.getLong(
                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        )
                        val newStatus = when (status) {
                            DownloadManager.STATUS_SUCCESSFUL -> "complete"
                            DownloadManager.STATUS_FAILED -> "failed"
                            else -> "downloading"
                        }
                        if (newStatus != rec.status || total != rec.totalBytes) {
                            downloadList[i] = rec.copy(status = newStatus, totalBytes = total)
                            changed = true
                        }
                    }
                }
            } catch (e: Exception) { /* record stays as-is */ }
        }
        if (changed) persistSlotData()
    }

    private fun openDownload(rec: BrowserDownload) {
        if (rec.status != "complete" || rec.dmId < 0) return
        try {
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = dm.getUriForDownloadedFile(rec.dmId)
            if (uri == null) {
                Toast.makeText(this, "File no longer available", Toast.LENGTH_SHORT).show()
                return
            }
            val mime = dm.getMimeTypeForDownloadedFile(rec.dmId) ?: "*/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                )
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No app can open this file", Toast.LENGTH_SHORT).show()
        }
    }

    // ------------------------------------------------------------ clear data

    /** Delete browsing data for THIS slot: cache, cookies, web storage,
     *  form data, per-WebView history + our persisted history store. */
    private fun clearBrowsingData() {
        tabs.forEach { tab ->
            tab.webView?.let { wv ->
                wv.clearCache(true)
                wv.clearHistory()
                wv.clearFormData()
            }
        }
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
        historyList.clear()
        persistSlotData()
        Toast.makeText(this, "Browsing data deleted", Toast.LENGTH_SHORT).show()
    }

    // ----------------------------------------------------------------- proxy

    /**
     * Per-profile proxy via androidx.webkit ProxyController.
     * Limits: HTTP/HTTPS proxy rules only — SOCKS and proxy AUTH are not
     * supported by ProxyController; we say so instead of failing silently.
     */
    private fun applyProxy(profile: Profile, done: () -> Unit) {
        // v2.3.1 run-path guard: never apply the fake demo host baked into
        // profiles by builds ≤ 2.3.0 — sanitized() maps it to "no proxy".
        val proxy = profile.proxy.sanitized()
        if (!proxy.isSet) {
            clearProxy(done)
            return
        }
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            Toast.makeText(this, "Proxy override not supported on this WebView version", Toast.LENGTH_LONG).show()
            done()
            return
        }
        if (proxy.type == "socks4" || proxy.type == "socks5") {
            Toast.makeText(this, "WebView proxy supports HTTP/HTTPS only — SOCKS proxy not applied", Toast.LENGTH_LONG).show()
            done()
            return
        }
        if (proxy.username.isNotBlank()) {
            Toast.makeText(this, "Proxy authentication is not supported by Android WebView — proxy applied without auth", Toast.LENGTH_LONG).show()
        }
        val config = ProxyConfig.Builder()
            .addProxyRule("${proxy.host}:${proxy.port}")
            .addBypassRule("localhost")
            .build()
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        try {
            ProxyController.getInstance().setProxyOverride(config, executor, {
                handler.post {
                    Toast.makeText(this, "Proxy applied: ${proxy.listLabel}", Toast.LENGTH_SHORT).show()
                    done()
                }
            })
        } catch (e: Exception) {
            Toast.makeText(this, "Proxy failed: ${e.message}", Toast.LENGTH_LONG).show()
            done()
        }
    }

    private fun clearProxy(done: () -> Unit) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            done()
            return
        }
        try {
            ProxyController.getInstance().clearProxyOverride(
                Executors.newSingleThreadExecutor(), { Handler(Looper.getMainLooper()).post(done) }
            )
        } catch (e: Exception) {
            done()
        }
    }

    /**
     * "Launch without proxy" from the proxy-error state: drop the proxy
     * override for THIS session only (the stored profile is untouched) and
     * reload the page the user actually wanted, direct.
     */
    private fun launchWithoutProxy() {
        proxyError = false
        clearProxy {
            val tab = activeTab() ?: return@clearProxy
            val target = tab.url
            if (target != NTP_URL) tab.webView?.loadUrl(target)
        }
    }

    /** Open one of the main app's existing Settings sections (account,
     *  Vault, API tokens…) — MainActivity honours EXTRA_OPEN_SECTION. */
    private fun openAppSection(section: String) {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_OPEN_SECTION, section)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Couldn't open the app settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hostLabel(url: String): String =
        Uri.parse(url).host?.removePrefix("www.")?.takeIf { it.isNotBlank() } ?: url

    override fun onDestroy() {
        try { unregisterReceiver(stopReceiver) } catch (e: Exception) { /* already gone */ }
        try { unregisterReceiver(downloadReceiver) } catch (e: Exception) { /* already gone */ }
        tabs.forEach { it.webView?.destroy() }
        tabs.clear()
        super.onDestroy()
    }

    // ------------------------------------------------------------- palette

    @Composable
    private fun orbitaBg(): Color = if (LocalDarkTheme.current) OrbitaBgDark else OrbitaBgLight

    @Composable
    private fun orbitaPill(): Color = if (LocalDarkTheme.current) OrbitaPillDark else OrbitaPillLight

    @Composable
    private fun orbitaCircle(): Color =
        if (LocalDarkTheme.current) OrbitaCircleDark else OrbitaCircleLight

    @Composable
    private fun orbitaPanel(): Color =
        if (LocalDarkTheme.current) OrbitaPanelDark else OrbitaPanelLight

    @Composable
    private fun orbitaRow(): Color = if (LocalDarkTheme.current) OrbitaRowDark else OrbitaRowLight

    @Composable
    private fun orbitaText2(): Color =
        if (LocalDarkTheme.current) OrbitaText2Dark else OrbitaText2Light

    @Composable
    private fun orbitaOnBg(): Color =
        if (LocalDarkTheme.current) Color(0xFFF7EFEC) else Color(0xFF1E1714)

    private fun formatElapsed(totalSec: Long): String {
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    // -------------------------------------------------------------------- UI

    @Composable
    private fun BrowserContent() {
        val p = profile ?: return
        val tab = activeTab()
        val bottomBar = browserPrefs.addressBarPosition == "bottom"

        // Session timer tick (real elapsed seconds, 1s cadence).
        LaunchedEffect(sessionStartMs) {
            while (true) {
                elapsedSec = ((System.currentTimeMillis() - sessionStartMs) / 1000).coerceAtLeast(0)
                delay(1000)
            }
        }

        BackHandler(
            enabled = findOpen || showSwitcher || panel != null || settingsRoute != "root"
        ) {
            when {
                settingsRoute != "root" && panel == BrowserPanel.SETTINGS ->
                    settingsRoute = "root"
                panel != null -> panel = null
                showSwitcher -> showSwitcher = false
                findOpen -> {
                    findOpen = false
                    tab?.webView?.clearMatches()
                }
            }
        }
        BackHandler(
            enabled = !findOpen && !showSwitcher && panel == null && tab?.canBack == true
        ) {
            tab?.webView?.goBack()
        }

        LaunchedEffect(activeTabId, tabs.size) { syncContainer() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(orbitaBg())
        ) {
            if (!bottomBar) {
                BrowserTopBar(tab)
                ProgressLine()
            }

            // ---- Page area
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(
                    factory = { ctx ->
                        FrameLayout(ctx).also {
                            containerRef = it
                            syncContainer()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                if (tab != null && tab.url == NTP_URL) {
                    NewTabPage(tab) { navigateActive(it) }
                }
                if (!proxyReady) {
                    // GoLogin pages render blank → centered spinner → content
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandGreen)
                    }
                }
                if (proxyError) {
                    // v2.3.1: the profile's proxy could not be reached
                    // (ERR_PROXY_*) — our own error state instead of
                    // Chromium's raw error page, with a proxy-free escape.
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(orbitaBg())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Proxy connection failed",
                            style = MaterialTheme.typography.titleMedium,
                            color = orbitaOnBg()
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "This profile's proxy (${p.proxy.listLabel}) could not be " +
                                "reached, so pages can't load. Check the proxy in the " +
                                "profile settings, or open this session without one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = orbitaText2(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = { launchWithoutProxy() }) {
                            Text("Launch without proxy")
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { stopProfile() }) {
                            Text("Close")
                        }
                    }
                }
            }

            if (findOpen) FindBar(tab)
            if (bottomBar) {
                ProgressLine()
                BrowserTopBar(tab)
            }
        }

        if (showSwitcher) TabSwitcher()
        when (panel) {
            BrowserPanel.HISTORY -> HistoryPanel()
            BrowserPanel.DOWNLOADS -> DownloadsPanel()
            BrowserPanel.BOOKMARKS -> BookmarksPanel(p)
            BrowserPanel.RECENT -> RecentTabsPanel()
            BrowserPanel.SETTINGS -> BrowserSettingsPanel(p)
            null -> Unit
        }
        if (showConsole) ConsoleSheet(p) { showConsole = false }
        if (showStopConfirm) {
            ConfirmSheet(
                title = "Stop profile",
                message = "Stop ${p.name} and close this browser session? " +
                    "The profile and its data stay saved.",
                confirmLabel = "Stop profile",
                onConfirm = {
                    showStopConfirm = false
                    stopProfile()
                },
                onDismiss = { showStopConfirm = false }
            )
        }
        if (showSiteInfo) SiteInfoSheet(p) { showSiteInfo = false }
        if (showProfileInfo) ProfileInfoSheet(p) { showProfileInfo = false }
        if (showGroupSheet) GroupSheet { showGroupSheet = false }
        if (showAddShortcut) AddShortcutSheet { showAddShortcut = false }
        if (showCustomize) CustomizeSheet { showCustomize = false }
        if (showHelp) HelpSheet(p) { showHelp = false }
        if (showClearDataConfirm) {
            ConfirmSheet(
                title = "Delete browsing data",
                message = "Delete history, cookies, cache and site data for this " +
                    "profile slot? You'll be signed out of sites in this slot.",
                confirmLabel = "Delete",
                onConfirm = {
                    showClearDataConfirm = false
                    clearBrowsingData()
                },
                onDismiss = { showClearDataConfirm = false }
            )
        }
    }

    @Composable
    private fun ProgressLine() {
        if (loadProgress in 1..99) {
            LinearProgressIndicator(
                progress = { loadProgress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = BrandGreenLight,
                trackColor = Color.Transparent
            )
        }
    }

    @Composable
    private fun BrowserTopBar(tab: BrowserTab?) {
        val url = tab?.url ?: NTP_URL
        val onBg = orbitaOnBg()
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Green logo tile (our mark) — opens the profile/session sheet.
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(BrandGreen)
                        .clickable { showProfileInfo = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.splash_logo_white),
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                if (url != NTP_URL) {
                    // Address pill: favicon / lock / globe + editable omnibox.
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(orbitaPill())
                            .padding(start = 12.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val favicon = tab?.favicon
                        when {
                            favicon != null -> Image(
                                bitmap = favicon.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            url.startsWith("https://") -> Icon(
                                Icons.Filled.Lock, contentDescription = "Secure",
                                tint = BrandGreen, modifier = Modifier.size(17.dp)
                            )
                            else -> Icon(
                                Icons.Filled.Public, contentDescription = null,
                                tint = orbitaText2(), modifier = Modifier.size(17.dp)
                            )
                        }
                        if (tab?.private == true) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Filled.VisibilityOff, contentDescription = "Private tab",
                                tint = orbitaText2(), modifier = Modifier.size(16.dp)
                            )
                        }
                        Omnibox(
                            tab = tab,
                            modifier = Modifier.weight(1f),
                            onSubmit = { navigateActive(it) }
                        )
                    }
                    IconButton(onClick = { createTab(NTP_URL) }) {
                        Icon(Icons.Filled.Add, contentDescription = "New tab", tint = onBg)
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                // Session timer pill — real elapsed run time; tap = stop sheet.
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(orbitaPill())
                        .clickable { showStopConfirm = true }
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Pause, contentDescription = "Session timer",
                        tint = onBg, modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        formatElapsed(elapsedSec),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = onBg
                    )
                }
                Spacer(Modifier.width(6.dp))
                // Tab-count square (Orbita/Chromium style)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .border(2.dp, onBg, RoundedCornerShape(7.dp))
                        .clickable { showSwitcher = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        tabs.size.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = onBg
                    )
                }
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Browser menu", tint = onBg)
                }
            }
            if (menuOpen) BrowserMenuPanel(tab)
        }
    }

    @Composable
    private fun Omnibox(tab: BrowserTab?, modifier: Modifier = Modifier, onSubmit: (String) -> Unit) {
        var editing by remember { mutableStateOf(false) }
        var text by remember { mutableStateOf("") }
        val current = tab?.url ?: NTP_URL
        val shown = if (editing) text else displayUrl(current)
        Box(
            modifier = modifier.padding(horizontal = 10.dp, vertical = 9.dp)
        ) {
            BasicTextField(
                value = shown,
                onValueChange = {
                    text = it
                    editing = true
                },
                singleLine = true,
                textStyle = TextStyle(
                    color = orbitaOnBg(),
                    fontSize = 15.sp
                ),
                cursorBrush = SolidColor(BrandGreen),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    onSubmit(if (editing) text else current)
                    editing = false
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focus ->
                        if (focus.isFocused && !editing) {
                            text = if (current == NTP_URL) "" else current
                            editing = true
                        } else if (!focus.isFocused) {
                            editing = false
                        }
                    }
            )
        }
    }

    private fun displayUrl(url: String): String = when {
        url == NTP_URL -> ""
        url.startsWith("https://") -> url.removePrefix("https://").removeSuffix("/")
        url.startsWith("http://") -> url.removePrefix("http://").removeSuffix("/")
        else -> url
    }

    /** The ⋮ menu: warm panel anchored top-right (spec §menu). */
    @Composable
    private fun BrowserMenuPanel(tab: BrowserTab?) {
        val density = LocalDensity.current
        val onBg = orbitaOnBg()
        val text2 = orbitaText2()
        val url = tab?.url ?: NTP_URL
        val hasPage = url != NTP_URL && url.startsWith("http")
        val bookmarked = hasPage && bookmarkList.any { it.url == url }
        val bottomBar = browserPrefs.addressBarPosition == "bottom"
        Popup(
            alignment = if (bottomBar) Alignment.BottomEnd else Alignment.TopEnd,
            offset = IntOffset(
                0,
                with(density) { (if (bottomBar) (-54).dp else 54.dp).roundToPx() }
            ),
            onDismissRequest = { menuOpen = false },
            properties = androidx.compose.ui.window.PopupProperties(focusable = true)
        ) {
            Card(
                modifier = Modifier
                    .width(324.dp)
                    .padding(end = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = orbitaPanel()),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 10.dp)
                ) {
                    // ---- Icon row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MenuIconButton(Icons.Filled.Home, "Home", onBg, enabled = true) {
                            menuOpen = false
                            navigateActive(NTP_URL)
                        }
                        MenuIconButton(
                            Icons.Filled.ArrowForward, "Forward", onBg,
                            enabled = tab?.canForward == true
                        ) {
                            menuOpen = false
                            tab?.webView?.goForward()
                        }
                        MenuIconButton(
                            Icons.Filled.Star, "Bookmark",
                            if (bookmarked) BrandGreen else onBg,
                            enabled = hasPage
                        ) {
                            menuOpen = false
                            toggleBookmark(tab)
                        }
                        MenuIconButton(
                            Icons.Filled.Download, "Download", onBg,
                            enabled = hasPage
                        ) {
                            menuOpen = false
                            if (tab != null) {
                                startDownload(
                                    tab.url,
                                    tab.webView?.settings?.userAgentString,
                                    null, null
                                )
                            }
                        }
                        MenuIconButton(Icons.Filled.Info, "Site info", onBg, enabled = hasPage) {
                            menuOpen = false
                            showSiteInfo = true
                        }
                        MenuIconButton(
                            Icons.Filled.Refresh, "Refresh", onBg,
                            enabled = url != NTP_URL
                        ) {
                            menuOpen = false
                            tab?.webView?.reload()
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    // ---- Items
                    MenuItem(Icons.Filled.Add, "New tab", onBg) {
                        menuOpen = false
                        createTab(NTP_URL)
                    }
                    MenuItem(Icons.Filled.VisibilityOff, "New Incognito tab", onBg) {
                        menuOpen = false
                        createTab(NTP_URL, private = true)
                    }
                    MenuItem(Icons.Filled.GridView, "Add tab to new group", onBg) {
                        menuOpen = false
                        showGroupSheet = true
                    }
                    MenuDivider()
                    MenuItem(Icons.Filled.History, "History", onBg) {
                        menuOpen = false
                        panel = BrowserPanel.HISTORY
                    }
                    MenuItem(Icons.Filled.Delete, "Delete browsing data", onBg) {
                        menuOpen = false
                        showClearDataConfirm = true
                    }
                    MenuDivider()
                    MenuItem(Icons.Filled.Download, "Downloads", onBg) {
                        menuOpen = false
                        refreshDownloadStatuses()
                        panel = BrowserPanel.DOWNLOADS
                    }
                    MenuItem(Icons.Filled.Bookmark, "Bookmarks", onBg) {
                        menuOpen = false
                        panel = BrowserPanel.BOOKMARKS
                    }
                    MenuItem(Icons.Filled.Restore, "Recent tabs", onBg) {
                        menuOpen = false
                        panel = BrowserPanel.RECENT
                    }
                    MenuDivider()
                    MenuItem(Icons.Filled.Settings, "Settings", onBg) {
                        menuOpen = false
                        settingsRoute = "root"
                        panel = BrowserPanel.SETTINGS
                    }
                    MenuItem(Icons.Filled.Edit, "Customize new tab page", onBg) {
                        menuOpen = false
                        showCustomize = true
                    }
                    MenuItem(Icons.Filled.Help, "Help & feedback", onBg) {
                        menuOpen = false
                        showHelp = true
                    }
                    MenuDivider()
                    MenuItem(Icons.Filled.Search, "Find in page", onBg) {
                        menuOpen = false
                        findQuery = ""
                        findResult = ""
                        findOpen = true
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                menuOpen = false
                                if (tab != null) setDesktop(tab, !tab.desktop)
                            }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.DesktopWindows, contentDescription = null,
                            tint = onBg, modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Text("Desktop site", color = onBg, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Checkbox(checked = tab?.desktop == true, onCheckedChange = { checked ->
                            menuOpen = false
                            if (tab != null) setDesktop(tab, checked)
                        })
                    }
                    MenuItem(Icons.Filled.Share, "Share page", onBg, enabled = hasPage) {
                        menuOpen = false
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, url)
                        }
                        startActivity(Intent.createChooser(send, "Share page"))
                    }
                    MenuDivider()
                    MenuItem(Icons.Filled.StopCircle, "Stop profile", DangerRed) {
                        menuOpen = false
                        showStopConfirm = true
                    }
                    Text(
                        "Session ${formatElapsed(elapsedSec)} · ${profile?.name ?: ""}",
                        color = text2,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    @Composable
    private fun MenuIconButton(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        label: String,
        tint: Color,
        enabled: Boolean,
        onClick: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(orbitaRow())
                .clickable(enabled = enabled) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, contentDescription = label,
                tint = if (enabled) tint else tint.copy(alpha = 0.35f),
                modifier = Modifier.size(21.dp)
            )
        }
    }

    @Composable
    private fun MenuItem(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        label: String,
        tint: Color,
        enabled: Boolean = true,
        onClick: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onClick() }
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon, contentDescription = null,
                tint = if (enabled) tint else tint.copy(alpha = 0.35f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                label,
                color = if (enabled) tint else tint.copy(alpha = 0.35f),
                fontSize = 15.sp
            )
        }
    }

    @Composable
    private fun MenuDivider() {
        HorizontalDivider(
            color = orbitaText2().copy(alpha = 0.25f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
    }

    private fun toggleBookmark(tab: BrowserTab?) {
        if (tab == null) return
        val url = tab.url
        if (url == NTP_URL || !url.startsWith("http")) return
        val existing = bookmarkList.firstOrNull { it.url == url }
        if (existing != null) {
            bookmarkList.remove(existing)
            Toast.makeText(this, "Bookmark removed", Toast.LENGTH_SHORT).show()
        } else {
            bookmarkList.add(
                0,
                BrowserBookmark(tab.title, url, System.currentTimeMillis())
            )
            Toast.makeText(this, "Bookmarked", Toast.LENGTH_SHORT).show()
        }
        persistSlotData()
    }

    @Composable
    private fun FindBar(tab: BrowserTab?) {
        Surface(shadowElevation = 6.dp, color = orbitaPanel()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = findQuery,
                    onValueChange = {
                        findQuery = it
                        if (it.isBlank()) {
                            tab?.webView?.clearMatches()
                            findResult = ""
                        } else {
                            tab?.webView?.findAllAsync(it)
                        }
                    },
                    placeholder = { Text("Find in page") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                if (findResult.isNotBlank()) {
                    Text(
                        findResult,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }
                IconButton(onClick = { tab?.webView?.findNext(false) }) {
                    Icon(Icons.Filled.ArrowUpward, contentDescription = "Previous match")
                }
                IconButton(onClick = { tab?.webView?.findNext(true) }) {
                    Icon(Icons.Filled.ArrowDownward, contentDescription = "Next match")
                }
                IconButton(onClick = {
                    findOpen = false
                    tab?.webView?.clearMatches()
                }) {
                    Icon(Icons.Filled.Close, contentDescription = "Close find bar")
                }
            }
        }
    }

    // ------------------------------------------------------------ New Tab page

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun NewTabPage(tab: BrowserTab, onOpen: (String) -> Unit) {
        val eng = engine()
        val onBg = orbitaOnBg()
        val text2 = orbitaText2()
        var query by remember { mutableStateOf("") }

        // Voice search: the system speech recognizer, when one exists.
        val speechLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) onOpen(spoken)
        }
        val micAvailable = remember {
            try {
                packageManager.queryIntentActivities(
                    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0
                ).isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(orbitaBg())
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(64.dp))
            // Search-engine wordmark (the chosen engine, Chromium-NTP style)
            Text(
                eng.name,
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = onBg
            )
            if (tab.private) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(orbitaPill())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.VisibilityOff, contentDescription = null,
                        tint = text2, modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Private tab — history isn't saved",
                        fontSize = 12.sp, color = text2
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            // Search pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(orbitaPill())
                    .padding(start = 18.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    eng.name.firstOrNull()?.uppercase() ?: "S",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandGreen
                )
                Spacer(Modifier.width(12.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = onBg, fontSize = 16.sp),
                    cursorBrush = SolidColor(BrandGreen),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onOpen(query) }),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(
                                "Search ${eng.name} or type URL",
                                color = text2, fontSize = 16.sp
                            )
                        }
                        inner()
                    },
                    modifier = Modifier.weight(1f)
                )
                if (micAvailable) {
                    IconButton(onClick = {
                        try {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH
                                )
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Search ${eng.name}")
                            }
                            speechLauncher.launch(intent)
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@BrowserActivityBase,
                                "Voice search isn't available",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }) {
                        Icon(Icons.Filled.Mic, contentDescription = "Voice search", tint = onBg)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            // AI Mode / Incognito pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(orbitaPill())
                        .clickable { onOpen(SearchEngines.aiModeUrl(query)) }
                        .padding(vertical = 13.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.AutoAwesome, contentDescription = null,
                        tint = onBg, modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("AI Mode", color = onBg, fontSize = 15.sp)
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(orbitaPill())
                        .clickable { createTab(NTP_URL, private = true) }
                        .padding(vertical = 13.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.VisibilityOff, contentDescription = null,
                        tint = onBg, modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Incognito", color = onBg, fontSize = 15.sp)
                }
            }
            // Shortcut circles
            if (browserPrefs.showShortcuts) {
                Spacer(Modifier.height(26.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    items(shortcutList, key = { it.url }) { shortcut ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(64.dp)
                                .combinedClickable(
                                    onClick = { onOpen(shortcut.url) },
                                    onLongClick = {
                                        shortcutList.remove(shortcut)
                                        persistSlotData()
                                        Toast.makeText(
                                            this@BrowserActivityBase,
                                            "Shortcut removed",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(orbitaCircle()),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    shortcut.title.firstOrNull()?.uppercase() ?: "?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = BrandGreen
                                )
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(
                                shortcut.title,
                                fontSize = 11.sp,
                                color = onBg,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(64.dp)
                                .clickable { showAddShortcut = true }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(orbitaCircle()),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Add, contentDescription = "Add shortcut",
                                    tint = onBg, modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.height(5.dp))
                            Text("Add", fontSize = 11.sp, color = onBg)
                        }
                    }
                }
            }
            // Tips carousel
            if (browserPrefs.showTips) {
                Spacer(Modifier.height(30.dp))
                Text(
                    "DevxyzLogin tips",
                    fontSize = 17.sp,
                    color = onBg,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                TipsCarousel(tab)
            }
            // Stories (static section, as in Orbita)
            if (browserPrefs.showStories) {
                Spacer(Modifier.height(30.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(orbitaCircle())
                        .padding(18.dp)
                ) {
                    Text(
                        "No stories available",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = onBg
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Check back later for new stories",
                        fontSize = 14.sp,
                        color = text2
                    )
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    @Composable
    private fun TipsCarousel(tab: BrowserTab) {
        val onBg = orbitaOnBg()
        val text2 = orbitaText2()
        val tips = listOf(
            Triple(
                "Enhance your security",
                "Check that this profile's proxy really answers before you browse.",
                "Check proxy"
            ),
            Triple(
                "Choose address bar position",
                "Switch the address bar to the bottom for one-handed browsing.",
                "Try it"
            ),
            Triple(
                "Per-profile fingerprints",
                "Every profile runs with its own user agent, platform and timezone.",
                "View fingerprint"
            )
        )
        val pagerState = rememberPagerState { tips.size }
        Column {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val (title, body, action) = tips[page]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(orbitaCircle())
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(orbitaPill()),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            when (page) {
                                0 -> Icons.Filled.Security
                                1 -> Icons.Filled.Apps
                                else -> Icons.Filled.Info
                            },
                            contentDescription = null,
                            tint = BrandGreen,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            title, fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold, color = onBg
                        )
                        Text(body, fontSize = 13.sp, color = text2)
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(BrandGreen)
                                .clickable {
                                    when (page) {
                                        0 -> {
                                            settingsRoute = "privacy"
                                            panel = BrowserPanel.SETTINGS
                                        }
                                        1 -> {
                                            settingsRoute = "addressbar"
                                            panel = BrowserPanel.SETTINGS
                                        }
                                        else -> showProfileInfo = true
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(action, fontSize = 13.sp, color = Color.White)
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                tips.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (pagerState.currentPage == i) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == i) onBg else text2.copy(alpha = 0.5f)
                            )
                    )
                }
            }
        }
    }

    // ------------------------------------------------------------------ sheets

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SiteInfoSheet(p: Profile, onClose: () -> Unit) {
        val tab = activeTab()
        val url = tab?.url ?: ""
        val secure = url.startsWith("https://")
        val check = p.lastProxyCheck
        AppBottomSheet(onDismiss = onClose) {
            Column(modifier = Modifier.padding(bottom = 20.dp)) {
                SheetTitle("Site info")
                Spacer(Modifier.height(6.dp))
                InfoRow("Page", if (url == NTP_URL || url.isBlank()) "New Tab" else url)
                InfoRow(
                    "Connection",
                    when {
                        url == NTP_URL -> "—"
                        secure -> "Secure (HTTPS)"
                        url.startsWith("http://") -> "Not secure (HTTP)"
                        else -> "—"
                    }
                )
                InfoRow("Profile", p.name)
                InfoRow("Proxy", p.proxy.sanitized().listLabel)
                if (check != null && (check.ip.isNotBlank() || check.country.isNotBlank())) {
                    InfoRow(
                        "Exit (last check)",
                        listOf(check.ip, check.place).filter { it.isNotBlank() }.joinToString(" · ")
                    )
                }
                if (tab?.private == true) {
                    InfoRow("Tab", "Private — history isn't saved for this tab")
                }
                tab?.group?.let { InfoRow("Tab group", it) }
            }
        }
    }

    @Composable
    private fun InfoRow(label: String, value: String) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ProfileInfoSheet(p: Profile, onClose: () -> Unit) {
        val fp = p.fingerprint
        AppBottomSheet(onDismiss = onClose) {
            Column(modifier = Modifier.padding(bottom = 20.dp)) {
                SheetTitle(p.name)
                Spacer(Modifier.height(6.dp))
                InfoRow("Session", "Running for ${formatElapsed(elapsedSec)} · slot $slot")
                InfoRow("Proxy", p.proxy.sanitized().listLabel)
                InfoRow("User agent", fp.navigator.userAgent)
                InfoRow("Platform", fp.navigator.platform)
                InfoRow("Screen", fp.navigator.screenResolution)
                InfoRow("Languages", fp.navigator.languages.joinToString(", "))
                InfoRow(
                    "Timezone",
                    if (fp.timezone.auto) "Based on IP" else fp.timezone.zone
                )
                InfoRow("WebRTC", fp.webrtcMode)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                    OutlinedButton(onClick = {
                        onClose()
                        showStopConfirm = true
                    }) { Text("Stop profile", color = DangerRed) }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun GroupSheet(onClose: () -> Unit) {
        var name by remember { mutableStateOf("") }
        val tab = activeTab()
        AppBottomSheet(onDismiss = onClose) {
            Column(modifier = Modifier.padding(bottom = 20.dp)) {
                SheetTitle("Add tab to new group")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                tab?.group?.let {
                    Text(
                        "This tab is currently in \"$it\" — saving replaces it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val trimmed = name.trim()
                        if (trimmed.isNotBlank() && tab != null) {
                            tab.group = trimmed
                            persistSlotData()
                            Toast.makeText(
                                this@BrowserActivityBase,
                                "Tab added to \"$trimmed\"",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        onClose()
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Create group") }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun AddShortcutSheet(onClose: () -> Unit) {
        val tab = activeTab()
        val currentUrl = tab?.url?.takeIf { it.startsWith("http") }
        var title by remember { mutableStateOf(currentUrl?.let { hostLabel(it) } ?: "") }
        var url by remember { mutableStateOf(currentUrl ?: "") }
        AppBottomSheet(onDismiss = onClose) {
            Column(modifier = Modifier.padding(bottom = 20.dp)) {
                SheetTitle("Add shortcut")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val resolved = resolveInput(url)
                        if (resolved != NTP_URL) {
                            // One shortcut per URL (LazyRow keys on it).
                            shortcutList.removeAll { it.url == resolved }
                            shortcutList.add(
                                BrowserShortcut(
                                    title.ifBlank { hostLabel(resolved) },
                                    resolved
                                )
                            )
                            persistSlotData()
                        }
                        onClose()
                    },
                    enabled = url.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Add") }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun CustomizeSheet(onClose: () -> Unit) {
        var shortcuts by remember { mutableStateOf(browserPrefs.showShortcuts) }
        var tips by remember { mutableStateOf(browserPrefs.showTips) }
        var stories by remember { mutableStateOf(browserPrefs.showStories) }
        AppBottomSheet(onDismiss = onClose) {
            Column(modifier = Modifier.padding(bottom = 20.dp)) {
                SheetTitle("Customize new tab page")
                Spacer(Modifier.height(6.dp))
                ToggleRow("Shortcuts", shortcuts) {
                    shortcuts = it
                    browserPrefs.showShortcuts = it
                }
                ToggleRow("Tips carousel", tips) {
                    tips = it
                    browserPrefs.showTips = it
                }
                ToggleRow("Stories section", stories) {
                    stories = it
                    browserPrefs.showStories = it
                }
                Text(
                    "Changes apply to the next New Tab page you open.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }

    @Composable
    private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun HelpSheet(p: Profile, onClose: () -> Unit) {
        val versionName = remember {
            try {
                packageManager.getPackageInfo(packageName, 0).versionName ?: "—"
            } catch (e: Exception) {
                "—"
            }
        }
        AppBottomSheet(onDismiss = onClose) {
            Column(modifier = Modifier.padding(bottom = 20.dp)) {
                SheetTitle("Help & feedback")
                Spacer(Modifier.height(6.dp))
                InfoRow("App", "DevxyzLogin $versionName")
                InfoRow("Profile", "${p.name} · slot $slot")
                InfoRow("Browser engine", "Android WebView (per-profile data directory)")
                Spacer(Modifier.height(6.dp))
                TextButton(
                    onClick = {
                        onClose()
                        createTab(WEB_DASHBOARD_URL)
                    },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) { Text("Open the DevxyzLogin dashboard") }
                TextButton(
                    onClick = {
                        onClose()
                        openAppSection("overview")
                    },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) { Text("Open app account & settings") }
            }
        }
    }

    // ------------------------------------------------------------ tab switcher

    @Composable
    private fun TabSwitcher() {
        val onBg = orbitaOnBg()
        Surface(modifier = Modifier.fillMaxSize(), color = orbitaBg()) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Tabs",
                        style = MaterialTheme.typography.titleLarge,
                        color = onBg,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { createTab(NTP_URL) }) {
                        Icon(Icons.Filled.Add, contentDescription = "New tab", tint = onBg)
                    }
                    IconButton(onClick = { showSwitcher = false }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close tab switcher", tint = onBg)
                    }
                }
                Spacer(Modifier.height(8.dp))
                // Sections: tab groups first (in first-appearance order),
                // ungrouped tabs under their own header when groups exist.
                val groups = tabs.mapNotNull { it.group }.distinct()
                val sections = mutableListOf<Pair<String?, List<BrowserTab>>>()
                groups.forEach { g -> sections.add(g to tabs.filter { it.group == g }) }
                val ungrouped = tabs.filter { it.group == null }
                if (ungrouped.isNotEmpty()) {
                    sections.add((if (groups.isEmpty()) null else "Ungrouped") to ungrouped)
                }
                val showHeaders = groups.isNotEmpty()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    sections.forEach { (group, sectionTabs) ->
                        if (showHeaders) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.GridView, contentDescription = null,
                                        tint = BrandGreen, modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        group ?: "Tabs",
                                        fontWeight = FontWeight.SemiBold,
                                        color = onBg
                                    )
                                }
                            }
                        }
                        items(sectionTabs, key = { it.id }) { tab -> TabCard(tab) }
                    }
                }
            }
        }
    }

    @Composable
    private fun TabCard(tab: BrowserTab) {
        val isActive = tab.id == activeTabId
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isActive) Modifier.border(
                        2.dp, BrandGreen, RoundedCornerShape(12.dp)
                    ) else Modifier
                )
                .clickable {
                    switchTab(tab)
                    showSwitcher = false
                },
            colors = CardDefaults.cardColors(containerColor = orbitaRow())
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val favicon = tab.favicon
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(orbitaCircle()),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            tab.private -> Icon(
                                Icons.Filled.VisibilityOff, contentDescription = "Private tab",
                                tint = orbitaText2(), modifier = Modifier.size(17.dp)
                            )
                            favicon != null -> Image(
                                bitmap = favicon.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            else -> Text(
                                tab.title.firstOrNull()?.uppercase() ?: "N",
                                fontWeight = FontWeight.Bold,
                                color = BrandGreen
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = { closeTab(tab) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close tab",
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    tab.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    when {
                        tab.private && tab.url == NTP_URL -> "Private tab"
                        tab.url == NTP_URL -> "New Tab"
                        else -> displayUrl(tab.url)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    // ------------------------------------------------------------------ panels

    @Composable
    private fun PanelScaffold(
        title: String,
        onClose: () -> Unit,
        actions: @Composable () -> Unit = {},
        content: @Composable () -> Unit
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = orbitaBg()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Filled.ArrowBack, contentDescription = "Back",
                            tint = orbitaOnBg()
                        )
                    }
                    Text(
                        title,
                        style = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Bold),
                        color = orbitaOnBg(),
                        modifier = Modifier.weight(1f)
                    )
                    actions()
                }
                content()
            }
        }
    }

    @Composable
    private fun PanelEmpty(text: String) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = orbitaText2(),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
        )
    }

    @Composable
    private fun HistoryPanel() {
        PanelScaffold(
            title = "History",
            onClose = { panel = null },
            actions = {
                if (historyList.isNotEmpty()) {
                    TextButton(onClick = {
                        historyList.clear()
                        persistSlotData()
                        Toast.makeText(
                            this@BrowserActivityBase, "History cleared",
                            Toast.LENGTH_SHORT
                        ).show()
                    }) { Text("Clear") }
                }
            }
        ) {
            if (historyList.isEmpty()) {
                PanelEmpty("No history yet in this profile slot. Pages you visit " +
                    "(outside private tabs) are kept here.")
            } else {
                val timeFmt = remember { SimpleDateFormat("MMM d, HH:mm", Locale.US) }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(historyList, key = { it.url + it.visitedAt }) { entry ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    panel = null
                                    navigateActive(entry.url)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                entry.title.ifBlank { entry.url },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = orbitaOnBg()
                            )
                            Text(
                                "${displayUrl(entry.url)} · ${timeFmt.format(Date(entry.visitedAt))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = orbitaText2(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun DownloadsPanel() {
        LaunchedEffect(Unit) { refreshDownloadStatuses() }
        PanelScaffold(title = "Downloads", onClose = { panel = null }) {
            if (downloadList.isEmpty()) {
                PanelEmpty("Nothing downloaded yet. Files you download in this " +
                    "browser are saved to the app's Downloads folder and listed here.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(downloadList, key = { it.dmId.toString() + it.startedAt }) { rec ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = rec.status == "complete") {
                                    openDownload(rec)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Download, contentDescription = null,
                                tint = orbitaText2(), modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    rec.fileName,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = orbitaOnBg()
                                )
                                Text(
                                    when (rec.status) {
                                        "complete" -> "Complete" +
                                            if (rec.totalBytes > 0) " · ${formatBytes(rec.totalBytes)}" else ""
                                        "failed" -> "Failed"
                                        else -> "Downloading…"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = orbitaText2(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = {
                                downloadList.remove(rec)
                                persistSlotData()
                            }) {
                                Icon(
                                    Icons.Filled.Delete, contentDescription = "Remove from list",
                                    tint = orbitaText2(), modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
        bytes >= 1_000 -> "%.0f KB".format(bytes / 1_000.0)
        else -> "$bytes B"
    }

    @Composable
    private fun BookmarksPanel(p: Profile) {
        PanelScaffold(title = "Bookmarks", onClose = { panel = null }) {
            val profileLinks = remember(p.bookmarks) { DefaultBookmarks.links(p.bookmarks) }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (bookmarkList.isEmpty() && profileLinks.isEmpty()) {
                    item {
                        PanelEmpty("No bookmarks yet. Use the star in the ⋮ menu " +
                            "to bookmark the current page.")
                    }
                }
                items(bookmarkList, key = { it.url }) { bm ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                panel = null
                                navigateActive(bm.url)
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Star, contentDescription = null,
                            tint = BrandGreen, modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                bm.title.ifBlank { bm.url },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = orbitaOnBg()
                            )
                            Text(
                                displayUrl(bm.url),
                                style = MaterialTheme.typography.bodySmall,
                                color = orbitaText2(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = {
                            bookmarkList.remove(bm)
                            persistSlotData()
                        }) {
                            Icon(
                                Icons.Filled.Delete, contentDescription = "Delete bookmark",
                                tint = orbitaText2(), modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
                if (profileLinks.isNotEmpty()) {
                    item {
                        Text(
                            "From this profile",
                            fontWeight = FontWeight.SemiBold,
                            color = orbitaOnBg(),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                    items(profileLinks) { (_, name, url) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    panel = null
                                    navigateActive(url)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis, color = orbitaOnBg())
                            Text(
                                displayUrl(url),
                                style = MaterialTheme.typography.bodySmall,
                                color = orbitaText2(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun RecentTabsPanel() {
        PanelScaffold(title = "Recent tabs", onClose = { panel = null }) {
            if (recentClosedList.isEmpty()) {
                PanelEmpty("No recently closed tabs. Tabs you close are kept " +
                    "here so you can reopen them.")
            } else {
                val timeFmt = remember { SimpleDateFormat("MMM d, HH:mm", Locale.US) }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(recentClosedList, key = { it.url + it.closedAt }) { entry ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    recentClosedList.remove(entry)
                                    persistSlotData()
                                    panel = null
                                    createTab(entry.url)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                entry.title.ifBlank { entry.url },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = orbitaOnBg()
                            )
                            Text(
                                "${displayUrl(entry.url)} · closed ${timeFmt.format(Date(entry.closedAt))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = orbitaText2(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------- browser settings

    private data class SettingRowData(
        val section: String,
        val title: String,
        val subtitle: String,
        val tag: String?,
        val onClick: () -> Unit
    )

    @Composable
    private fun BrowserSettingsPanel(p: Profile) {
        when (settingsRoute) {
            "engine" -> EngineSettingsScreen()
            "addressbar" -> AddressBarSettingsScreen()
            "privacy" -> PrivacySettingsScreen(p)
            "safety" -> SafetyCheckScreen(p)
            "appearance" -> AppearanceSettingsScreen()
            "accessibility" -> AccessibilitySettingsScreen()
            "sitesettings" -> SiteSettingsScreen()
            "tabs" -> TabsSettingsScreen()
            "homepage" -> HomepageSettingsScreen()
            "about" -> AboutBrowserScreen(p)
            else -> BrowserSettingsRoot(p)
        }
    }

    @Composable
    private fun SettingsSubScaffold(title: String, content: @Composable () -> Unit) {
        PanelScaffold(title = title, onClose = { settingsRoute = "root" }) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                content()
            }
        }
    }

    @Composable
    private fun BrowserSettingsRoot(p: Profile) {
        var query by remember { mutableStateOf("") }
        val onBg = orbitaOnBg()
        val text2 = orbitaText2()
        val session = remember { SessionStore(this@BrowserActivityBase).load() }
        val accountTitle = session?.email?.ifBlank { null } ?: "DevxyzLogin account"
        val accountSubtitle = if (session != null) {
            "Signed in — tap to manage in the app"
        } else {
            "Local mode — tap to open the app"
        }
        val rows = listOf(
            SettingRowData("You and DevxyzLogin", accountTitle, accountSubtitle, null) {
                openAppSection("overview")
            },
            SettingRowData("Basics", "Search engine", engine().name, null) {
                settingsRoute = "engine"
            },
            SettingRowData(
                "Basics", "Address bar",
                if (browserPrefs.addressBarPosition == "bottom") "Bottom" else "Top",
                "New"
            ) { settingsRoute = "addressbar" },
            SettingRowData("Basics", "Privacy and security", "Clear data · Cookies", null) {
                settingsRoute = "privacy"
            },
            SettingRowData("Basics", "Safety check", "Proxy, WebView, storage", null) {
                settingsRoute = "safety"
            },
            SettingRowData("Basics", "Default browser", "System default apps", null) {
                openDefaultAppsSettings()
            },
            SettingRowData(
                "Passwords and Autofill", "DevxyzLogin Vault",
                "Saved logins stored on this device", null
            ) { openAppSection("vault") },
            SettingRowData(
                "Passwords and Autofill", "API tokens",
                "Server API access tokens", null
            ) { openAppSection("api") },
            SettingRowData(
                "Passwords and Autofill", "Autofill & profile defaults",
                "Default profile settings in the main app", null
            ) { openAppSection("defaults") },
            SettingRowData(
                "Advanced", "Tabs and tab groups",
                "Restore tabs: ${if (browserPrefs.restoreTabs) "On" else "Off"}", null
            ) { settingsRoute = "tabs" },
            SettingRowData(
                "Advanced", "Homepage",
                if (browserPrefs.homepageEnabled) "On — New Tab page" else "Off — search engine home",
                null
            ) { settingsRoute = "homepage" },
            SettingRowData("Advanced", "Notifications", "System app notifications", null) {
                openNotificationSettings()
            },
            SettingRowData(
                "Advanced", "Appearance",
                appearanceMode.replaceFirstChar { it.uppercase() }, "New"
            ) { settingsRoute = "appearance" },
            SettingRowData(
                "Advanced", "Accessibility",
                "Text size ${browserPrefs.textZoom}%", null
            ) { settingsRoute = "accessibility" },
            SettingRowData(
                "Advanced", "Site settings",
                "JavaScript · Location", null
            ) { settingsRoute = "sitesettings" },
            SettingRowData(
                "Advanced", "Languages",
                currentLanguageLabel(), null
            ) { openLocaleSettings() },
            SettingRowData("Advanced", "Downloads", "Downloaded files", "New") {
                refreshDownloadStatuses()
                panel = BrowserPanel.DOWNLOADS
            },
            SettingRowData("Advanced", "About DevxyzLogin", "Version & engine", null) {
                settingsRoute = "about"
            }
        )
        val filtered = if (query.isBlank()) rows
        else rows.filter { it.title.contains(query, ignoreCase = true) }
        Surface(modifier = Modifier.fillMaxSize(), color = orbitaBg()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    IconButton(onClick = { panel = null }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = onBg)
                    }
                    Text(
                        "Settings",
                        style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                        color = onBg,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showHelp = true }) {
                        Icon(Icons.Filled.Help, contentDescription = "Help", tint = onBg)
                    }
                }
                // Search settings pill (really filters the rows)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(orbitaPill())
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Search, contentDescription = null,
                        tint = text2, modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = onBg, fontSize = 15.sp),
                        cursorBrush = SolidColor(BrandGreen),
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text("Search settings", color = text2, fontSize = 15.sp)
                            }
                            inner()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    var lastSection: String? = null
                    filtered.forEach { row ->
                        if (query.isBlank() && row.section != lastSection) {
                            item {
                                Text(
                                    row.section,
                                    fontSize = 14.sp,
                                    color = text2,
                                    modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)
                                )
                            }
                            lastSection = row.section
                        }
                        item(key = row.title) {
                            SettingsRowCard(row)
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }

    @Composable
    private fun SettingsRowCard(row: SettingRowData) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(orbitaRow())
                .clickable { row.onClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(row.title, fontSize = 16.sp, color = orbitaOnBg())
                if (row.tag != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(row.tag, fontSize = 13.sp, color = Color(0xFFD97A3D))
                }
            }
            if (row.subtitle.isNotBlank()) {
                Text(row.subtitle, fontSize = 13.sp, color = orbitaText2())
            }
        }
    }

    private fun currentLanguageLabel(): String {
        val code = try {
            getSharedPreferences("devxyz_prefs", Context.MODE_PRIVATE)
                .getString("ui_language", "en") ?: "en"
        } catch (e: Exception) {
            "en"
        }
        val name = Locale(code).getDisplayLanguage(Locale.ENGLISH)
        return name.ifBlank { code }
    }

    private fun openDefaultAppsSettings() {
        try {
            startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        } catch (e: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (e2: Exception) {
                Toast.makeText(this, "Couldn't open system settings", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openNotificationSettings() {
        try {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Couldn't open notification settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openLocaleSettings() {
        try {
            startActivity(Intent(Settings.ACTION_LOCALE_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, "Couldn't open language settings", Toast.LENGTH_SHORT).show()
        }
    }

    @Composable
    private fun RadioRow(
        label: String,
        subtitle: String,
        selected: Boolean,
        onSelect: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(orbitaRow())
                .clickable { onSelect() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onSelect)
            Column {
                Text(label, fontSize = 15.sp, color = orbitaOnBg())
                if (subtitle.isNotBlank()) {
                    Text(subtitle, fontSize = 12.sp, color = orbitaText2())
                }
            }
        }
    }

    @Composable
    private fun EngineSettingsScreen() {
        var selected by remember { mutableStateOf(browserPrefs.searchEngineId) }
        SettingsSubScaffold("Search engine") {
            Text(
                "Used by the address bar, the New Tab page and voice search.",
                color = orbitaText2(), fontSize = 13.sp
            )
            Spacer(Modifier.height(12.dp))
            SearchEngines.ALL.forEach { eng ->
                RadioRow(
                    label = eng.name,
                    subtitle = eng.homeUrl,
                    selected = selected == eng.id,
                    onSelect = {
                        selected = eng.id
                        browserPrefs.searchEngineId = eng.id
                    }
                )
                Spacer(Modifier.height(6.dp))
            }
        }
    }

    @Composable
    private fun AddressBarSettingsScreen() {
        var selected by remember { mutableStateOf(browserPrefs.addressBarPosition) }
        SettingsSubScaffold("Address bar") {
            RadioRow(
                "Top", "Address bar at the top of the screen",
                selected = selected == "top",
                onSelect = {
                    selected = "top"
                    browserPrefs.addressBarPosition = "top"
                }
            )
            Spacer(Modifier.height(6.dp))
            RadioRow(
                "Bottom", "Address bar at the bottom — easier one-handed",
                selected = selected == "bottom",
                onSelect = {
                    selected = "bottom"
                    browserPrefs.addressBarPosition = "bottom"
                }
            )
        }
    }

    @Composable
    private fun PrivacySettingsScreen(p: Profile) {
        var blockCookies by remember { mutableStateOf(browserPrefs.blockThirdPartyCookies) }
        SettingsSubScaffold("Privacy and security") {
            Text(
                "Browsing data for this profile slot",
                fontWeight = FontWeight.SemiBold, color = orbitaOnBg()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "History entries: ${historyList.size} · Cookies, cache and site " +
                    "data live in this slot's own WebView storage.",
                color = orbitaText2(), fontSize = 13.sp
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { showClearDataConfirm = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text("Delete browsing data") }
            Spacer(Modifier.height(18.dp))
            ToggleRow("Block third-party cookies", blockCookies) {
                blockCookies = it
                browserPrefs.blockThirdPartyCookies = it
                applyBrowserSettingsToAll()
            }
            Text(
                "Save history for this profile: " +
                    if (p.browser.saveHistory) "On (profile setting)" else "Off (profile setting)",
                color = orbitaText2(), fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }

    @Composable
    private fun SafetyCheckScreen(p: Profile) {
        var results by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
        var running by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            val out = mutableListOf<Pair<String, String>>()
            // WebView engine (real package info)
            val wvPkg = try {
                WebView.getCurrentWebViewPackage()
            } catch (e: Exception) {
                null
            }
            out.add(
                "WebView engine" to
                    (wvPkg?.let { "${it.packageName} · version ${it.versionName}" }
                        ?: "Version unavailable")
            )
            // Proxy reachability — a real TCP connect from this device.
            val proxy = p.proxy.sanitized()
            if (!proxy.isSet) {
                out.add("Proxy" to "No proxy set — pages load on a direct connection")
            } else {
                val (ok, ms) = withContext(Dispatchers.IO) {
                    val started = System.nanoTime()
                    try {
                        Socket().use { socket ->
                            socket.connect(InetSocketAddress(proxy.host, proxy.port), 3000)
                        }
                        true to ((System.nanoTime() - started) / 1_000_000)
                    } catch (e: Exception) {
                        false to -1L
                    }
                }
                out.add(
                    "Proxy ${proxy.listLabel}" to
                        if (ok) "Reachable — answered in ${ms} ms" else "Not reachable from this device"
                )
            }
            // Free storage for browser data
            val free = try {
                android.os.StatFs(filesDir.path).availableBytes
            } catch (e: Exception) {
                0L
            }
            out.add("Storage" to "${formatBytes(free)} free for browser data")
            out.add(
                "Third-party cookies" to
                    if (browserPrefs.blockThirdPartyCookies) "Blocked" else "Allowed"
            )
            out.add(
                "Session" to "Running for ${formatElapsed(elapsedSec)} · slot $slot"
            )
            results = out
            running = false
        }
        SettingsSubScaffold("Safety check") {
            if (running) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = BrandGreen, modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Running checks…", color = orbitaOnBg())
                }
            } else {
                results.forEach { (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Check, contentDescription = null,
                            tint = BrandGreen, modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(label, fontWeight = FontWeight.SemiBold, color = orbitaOnBg())
                            Text(value, fontSize = 13.sp, color = orbitaText2())
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TabsSettingsScreen() {
        var restore by remember { mutableStateOf(browserPrefs.restoreTabs) }
        SettingsSubScaffold("Tabs and tab groups") {
            ToggleRow("Restore open tabs on launch", restore) {
                restore = it
                browserPrefs.restoreTabs = it
                persistSlotData()
            }
            Text(
                "Group the current tab from the ⋮ menu → \"Add tab to new group\". " +
                    "Groups appear as headers in the tab switcher. Private tabs are " +
                    "never restored.",
                color = orbitaText2(), fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }

    @Composable
    private fun HomepageSettingsScreen() {
        var enabled by remember { mutableStateOf(browserPrefs.homepageEnabled) }
        SettingsSubScaffold("Homepage") {
            ToggleRow("Homepage", enabled) {
                enabled = it
                browserPrefs.homepageEnabled = it
            }
            Text(
                if (enabled) {
                    "New tabs open the New Tab page (search, shortcuts, tips)."
                } else {
                    "New tabs open your search engine's home page instead."
                },
                color = orbitaText2(), fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }

    @Composable
    private fun AppearanceSettingsScreen() {
        var selected by remember { mutableStateOf(appearanceMode) }
        SettingsSubScaffold("Appearance") {
            listOf(
                "system" to "System default",
                "light" to "Light",
                "dark" to "Dark"
            ).forEach { (value, label) ->
                RadioRow(
                    label = label,
                    subtitle = "",
                    selected = selected == value,
                    onSelect = {
                        selected = value
                        appearanceMode = value
                        browserPrefs.appearance = value
                    }
                )
                Spacer(Modifier.height(6.dp))
            }
            Text(
                "Applies to the browser chrome (toolbars, New Tab page, menus, settings).",
                color = orbitaText2(), fontSize = 13.sp
            )
        }
    }

    @Composable
    private fun AccessibilitySettingsScreen() {
        var zoom by remember { mutableStateOf(browserPrefs.textZoom.toFloat()) }
        SettingsSubScaffold("Accessibility") {
            Text(
                "Text size: ${zoom.toInt()}%",
                fontWeight = FontWeight.SemiBold, color = orbitaOnBg()
            )
            Spacer(Modifier.height(8.dp))
            Slider(
                value = zoom,
                onValueChange = {
                    zoom = it
                    browserPrefs.textZoom = it.toInt()
                    tabs.forEach { tab -> tab.webView?.settings?.textZoom = it.toInt() }
                },
                valueRange = 50f..200f,
                steps = 14
            )
            Text(
                "Scales text on web pages in every open tab (WebView text zoom).",
                color = orbitaText2(), fontSize = 13.sp
            )
        }
    }

    @Composable
    private fun SiteSettingsScreen() {
        var js by remember { mutableStateOf(browserPrefs.javaScriptEnabled) }
        var loc by remember { mutableStateOf(browserPrefs.locationEnabled) }
        SettingsSubScaffold("Site settings") {
            ToggleRow("JavaScript", js) {
                js = it
                browserPrefs.javaScriptEnabled = it
                applyBrowserSettingsToAll()
            }
            Text(
                "Turning JavaScript off also disables this profile's fingerprint " +
                    "injection on pages loaded afterwards.",
                color = orbitaText2(), fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(8.dp))
            ToggleRow("Location access", loc) {
                loc = it
                browserPrefs.locationEnabled = it
                applyBrowserSettingsToAll()
            }
            Text(
                "When on, websites may request your location through the WebView " +
                    "geolocation prompt; when off, requests are denied.",
                color = orbitaText2(), fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }

    @Composable
    private fun AboutBrowserScreen(p: Profile) {
        val (versionName, versionCode) = remember {
            try {
                val info = packageManager.getPackageInfo(packageName, 0)
                @Suppress("DEPRECATION")
                (info.versionName ?: "—") to
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode
                    else info.versionCode.toLong()
            } catch (e: Exception) {
                "—" to 0L
            }
        }
        val wvVersion = remember {
            try {
                WebView.getCurrentWebViewPackage()?.versionName ?: "Unknown"
            } catch (e: Exception) {
                "Unknown"
            }
        }
        SettingsSubScaffold("About DevxyzLogin") {
            InfoRow("App", "DevxyzLogin $versionName (build $versionCode)")
            InfoRow("Browser engine", "Android WebView $wvVersion")
            InfoRow("Profile", p.name)
            InfoRow("Storage slot", "slot_$slot (own WebView data directory)")
            InfoRow("Search engine", engine().name)
        }
    }

    /**
     * API-control console — a faithful local echo of the captured GoLogin
     * cloud-browser console (capture §0): session line, masked token with
     * Copy, the default Puppeteer script, and timestamped log lines. It is
     * honest about being on-device: real Puppeteer sessions come from the
     * DevxyzLogin server API.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ConsoleSheet(p: Profile, onClose: () -> Unit) {
        val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
        val token = remember(p.id) { sessionToken(p.id) }
        val masked = remember(token) {
            token.take(12) + "••••••••••••" + token.takeLast(4)
        }
        val now = remember {
            SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        }
        val script = "await page.goto('https://example.com');\n" +
            "await page.waitForSelector('body');\n\n" +
            "const title = await page.evaluate(() => document.title);\n" +
            "console.log(title);"
        AppBottomSheet(onDismiss = onClose) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                SheetTitle("API control")
                Spacer(Modifier.height(4.dp))
                Text(
                    "🌐 ${p.proxy.listLabel} · ${p.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        masked,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(onClick = {
                        clipboard.setText(androidx.compose.ui.text.AnnotatedString(token))
                        Toast.makeText(this@BrowserActivityBase, "Token copied", Toast.LENGTH_SHORT).show()
                    }) { Text("Copy token") }
                }
                Spacer(Modifier.height(6.dp))
                Text("Script", fontWeight = FontWeight.Bold, color = BrandGreen)
                Text(
                    "Uses the Puppeteer page API",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        script,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text("Logs", fontWeight = FontWeight.Bold, color = BrandGreen)
                val shortId = p.id.replace("-", "").take(24)
                listOf(
                    "$now [session] connecting profileId=$shortId",
                    "$now [session] local session started — profile browser running on this device",
                    "$now [info] Remote Puppeteer sessions run through the DevxyzLogin server API; this console is the on-device echo."
                ).forEach { line ->
                    Text(
                        line,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                // Capture §0: the console carries a "Cloud Browser docs" link.
                TextButton(onClick = {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://gologin.com/docs")))
                }) { Text("Cloud Browser docs") }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    private fun sessionToken(profileId: String): String {
        fun b64(s: String): String =
            Base64.getUrlEncoder().withoutPadding()
                .encodeToString(s.toByteArray(Charsets.UTF_8))
        val header = b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}")
        val payload = b64("{\"profileId\":\"$profileId\",\"scope\":\"local-session\"}")
        val sig = b64(profileId.hashCode().toString() + profileId.reversed())
        return "$header.$payload.$sig"
    }

    companion object {
        const val EXTRA_PROFILE_ID = "profile_id"
        const val EXTRA_SLOT = "slot"
        const val EXTRA_API_MODE = "api_mode"
        const val ACTION_STOP_SLOT = "com.devxyz.login.STOP_SLOT"
        const val START_URL = "https://www.google.com"
        const val WEB_DASHBOARD_URL = "https://devxyzlogin.vercel.app"
        const val DESKTOP_UA =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/156.0.8078.47 Safari/537.36"

        fun intentFor(context: Context, slot: Int, profileId: String, apiMode: Boolean = false): Intent {
            val cls = when (slot) {
                1 -> BrowserActivityP1::class.java
                2 -> BrowserActivityP2::class.java
                else -> BrowserActivityP3::class.java
            }
            return Intent(context, cls).apply {
                putExtra(EXTRA_PROFILE_ID, profileId)
                putExtra(EXTRA_SLOT, slot)
                putExtra(EXTRA_API_MODE, apiMode)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        fun stopSlot(context: Context, slot: Int) {
            // Ending the run also ends its session-timer timestamp.
            try {
                BrowserPrefs(context).clearSessionStart(slot)
            } catch (e: Exception) { /* best effort */ }
            val intent = Intent(ACTION_STOP_SLOT).apply {
                setPackage(context.packageName)
                putExtra(EXTRA_SLOT, slot)
            }
            context.sendBroadcast(intent)
        }
    }
}

/** Browser slot processes — see AndroidManifest (:p1 / :p2 / :p3). */
class BrowserActivityP1 : BrowserActivityBase(1)
class BrowserActivityP2 : BrowserActivityBase(2)
class BrowserActivityP3 : BrowserActivityBase(3)
