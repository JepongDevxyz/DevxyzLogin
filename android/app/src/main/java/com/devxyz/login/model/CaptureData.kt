package com.devxyz.login.model

/**
 * Option lists and defaults transcribed from the exhaustive GoLogin web-app
 * capture (2026-11-10/11, see GOLOGIN_CAPTURE.md). These are the exact
 * values the GoLogin editor/settings show — do not invent others.
 */
object CaptureData {

    /** Orbita/Browser version picker: "Chrome 156" stable, then 154 down to 111 (155 is absent in GoLogin). */
    val browserVersionMajors: List<String> =
        listOf("156") + (154 downTo 111).map { it.toString() }

    fun browserVersionLabel(major: String): String = "Chrome $major"

    /** Advanced → Screen Resolution (capture list; a few mid entries were virtualized away upstream). */
    val screenResolutions: List<String> = listOf(
        "800x600", "960x720", "1024x576", "1024x600", "1024x640", "1024x768",
        "1152x648", "1152x864", "1280x720", "1280x768",
        "1512x982", "1536x864", "1600x900", "1600x1200", "1680x1050",
        "1710x1107", "1728x1117", "1856x1392", "1920x1080", "1920x1200",
        "1920x1440", "2048x1152", "2048x1536", "2304x1440", "2560x1080",
        "2560x1440", "2560x1600", "2560x1664", "2560x2048", "2880x1800",
        "3440x1440"
    )

    /** Advanced → CPU Threads. */
    val cpuOptions: List<Int> = listOf(2, 4, 6, 8, 10, 12, 16, 20, 24, 32)

    /** Advanced → RAM (GB). */
    val ramOptions: List<Int> = listOf(1, 2, 4, 6, 8, 16, 32)

    /** Advanced → WebGL Vendor (14, exact from capture). */
    val webglVendors: List<String> = listOf(
        "ARM",
        "Apple Inc.",
        "Brave",
        "Google Inc. (AMD)",
        "Google Inc. (ARM)",
        "Google Inc. (Apple)",
        "Google Inc. (Intel Inc.)",
        "Google Inc. (Intel)",
        "Google Inc. (Microsoft)",
        "Google Inc. (NVIDIA Corporation)",
        "Google Inc. (NVIDIA)",
        "Google Inc. (Qualcomm)",
        "Intel Inc.",
        "Qualcomm"
    )

    private val adrenoRenderers: List<String> =
        listOf(610, 618, 619, 650, 710, 730, 735, 750, 810, 829, 830).map {
            "ANGLE (Qualcomm, Adreno (TM) $it, OpenGL ES 3.2)"
        }

    /** WebGL Renderer list is vendor-dependent in GoLogin; Qualcomm is capture-exact,
     *  other families follow the same ANGLE naming pattern. */
    fun webglRenderersFor(vendor: String): List<String> = when (vendor) {
        "Google Inc. (Qualcomm)", "Qualcomm" -> adrenoRenderers
        "Google Inc. (ARM)", "ARM" -> listOf(
            "ANGLE (ARM, Mali-G615 MC2, OpenGL ES 3.2)",
            "ANGLE (ARM, Mali-G710 MC10, OpenGL ES 3.2)",
            "ANGLE (ARM, Mali-G78, OpenGL ES 3.2)"
        )
        "Google Inc. (Apple)", "Apple Inc." -> listOf(
            "ANGLE (Apple, ANGLE Metal Renderer: Apple M1, Unspecified Version)",
            "ANGLE (Apple, ANGLE Metal Renderer: Apple M2, Unspecified Version)",
            "ANGLE (Apple, ANGLE Metal Renderer: Intel Iris Plus Graphics, Unspecified Version)"
        )
        "Google Inc. (Intel)", "Google Inc. (Intel Inc.)", "Intel Inc." -> listOf(
            "ANGLE (Intel, Intel(R) UHD Graphics 630 Direct3D11 vs_5_0 ps_5_0, D3D11)",
            "ANGLE (Intel, Intel(R) Iris(R) Xe Graphics Direct3D11 vs_5_0 ps_5_0, D3D11)",
            "ANGLE (Intel, Intel(R) HD Graphics 520 Direct3D11 vs_5_0 ps_5_0, D3D11)"
        )
        "Google Inc. (NVIDIA)", "Google Inc. (NVIDIA Corporation)" -> listOf(
            "ANGLE (NVIDIA, NVIDIA GeForce RTX 3060 Direct3D11 vs_5_0 ps_5_0, D3D11)",
            "ANGLE (NVIDIA, NVIDIA GeForce GTX 1660 SUPER Direct3D11 vs_5_0 ps_5_0, D3D11)"
        )
        "Google Inc. (AMD)" -> listOf(
            "ANGLE (AMD, AMD Radeon RX 6600 Direct3D11 vs_5_0 ps_5_0, D3D11)",
            "ANGLE (AMD, AMD Radeon(TM) Graphics Direct3D11 vs_5_0 ps_5_0, D3D11)"
        )
        "Google Inc. (Microsoft)" -> listOf(
            "ANGLE (Microsoft, Microsoft Basic Render Driver Direct3D11 vs_5_0 ps_5_0, D3D11)"
        )
        "Brave" -> listOf(
            "ANGLE (Intel, Intel(R) UHD Graphics 630 Direct3D11 vs_5_0 ps_5_0, D3D11)"
        )
        else -> emptyList()
    }

    /** Start URL options (exactly 3 in GoLogin): value -> label. */
    val startUrlOptions: List<Pair<String, String>> = listOf(
        "newTab" to "Open new tab page",
        "previouslyOpened" to "Previously opened tabs",
        "specified" to "Specified pages"
    )

    /** Proxy tab "Your proxy" type combobox (exactly 3 in GoLogin): value -> label. */
    val proxyTypeOptions: List<Pair<String, String>> = listOf(
        "http" to "HTTP Proxy",
        "socks4" to "Socks 4 Proxy",
        "socks5" to "Socks 5 Proxy"
    )

    /** Overview language picker (Select language in Settings → Overview). */
    val uiLanguages: List<Pair<String, String>> = listOf(
        "en" to "English",
        "ru-RU" to "Русский",
        "fr" to "Français",
        "pt" to "Português",
        "vi" to "Tiếng việt",
        "zh" to "中文",
        "ja" to "日本"
    )

    /** Built-in proxy countries shown first in GoLogin's combobox (flag, name). */
    val proxyCountries: List<Pair<String, String>> = listOf(
        "FR" to "France",
        "DE" to "Germany",
        "GB" to "United Kingdom",
        "US" to "United States",
        "AU" to "Australia"
    )

    /** Full build of a UA string for a Chrome major version + OS family (capture UA patterns). */
    fun uaFor(os: String, major: String, fullVersion: String): String = when (os) {
        "windows10", "windows11" ->
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$fullVersion Safari/537.36"
        "macIntel", "macArm" ->
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$fullVersion Safari/537.36"
        "linux" ->
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$fullVersion Safari/537.36"
        else ->
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$fullVersion Mobile Safari/537.36"
    }
}

/** Default bookmark lists. GoLogin ships TWO variants — the profile EDITOR
 *  defaults and the (different) Default-profile-settings defaults. */
object DefaultBookmarks {

    /** New-profile editor variant (capture §4.5, incl. GoLogin's own "Dropshipphing" typo line). */
    val editorLines: List<String> = listOf(
        "Facebook::Facebook::https://facebook.com",
        "Facebook::Google Ads::https://ads.google.com",
        "Facebook::Google Drive::https://drive.google.com",
        "Facebook::Instagram::https://instagram.com",
        "Facebook::ChatGPT::https://chatgpt.com",
        "Facebook::Pinterest::https://www.pinterest.com",
        "Dropshipping::",
        "Dropshipping::Amazon::https://www.amazon.com/",
        "Dropshipphing::eBay::https://www.ebay.com/",
        "Dropshipping::Walmart::https://www.walmart.com/",
        "Dropshipping::Shopify::https://www.shopify.com/",
        "Dropshipping::Taobao::https://www.taobao.com/",
        "Dropshipping::Aliexpress::https://www.aliexpress.com/",
        "Dropshipping::PayPal::https://www.paypal.com/",
        "Dropshipping::Payoneer::https://www.payoneer.com/",
        "Crypto::Coinlist::https://coinlist.co",
        "Crypto::Huobi::https://www.htx.com",
        "Crypto::Discord::https://discord.com",
        "Crypto::Premint::https://www.premint.xyz",
        "Facebook",
        "Amazon",
        "TikTok",
        "Google Ads",
        "Browser check",
        "Browser check::https://iphey.com/"
    )

    val editorText: String get() = editorLines.joinToString("\n")

    /** Default-profile-settings variant (capture §4.5 note / §12.2):
     *  Facebook block uses facebook.com product URLs; dropshipping URLs have no "www."; Huobi = huobi.com. */
    val settingsLines: List<String> = listOf(
        "Facebook::Facebook::https://www.facebook.com/",
        "Facebook::Notifications::https://www.facebook.com/notifications/",
        "Facebook::Account Quality::https://www.facebook.com/accountquality/",
        "Facebook::Ads Manager::https://www.facebook.com/adsmanager/",
        "Facebook::Billing::https://www.facebook.com/ads/manager/account/billing/",
        "Facebook::Business Manager::https://business.facebook.com/",
        "Dropshipping::",
        "Dropshipping::Amazon::https://www.amazon.com/",
        "Dropshipphing::eBay::https://ebay.com/",
        "Dropshipping::Walmart::https://walmart.com/",
        "Dropshipping::Shopify::https://www.shopify.com/",
        "Dropshipping::Taobao::https://taobao.com/",
        "Dropshipping::Aliexpress::https://aliexpress.com/",
        "Dropshipping::PayPal::https://paypal.com/",
        "Dropshipping::Payoneer::https://payoneer.com/",
        "Crypto::Coinlist::https://coinlist.co",
        "Crypto::Huobi::https://www.huobi.com/",
        "Crypto::Discord::https://discord.com",
        "Crypto::Premint::https://www.premint.xyz"
    )

    val settingsText: String get() = settingsLines.joinToString("\n")

    /** Parse one raw bookmark line into (folder, name, url); null when it has no URL. */
    fun parse(line: String): Triple<String, String, String>? {
        val parts = line.split("::").map { it.trim() }
        return when {
            parts.size >= 3 && parts[2].startsWith("http") ->
                Triple(parts[0], parts[1], parts[2])
            parts.size == 2 && parts[1].startsWith("http") ->
                Triple("", parts[0], parts[1])
            parts.size == 1 && parts[0].startsWith("http") ->
                Triple("", parts[0].removePrefix("https://").removePrefix("http://"), parts[0])
            else -> null
        }
    }

    /** All tappable bookmarks (lines that carry a URL). */
    fun links(lines: List<String>): List<Triple<String, String, String>> =
        lines.mapNotNull { parse(it) }
}
