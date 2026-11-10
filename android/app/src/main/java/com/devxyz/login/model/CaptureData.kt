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

    /** WebGL Renderer list is vendor-dependent in GoLogin. Qualcomm is the
     *  capture-verbatim Adreno set; the other families use the canonical
     *  ANGLE renderer strings for those GPU families (Mali / Apple
     *  M-series / Intel UHD+Iris / NVIDIA GeForce / AMD Radeon /
     *  Microsoft Basic Render Driver / Brave's generic Intel string). */
    fun webglRenderersFor(vendor: String): List<String> = when (vendor) {
        "Google Inc. (Qualcomm)", "Qualcomm" -> adrenoRenderers
        "Google Inc. (ARM)", "ARM" -> listOf(
            "ANGLE (ARM, Mali-G57 MC2, OpenGL ES 3.2)",
            "ANGLE (ARM, Mali-G68 MC4, OpenGL ES 3.2)",
            "ANGLE (ARM, Mali-G78, OpenGL ES 3.2)",
            "ANGLE (ARM, Mali-G610 MC6, OpenGL ES 3.2)",
            "ANGLE (ARM, Mali-G615 MC2, OpenGL ES 3.2)",
            "ANGLE (ARM, Mali-G710 MC10, OpenGL ES 3.2)",
            "ANGLE (ARM, Mali-G720 MC7, OpenGL ES 3.2)",
            "ANGLE (ARM, Mali-G925 MC12, OpenGL ES 3.2)"
        )
        "Google Inc. (Apple)", "Apple Inc." -> listOf(
            "ANGLE (Apple, ANGLE Metal Renderer: Apple M1, Unspecified Version)",
            "ANGLE (Apple, ANGLE Metal Renderer: Apple M1 Pro, Unspecified Version)",
            "ANGLE (Apple, ANGLE Metal Renderer: Apple M2, Unspecified Version)",
            "ANGLE (Apple, ANGLE Metal Renderer: Apple M3, Unspecified Version)",
            "ANGLE (Apple, ANGLE Metal Renderer: Apple M4, Unspecified Version)",
            "ANGLE (Apple, ANGLE Metal Renderer: Intel Iris Plus Graphics, Unspecified Version)"
        )
        "Google Inc. (Intel)", "Google Inc. (Intel Inc.)", "Intel Inc." -> listOf(
            "ANGLE (Intel, Intel(R) HD Graphics 520 Direct3D11 vs_5_0 ps_5_0, D3D11)",
            "ANGLE (Intel, Intel(R) HD Graphics 630 Direct3D11 vs_5_0 ps_5_0, D3D11)",
            "ANGLE (Intel, Intel(R) UHD Graphics 620 Direct3D11 vs_5_0 ps_5_0, D3D11)",
            "ANGLE (Intel, Intel(R) UHD Graphics 630 Direct3D11 vs_5_0 ps_5_0, D3D11)",
            "ANGLE (Intel, Intel(R) UHD Graphics 770 Direct3D11 vs_5_0 ps_5_0, D3D11)",
            "ANGLE (Intel, Intel(R) Iris(R) Plus Graphics 655 Direct3D11 vs_5_0 ps_5_0, D3D11)",
            "ANGLE (Intel, Intel(R) Iris(R) Xe Graphics Direct3D11 vs_5_0 ps_5_0, D3D11)"
        )
        "Google Inc. (NVIDIA)", "Google Inc. (NVIDIA Corporation)" -> listOf(
            "ANGLE (NVIDIA, NVIDIA GeForce GTX 1650 Direct3D11 vs_5_0 ps_5_0, D3D11)",
            "ANGLE (NVIDIA, NVIDIA GeForce GTX 1660 SUPER Direct3D11 vs_5_0 ps_5_0, D3D11)",
            "ANGLE (NVIDIA, NVIDIA GeForce RTX 3050 Direct3D11 vs_5_0 ps_5_0, D3D11)",
            "ANGLE (NVIDIA, NVIDIA GeForce RTX 3060 Direct3D11 vs_5_0 ps_5_0, D3D11)",
            "ANGLE (NVIDIA, NVIDIA GeForce RTX 3080 Direct3D11 vs_5_0 ps_5_0, D3D11)",
            "ANGLE (NVIDIA, NVIDIA GeForce RTX 4060 Direct3D11 vs_5_0 ps_5_0, D3D11)",
            "ANGLE (NVIDIA, NVIDIA GeForce RTX 4070 Direct3D11 vs_5_0 ps_5_0, D3D11)"
        )
        "Google Inc. (AMD)" -> listOf(
            "ANGLE (AMD, AMD Radeon RX 580 Direct3D11 vs_5_0 ps_5_0, D3D11)",
            "ANGLE (AMD, AMD Radeon RX 6600 Direct3D11 vs_5_0 ps_5_0, D3D11)",
            "ANGLE (AMD, AMD Radeon RX 6700 XT Direct3D11 vs_5_0 ps_5_0, D3D11)",
            "ANGLE (AMD, AMD Radeon RX 7800 XT Direct3D11 vs_5_0 ps_5_0, D3D11)",
            "ANGLE (AMD, AMD Radeon(TM) Graphics Direct3D11 vs_5_0 ps_5_0, D3D11)"
        )
        "Google Inc. (Microsoft)" -> listOf(
            "ANGLE (Microsoft, Microsoft Basic Render Driver Direct3D11 vs_5_0 ps_5_0, D3D11)"
        )
        "Brave" -> listOf(
            "ANGLE (Intel, Intel(R) UHD Graphics 630 Direct3D11 vs_5_0 ps_5_0, D3D11)",
            "ANGLE (Intel, Intel(R) Iris(R) Xe Graphics Direct3D11 vs_5_0 ps_5_0, D3D11)"
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

    /** Built-in proxy countries shown first in GoLogin's combobox (flag, name).
     *  Superseded for picking by the full [Countries] list (web v3 parity);
     *  kept for the capture record. */
    val proxyCountries: List<Pair<String, String>> = listOf(
        "FR" to "France",
        "DE" to "Germany",
        "GB" to "United Kingdom",
        "US" to "United States",
        "AU" to "Australia"
    )

    /**
     * Languages modal checklist (Settings → Default profile settings,
     * capture §12.2: a searchable locale checklist running af → zu).
     * tag -> display name, ordered by tag.
     */
    val localeOptions: List<Pair<String, String>> = listOf(
        "af" to "Afrikaans", "af-ZA" to "Afrikaans (South Africa)",
        "am" to "Amharic", "ar" to "Arabic", "ar-EG" to "Arabic (Egypt)",
        "ar-SA" to "Arabic (Saudi Arabia)", "az" to "Azerbaijani",
        "be" to "Belarusian", "bg" to "Bulgarian", "bg-BG" to "Bulgarian (Bulgaria)",
        "bn" to "Bengali", "bn-BD" to "Bengali (Bangladesh)", "bs" to "Bosnian",
        "ca" to "Catalan", "cs" to "Czech", "cs-CZ" to "Czech (Czechia)",
        "cy" to "Welsh", "da" to "Danish", "da-DK" to "Danish (Denmark)",
        "de" to "German", "de-AT" to "German (Austria)", "de-CH" to "German (Switzerland)",
        "de-DE" to "German (Germany)", "el" to "Greek", "el-GR" to "Greek (Greece)",
        "en" to "English", "en-AU" to "English (Australia)", "en-CA" to "English (Canada)",
        "en-GB" to "English (United Kingdom)", "en-IE" to "English (Ireland)",
        "en-IN" to "English (India)", "en-NZ" to "English (New Zealand)",
        "en-PH" to "English (Philippines)", "en-SG" to "English (Singapore)",
        "en-US" to "English (United States)", "en-ZA" to "English (South Africa)",
        "es" to "Spanish", "es-AR" to "Spanish (Argentina)", "es-CL" to "Spanish (Chile)",
        "es-CO" to "Spanish (Colombia)", "es-ES" to "Spanish (Spain)",
        "es-MX" to "Spanish (Mexico)", "es-PE" to "Spanish (Peru)",
        "es-VE" to "Spanish (Venezuela)", "et" to "Estonian", "eu" to "Basque",
        "fa" to "Persian", "fa-IR" to "Persian (Iran)", "fi" to "Finnish",
        "fi-FI" to "Finnish (Finland)", "fil" to "Filipino", "fil-PH" to "Filipino (Philippines)",
        "fr" to "French", "fr-BE" to "French (Belgium)", "fr-CA" to "French (Canada)",
        "fr-CH" to "French (Switzerland)", "fr-FR" to "French (France)",
        "ga" to "Irish", "gl" to "Galician", "gu" to "Gujarati", "he" to "Hebrew",
        "he-IL" to "Hebrew (Israel)", "hi" to "Hindi", "hi-IN" to "Hindi (India)",
        "hr" to "Croatian", "hr-HR" to "Croatian (Croatia)", "hu" to "Hungarian",
        "hu-HU" to "Hungarian (Hungary)", "hy" to "Armenian", "id" to "Indonesian",
        "id-ID" to "Indonesian (Indonesia)", "is" to "Icelandic", "it" to "Italian",
        "it-CH" to "Italian (Switzerland)", "it-IT" to "Italian (Italy)",
        "ja" to "Japanese", "ja-JP" to "Japanese (Japan)", "ka" to "Georgian",
        "kk" to "Kazakh", "km" to "Khmer", "kn" to "Kannada", "ko" to "Korean",
        "ko-KR" to "Korean (South Korea)", "lo" to "Lao", "lt" to "Lithuanian",
        "lt-LT" to "Lithuanian (Lithuania)", "lv" to "Latvian", "lv-LV" to "Latvian (Latvia)",
        "mk" to "Macedonian", "ml" to "Malayalam", "mn" to "Mongolian", "mr" to "Marathi",
        "ms" to "Malay", "ms-MY" to "Malay (Malaysia)", "mt" to "Maltese",
        "my" to "Burmese", "nb" to "Norwegian Bokmål", "ne" to "Nepali",
        "nl" to "Dutch", "nl-BE" to "Dutch (Belgium)", "nl-NL" to "Dutch (Netherlands)",
        "nn" to "Norwegian Nynorsk", "no" to "Norwegian", "pa" to "Punjabi",
        "pl" to "Polish", "pl-PL" to "Polish (Poland)", "ps" to "Pashto",
        "pt" to "Portuguese", "pt-BR" to "Portuguese (Brazil)", "pt-PT" to "Portuguese (Portugal)",
        "ro" to "Romanian", "ro-RO" to "Romanian (Romania)", "ru" to "Russian",
        "ru-RU" to "Russian (Russia)", "ru-UA" to "Russian (Ukraine)",
        "si" to "Sinhala", "sk" to "Slovak", "sk-SK" to "Slovak (Slovakia)",
        "sl" to "Slovenian", "sq" to "Albanian", "sr" to "Serbian",
        "sv" to "Swedish", "sv-SE" to "Swedish (Sweden)", "sw" to "Swahili",
        "ta" to "Tamil", "ta-IN" to "Tamil (India)", "te" to "Telugu",
        "th" to "Thai", "th-TH" to "Thai (Thailand)", "tl" to "Tagalog",
        "tr" to "Turkish", "tr-TR" to "Turkish (Türkiye)", "uk" to "Ukrainian",
        "uk-UA" to "Ukrainian (Ukraine)", "ur" to "Urdu", "ur-PK" to "Urdu (Pakistan)",
        "uz" to "Uzbek", "vi" to "Vietnamese", "vi-VN" to "Vietnamese (Vietnam)",
        "zh" to "Chinese", "zh-CN" to "Chinese (China)", "zh-HK" to "Chinese (Hong Kong)",
        "zh-TW" to "Chinese (Taiwan)", "zu" to "Zulu"
    )

    /** Accept-Language header preview for a selected language list
     *  (capture §12.2 format: first tag bare, then ;q=0.9, ;q=0.8, ...). */
    fun acceptLanguageHeader(languages: List<String>): String {
        if (languages.isEmpty()) return ""
        return languages.mapIndexed { i, tag ->
            if (i == 0) tag else "$tag;q=${(10 - i) / 10.0}"
        }.joinToString(",")
    }

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
