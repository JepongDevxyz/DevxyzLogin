package com.devxyz.login.browser

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import com.devxyz.login.data.ProfileStore
import com.devxyz.login.model.Profile
import java.util.concurrent.Executors

/**
 * Browser window for ONE profile slot.
 *
 * ISOLATION MODEL (honest, see SHARED_SCHEMA.md):
 * - This activity runs in a dedicated process (:p1 / :p2 / :p3, declared in
 *   the manifest via the subclasses below). Before ANY WebView is created in
 *   that process we call WebView.setDataDirectorySuffix("slot_N"), which
 *   gives the slot its own cookies/storage directory.
 * - Each profile is permanently assigned a slot by ProfileStore.slotFor(),
 *   so a profile reuses the same storage across runs. Profiles sharing a
 *   slot SHARE storage — that is the real WebView limit (one data dir per
 *   process), documented rather than hidden.
 * - The main (list) process never creates a WebView, so the suffix rule is
 *   never violated there.
 */
abstract class BrowserActivityBase(private val slot: Int) : Activity() {

    private var webView: WebView? = null
    private var profile: Profile? = null

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

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val header = TextView(this).apply {
            text = "DevxyzLogin • ${loaded.name} (slot $slot)"
            setPadding(24, 16, 24, 16)
            textSize = 14f
        }
        root.addView(header)

        val wv = WebView(this)
        root.addView(wv, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        webView = wv
        setContentView(root)

        configureWebView(wv, loaded)
        applyProxy(loaded) {
            wv.loadUrl(START_URL)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView(wv: WebView, profile: Profile) {
        val script = FingerprintScript.build(profile)
        with(wv.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            // Custom user agent straight from the profile fingerprint.
            userAgentString = profile.fingerprint.navigator.userAgent
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = false
            }
        }
        wv.webChromeClient = WebChromeClient()
        wv.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                // Inject as early as WebView lets us (document start is not
                // available; there is a small race with page scripts — a
                // known WebView limit vs. a real Chromium fork).
                view?.evaluateJavascript(script, null)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
        }
    }

    /**
     * Per-profile proxy via androidx.webkit ProxyController.
     * Limits: HTTP/HTTPS proxy rules only — SOCKS and proxy AUTH are not
     * supported by ProxyController; we say so instead of failing silently.
     */
    private fun applyProxy(profile: Profile, done: () -> Unit) {
        val proxy = profile.proxy
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

    override fun onBackPressed() {
        val wv = webView
        if (wv != null && wv.canGoBack()) wv.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        try { unregisterReceiver(stopReceiver) } catch (e: Exception) { /* already gone */ }
        webView?.destroy()
        webView = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_PROFILE_ID = "profile_id"
        const val EXTRA_SLOT = "slot"
        const val ACTION_STOP_SLOT = "com.devxyz.login.STOP_SLOT"
        const val START_URL = "https://www.google.com"

        fun intentFor(context: Context, slot: Int, profileId: String): Intent {
            val cls = when (slot) {
                1 -> BrowserActivityP1::class.java
                2 -> BrowserActivityP2::class.java
                else -> BrowserActivityP3::class.java
            }
            return Intent(context, cls).apply {
                putExtra(EXTRA_PROFILE_ID, profileId)
                putExtra(EXTRA_SLOT, slot)
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
