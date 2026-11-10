package com.devxyz.login.model

import kotlin.random.Random

/**
 * "New Fingerprint" generator (GoLogin Overview tab parity).
 * Produces an internally CONSISTENT fingerprint for the chosen OS: UA,
 * platform, screen, WebGL vendor/renderer and language pools all come from
 * the same OS family, and a fixed [seed] reproduces the same fingerprint.
 */
object FingerprintGenerator {

    private val chromeVersions = listOf("126.0.6478.127", "128.0.6613.120", "130.0.6723.92", "131.0.6778.140")

    private val languagePools = listOf(
        listOf("en-US", "en"),
        listOf("en-GB", "en"),
        listOf("en-US", "en", "es"),
        listOf("de-DE", "de", "en"),
        listOf("fr-FR", "fr", "en"),
        listOf("fil-PH", "fil", "en-US", "en"),
        listOf("ja-JP", "ja", "en")
    )

    fun generate(os: String, seed: Long = System.nanoTime()): Fingerprint {
        val rnd = Random(seed)
        val version = chromeVersions[rnd.nextInt(chromeVersions.size)]
        val languages = languagePools[rnd.nextInt(languagePools.size)]
        val cpu = listOf(4, 6, 8, 8, 12, 16)[rnd.nextInt(6)]
        val ram = listOf(4, 8, 8, 16, 32)[rnd.nextInt(5)]

        val nav: NavigatorInfo
        val hw: HardwareInfo
        when (os) {
            "windows10", "windows11" -> {
                val win = if (os == "windows11") "Windows NT 10.0; Win64; x64" else "Windows NT 10.0; Win64; x64"
                nav = NavigatorInfo(
                    userAgent = "Mozilla/5.0 ($win) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$version Safari/537.36",
                    browserVersion = version,
                    screenResolution = listOf("1920x1080", "1366x768", "1536x864", "2560x1440")[rnd.nextInt(4)],
                    languages = languages, platform = "Win32", cpuCores = cpu, ram = ram
                )
                hw = windowsWebGl(rnd)
            }
            "macIntel", "macArm" -> {
                nav = NavigatorInfo(
                    userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$version Safari/537.36",
                    browserVersion = version,
                    screenResolution = listOf("1440x900", "1680x1050", "2560x1600", "1512x982")[rnd.nextInt(4)],
                    languages = languages, platform = "MacIntel", cpuCores = cpu, ram = ram
                )
                hw = HardwareInfo(
                    webglVendor = "Google Inc. (Apple)",
                    webglRenderer = if (os == "macArm")
                        "ANGLE (Apple, ANGLE Metal Renderer: Apple M1, Unspecified Version)"
                    else
                        "ANGLE (Apple, ANGLE Metal Renderer: Intel Iris Plus Graphics, Unspecified Version)"
                )
            }
            "linux" -> {
                nav = NavigatorInfo(
                    userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$version Safari/537.36",
                    browserVersion = version,
                    screenResolution = listOf("1920x1080", "1366x768", "2560x1440")[rnd.nextInt(3)],
                    languages = languages, platform = "Linux x86_64", cpuCores = cpu, ram = ram
                )
                hw = windowsWebGl(rnd)
            }
            else -> { // android (GoLogin Android app default: mobile fingerprint)
                val device = listOf(
                    Triple("Pixel 8", "Android 14; Pixel 8", "1080x2400"),
                    Triple("Pixel 7", "Android 13; Pixel 7", "1080x2400"),
                    Triple("Galaxy S23", "Android 13; SM-S911B", "1080x2340"),
                    Triple("Galaxy S24", "Android 14; SM-S921B", "1080x2340")
                )[rnd.nextInt(4)]
                nav = NavigatorInfo(
                    userAgent = "Mozilla/5.0 (Linux; ${device.second}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$version Mobile Safari/537.36",
                    browserVersion = version,
                    screenResolution = device.third,
                    languages = languages, platform = "Linux armv8l",
                    cpuCores = listOf(4, 8)[rnd.nextInt(2)], ram = listOf(4, 6, 8, 12)[rnd.nextInt(4)]
                )
                hw = HardwareInfo(
                    webglVendor = "Google Inc. (Qualcomm)",
                    webglRenderer = "ANGLE (Qualcomm, Adreno (TM) 740, OpenGL ES 3.2)"
                )
            }
        }

        return Fingerprint(
            navigator = nav,
            fontsMasking = true,
            mediaDevices = MediaDevices(
                videoInputs = 1,
                audioInputs = 1,
                audioOutputs = listOf(1, 2)[rnd.nextInt(2)]
            ),
            hardware = hw,
            webrtcMode = "basedOnIp",
            timezone = TimezoneInfo(auto = true),
            geolocation = GeolocationInfo(mode = "prompt", auto = true)
        )
    }

    private fun windowsWebGl(rnd: Random): HardwareInfo {
        val pair = listOf(
            "Google Inc. (Intel)" to "ANGLE (Intel, Intel(R) UHD Graphics 630 Direct3D11 vs_5_0 ps_5_0, D3D11)",
            "Google Inc. (NVIDIA)" to "ANGLE (NVIDIA, NVIDIA GeForce RTX 3060 Direct3D11 vs_5_0 ps_5_0, D3D11)",
            "Google Inc. (AMD)" to "ANGLE (AMD, AMD Radeon RX 6600 Direct3D11 vs_5_0 ps_5_0, D3D11)",
            "Google Inc. (Intel)" to "ANGLE (Intel, Intel(R) Iris(R) Xe Graphics Direct3D11 vs_5_0 ps_5_0, D3D11)"
        )[rnd.nextInt(4)]
        return HardwareInfo(webglVendor = pair.first, webglRenderer = pair.second)
    }
}
