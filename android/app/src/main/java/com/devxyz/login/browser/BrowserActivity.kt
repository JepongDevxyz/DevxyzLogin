package com.devxyz.login.browser

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import com.devxyz.login.data.ProfileStore
import com.devxyz.login.model.DefaultBookmarks
import com.devxyz.login.model.Profile
import com.devxyz.login.ui.BrandGreen
import com.devxyz.login.ui.BrandGreenLight
import com.devxyz.login.ui.DangerRed
import com.devxyz.login.ui.DevxyzTheme
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/** Internal URL of the Chrome-style New Tab page. */
const val NTP_URL = "devxyz://newtab"

/** One real browser tab: its own WebView inside the profile's process/data-dir. */
class BrowserTab(val id: Int) {
    var webView: WebView? = null
    var url by mutableStateOf(NTP_URL)
    var title by mutableStateOf("New Tab")
    var desktop by mutableStateOf(false)
    var canBack by mutableStateOf(false)
    var canForward by mutableStateOf(false)
}

data class HistoryEntry(val title: String, val url: String)

/**
 * Chrome-for-Android-style browser window for ONE profile slot.
 *
 * GoLogin's Orbita is a Chromium fork, so its window IS Chrome's UI; v2
 * mirrors that here: omnibox toolbar (lock icon + editable URL/search),
 * tab-count button → tab-switcher grid (multiple real tabs per profile,
 * each its own WebView in the same profile process/data directory),
 * Chrome-style ⋮ menu, New Tab page with bookmark shortcut tiles, find in
 * page, desktop-site toggle, history/bookmarks panels.
 *
 * ISOLATION MODEL (unchanged from v1, see SHARED_SCHEMA.md): the activity
 * runs in a dedicated process (:p1/:p2/:p3); before ANY WebView exists in
 * that process we call WebView.setDataDirectorySuffix("slot_N"). Profiles
 * are permanently assigned a slot; profiles sharing a slot share storage —
 * the real WebView limit, documented rather than hidden.
 */
abstract class BrowserActivityBase(private val slot: Int) : ComponentActivity() {

    private var profile: Profile? = null
    private var apiMode: Boolean = false

    private val tabs = mutableStateListOf<BrowserTab>()
    private var activeTabId by mutableStateOf(0)
    private var loadProgress by mutableStateOf(0)
    private var proxyReady by mutableStateOf(false)
    private val historyEntries = mutableStateListOf<HistoryEntry>()
    private var containerRef: FrameLayout? = null
    private var nextTabId = 1

    // Transient UI state
    private var showSwitcher by mutableStateOf(false)
    private var menuOpen by mutableStateOf(false)
    private var findOpen by mutableStateOf(false)
    private var findQuery by mutableStateOf("")
    private var findResult by mutableStateOf("")
    private var showHistory by mutableStateOf(false)
    private var showBookmarks by mutableStateOf(false)
    private var showConsole by mutableStateOf(false)
    private var stubDialog by mutableStateOf<String?>(null)
    /** v2.3.1: the active tab's main-frame load died on a proxy error
     *  (ERR_PROXY_*) — BrowserContent shows our own error state for it. */
    private var proxyError by mutableStateOf(false)

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.getIntExtra(EXTRA_SLOT, -1) == slot) finish()
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

        ContextCompat.registerReceiver(
            this, stopReceiver, IntentFilter(ACTION_STOP_SLOT),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        setContent {
            DevxyzTheme {
                BrowserContent()
            }
        }

        applyProxy(loaded) {
            proxyReady = true
            if (tabs.isEmpty()) {
                val start = when {
                    loaded.browser.startUrl == "specified" &&
                        loaded.browser.startPages.isNotEmpty() -> loaded.browser.startPages.first()
                    else -> NTP_URL
                }
                createTab(start)
            }
            if (apiMode) showConsole = true
        }
    }

    // ------------------------------------------------------------------ tabs

    private fun activeTab(): BrowserTab? =
        tabs.firstOrNull { it.id == activeTabId } ?: tabs.firstOrNull()

    private fun createTab(initialUrl: String): BrowserTab {
        val tab = BrowserTab(nextTabId++)
        val wv = WebView(this)
        tab.webView = wv
        configureWebView(wv, tab)
        tab.url = initialUrl
        tabs.add(tab)
        activeTabId = tab.id
        if (initialUrl != NTP_URL) wv.loadUrl(initialUrl)
        syncContainer()
        return tab
    }

    private fun closeTab(tab: BrowserTab) {
        val wasActive = tab.id == activeTabId
        tab.webView?.let { wv ->
            (wv.parent as? ViewGroup)?.removeView(wv)
            wv.destroy()
        }
        tabs.remove(tab)
        if (tabs.isEmpty()) {
            createTab(NTP_URL)
        } else if (wasActive) {
            activeTabId = tabs.last().id
        }
        syncContainer()
    }

    private fun switchTab(tab: BrowserTab) {
        activeTabId = tab.id
        loadProgress = 0
        syncContainer()
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
            javaScriptEnabled = true
            domStorageEnabled = p.browser.localStorage
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            loadsImagesAutomatically = !p.browser.disableImageLoading
            userAgentString = p.fingerprint.navigator.userAgent
            useWideViewPort = true
            loadWithOverviewMode = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = false
            }
        }
        wv.setFindListener { _, numberOfMatches, isDoneCounting ->
            if (isDoneCounting && tab.id == activeTabId) {
                findResult = if (findQuery.isBlank()) "" else "$numberOfMatches matches"
            }
        }
        wv.setDownloadListener { _, _, _, _, _ ->
            Toast.makeText(
                this,
                "Downloads are not supported in the profile browser",
                Toast.LENGTH_SHORT
            ).show()
        }
        wv.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (tab.id == activeTabId) loadProgress = newProgress
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (!title.isNullOrBlank()) tab.title = title
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
                    if (url != NTP_URL && historyEntries.firstOrNull()?.url != url) {
                        historyEntries.add(0, HistoryEntry(t, url))
                    }
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
                    val desc = error?.description?.toString().orEmpty()
                    if (error?.errorCode == WebViewClient.ERROR_PROXY_CONNECTION ||
                        desc.contains("ERR_PROXY")
                    ) {
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
        return "https://www.google.com/search?q=" + URLEncoder.encode(q, "UTF-8")
    }

    private fun navigateActive(raw: String) {
        // A fresh manual navigation dismisses a stale proxy-error state;
        // if the proxy is still dead it will simply be raised again.
        proxyError = false
        val tab = activeTab() ?: return
        val url = resolveInput(raw)
        if (url == NTP_URL) {
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
        finish()
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

    override fun onDestroy() {
        try { unregisterReceiver(stopReceiver) } catch (e: Exception) { /* already gone */ }
        tabs.forEach { it.webView?.destroy() }
        tabs.clear()
        super.onDestroy()
    }

    // -------------------------------------------------------------------- UI

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun BrowserContent() {
        val p = profile ?: return
        val tab = activeTab()

        BackHandler(enabled = findOpen || showSwitcher) {
            if (findOpen) {
                findOpen = false
                tab?.webView?.clearMatches()
            } else {
                showSwitcher = false
            }
        }
        BackHandler(enabled = !findOpen && !showSwitcher && tab?.canBack == true) {
            tab?.webView?.goBack()
        }

        LaunchedEffect(activeTabId, tabs.size) { syncContainer() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ---- Chrome-style toolbar: lock/globe · omnibox · tab count · ⋮
            Surface(shadowElevation = 3.dp, color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val url = tab?.url ?: NTP_URL
                    Icon(
                        imageVector = when {
                            url == NTP_URL -> Icons.Filled.Search
                            url.startsWith("https://") -> Icons.Filled.Lock
                            else -> Icons.Filled.Public
                        },
                        contentDescription = null,
                        tint = if (url.startsWith("https://")) BrandGreen
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(18.dp)
                    )
                    Omnibox(
                        tab = tab,
                        modifier = Modifier.weight(1f),
                        onSubmit = { navigateActive(it) }
                    )
                    Spacer(Modifier.width(6.dp))
                    // Tab-count square (Chrome style)
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .border(
                                2.dp,
                                MaterialTheme.colorScheme.onSurfaceVariant,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { showSwitcher = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            tabs.size.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Browser menu")
                        }
                        ChromeMenu(tab)
                    }
                }
            }
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
                    NewTabPage(p) { navigateActive(it) }
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
                            .background(MaterialTheme.colorScheme.background)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Proxy connection failed",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "This profile's proxy (${p.proxy.listLabel}) could not be " +
                                "reached, so pages can't load. Check the proxy in the " +
                                "profile settings, or open this session without one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        }

        if (showSwitcher) TabSwitcher()
        if (showHistory) HistorySheet { showHistory = false }
        if (showBookmarks) BookmarksSheet(p) { showBookmarks = false }
        if (showConsole) ConsoleSheet(p) { showConsole = false }
        stubDialog?.let { message ->
            AlertDialog(
                onDismissRequest = { stubDialog = null },
                title = { Text(message.substringBefore(" — ")) },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { stubDialog = null }) { Text("OK") }
                }
            )
        }
    }

    @Composable
    private fun Omnibox(tab: BrowserTab?, modifier: Modifier = Modifier, onSubmit: (String) -> Unit) {
        var editing by remember { mutableStateOf(false) }
        var text by remember { mutableStateOf("") }
        val current = tab?.url ?: NTP_URL
        val shown = if (editing) text else displayUrl(current)
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            BasicTextField(
                value = shown,
                onValueChange = {
                    text = it
                    editing = true
                },
                singleLine = true,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
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

    @Composable
    private fun ChromeMenu(tab: BrowserTab?) {
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("New tab") },
                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    createTab(NTP_URL)
                }
            )
            DropdownMenuItem(
                text = { Text("History") },
                leadingIcon = { Icon(Icons.Filled.History, contentDescription = null) },
                onClick = { menuOpen = false; showHistory = true }
            )
            DropdownMenuItem(
                text = { Text("Bookmarks") },
                leadingIcon = { Icon(Icons.Filled.Bookmark, contentDescription = null) },
                onClick = { menuOpen = false; showBookmarks = true }
            )
            DropdownMenuItem(
                text = { Text("Downloads") },
                leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    stubDialog = "Downloads — Downloads are not stored by the profile browser (WebView limitation)."
                }
            )
            DropdownMenuItem(
                text = { Text("Share page") },
                leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    val url = tab?.url
                    if (url != null && url != NTP_URL) {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, url)
                        }
                        startActivity(Intent.createChooser(send, "Share page"))
                    }
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Desktop site") },
                leadingIcon = { Icon(Icons.Filled.DesktopWindows, contentDescription = null) },
                trailingIcon = {
                    Checkbox(checked = tab?.desktop == true, onCheckedChange = null)
                },
                onClick = {
                    menuOpen = false
                    if (tab != null) setDesktop(tab, !tab.desktop)
                }
            )
            DropdownMenuItem(
                text = { Text("Reload") },
                leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    if (tab?.url != NTP_URL) tab?.webView?.reload()
                }
            )
            DropdownMenuItem(
                text = { Text("Forward") },
                leadingIcon = { Icon(Icons.Filled.ArrowForward, contentDescription = null) },
                enabled = tab?.canForward == true,
                onClick = {
                    menuOpen = false
                    tab?.webView?.goForward()
                }
            )
            DropdownMenuItem(
                text = { Text("Find in page") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    findQuery = ""
                    findResult = ""
                    findOpen = true
                }
            )
            DropdownMenuItem(
                text = { Text("Settings") },
                leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    stubDialog = "Settings — Fingerprint, proxy and storage for this profile are set in the profile editor (profile list → Settings)."
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Stop profile", color = DangerRed) },
                leadingIcon = {
                    Icon(Icons.Filled.StopCircle, contentDescription = null, tint = DangerRed)
                },
                onClick = {
                    menuOpen = false
                    stopProfile()
                }
            )
        }
    }

    @Composable
    private fun FindBar(tab: BrowserTab?) {
        Surface(shadowElevation = 6.dp, color = MaterialTheme.colorScheme.surface) {
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

    @Composable
    private fun NewTabPage(p: Profile, onOpen: (String) -> Unit) {
        var query by remember { mutableStateOf("") }
        val links = remember(p.bookmarks) { DefaultBookmarks.links(p.bookmarks) }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(56.dp))
            Text(
                "DevxyzLogin",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(
                    brush = Brush.linearGradient(listOf(BrandGreenLight, BrandGreen))
                )
            )
            Text(
                p.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 18.dp, vertical = 13.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Search, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        ),
                        cursorBrush = SolidColor(BrandGreen),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onOpen(query) }),
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text(
                                    "Search or type URL",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 16.sp
                                )
                            }
                            inner()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            if (links.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(((links.take(12).size + 3) / 4 * 92).dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    userScrollEnabled = false
                ) {
                    items(links.take(12)) { (_, name, url) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onOpen(url) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    name.firstOrNull()?.uppercase() ?: "?",
                                    fontWeight = FontWeight.Bold,
                                    color = BrandGreen
                                )
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(
                                name,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                Text(
                    "No bookmark shortcuts yet — add bookmarks in the profile editor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    @Composable
    private fun TabSwitcher() {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Tabs",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { createTab(NTP_URL) }) {
                        Icon(Icons.Filled.Add, contentDescription = "New tab")
                    }
                    IconButton(onClick = { showSwitcher = false }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close tab switcher")
                    }
                }
                Spacer(Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(tabs, key = { it.id }) { tab ->
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
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            tab.title.firstOrNull()?.uppercase() ?: "N",
                                            fontWeight = FontWeight.Bold,
                                            color = BrandGreen
                                        )
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
                                    if (tab.url == NTP_URL) "New Tab" else displayUrl(tab.url),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun HistorySheet(onClose: () -> Unit) {
        ModalBottomSheet(onDismissRequest = onClose) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("History", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                if (historyEntries.isEmpty()) {
                    Text(
                        "No history yet in this session.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
                LazyColumn(modifier = Modifier.height(360.dp)) {
                    items(historyEntries) { entry ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onClose()
                                    navigateActive(entry.url)
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(entry.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                entry.url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun BookmarksSheet(p: Profile, onClose: () -> Unit) {
        val links = remember(p.bookmarks) { DefaultBookmarks.links(p.bookmarks) }
        ModalBottomSheet(onDismissRequest = onClose) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Bookmarks", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                if (links.isEmpty()) {
                    Text(
                        "No bookmarks with URLs in this profile.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
                LazyColumn(modifier = Modifier.height(360.dp)) {
                    items(links) { (folder, name, url) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onClose()
                                    navigateActive(url)
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                if (folder.isNotBlank()) "$folder · $name" else name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
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
        val clipboard = LocalClipboardManager.current
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
        ModalBottomSheet(onDismissRequest = onClose) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("API control", style = MaterialTheme.typography.titleMedium)
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
                        clipboard.setText(AnnotatedString(token))
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
