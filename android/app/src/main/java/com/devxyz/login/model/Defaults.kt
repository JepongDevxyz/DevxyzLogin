package com.devxyz.login.model

import kotlinx.serialization.Serializable

/**
 * Settings → "Default profile settings" (capture §12.2). Stored locally as
 * JSON in SharedPreferences — it is app settings, not part of the shared
 * profile store schema. Defaults transcribe GoLogin's page, incl.
 * IndexedDB OFF and "Without proxy" selected.
 */
@Serializable
data class DefaultProfileSettings(
    /** Language "Auto" + Based on IP toggle */
    val languageAuto: Boolean = true,
    val minResolution: String = "1920x1080",
    val maxResolution: String = "1920x1080",
    /** "device" (Device OS pill, selected in GoLogin) or an OsOptions value */
    val osMode: String = "device",
    val profileNameTemplate: String = "profile {number}",
    /** Proxy & Location segments: "devxyz" (Gologin proxy) | "none" (Without proxy, selected) */
    val proxyMode: String = "none",
    val customDns: String = "",
    val disableImageLoading: Boolean = false,
    val disableVideoAutoplay: Boolean = false,
    /** Orbita/browser version major for new profiles */
    val browserVersion: String = "156",
    /** "newTab" | "previouslyOpened" | "specified" */
    val startUrl: String = "previouslyOpened",
    /** Settings-variant default bookmarks (NOT the editor variant) */
    val bookmarksText: String = DefaultBookmarks.settingsText,
    val launchArguments: String = "",
    val saveHistory: Boolean = true,
    val saveBookmarks: Boolean = true,
    val savePasswords: Boolean = true,
    val localStorage: Boolean = true,
    /** Capture §12.2: IndexedDB is OFF in Default profile settings */
    val indexedDb: Boolean = false,
    val extensionStorage: Boolean = true,
    val systemExtensions: Boolean = false
)
