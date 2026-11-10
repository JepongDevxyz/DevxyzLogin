package com.devxyz.login.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * DevxyzLogin theme — GoLogin capture palette (GOLOGIN_CAPTURE.md §1):
 * teal-green accent family #0FBECA / #00A987 / #14AAB8; light surfaces
 * #FAFAFA on white; dark surfaces #191A24 / #1E1F2B / #2C2D3D.
 */
val BrandTeal = Color(0xFF00A987)
val BrandTealLight = Color(0xFF0FBECA)
val BrandTealLink = Color(0xFF14AAB8)
val DarkSidebar = Color(0xFF191A24)
val DarkSurface = Color(0xFF1E1F2B)
val DarkVariant = Color(0xFF2C2D3D)
val StatusBlue = Color(0xFF2F7CF6)
val DangerRed = Color(0xFFE5484D)
val GreyText = Color(0xFF6B7280)

private val LightColors = lightColorScheme(
    primary = BrandTeal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDEF6EE),
    onPrimaryContainer = Color(0xFF04503F),
    secondary = BrandTealLight,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = Color(0xFF111827),
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = GreyText,
    outline = Color(0xFFE5E7EB),
    error = DangerRed,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = BrandTealLight,
    onPrimary = Color(0xFF062A28),
    primaryContainer = Color(0xFF0D8478),
    onPrimaryContainer = Color.White,
    secondary = BrandTeal,
    onSecondary = Color.White,
    background = DarkSurface,
    onBackground = Color(0xFFF3F4F6),
    surface = DarkSidebar,
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = DarkVariant,
    onSurfaceVariant = Color(0xFFB6BAC4),
    outline = Color(0xFF3A3B4D),
    error = DangerRed,
    onError = Color.White
)

/**
 * @param themeMode "system" | "light" | "dark" (Settings → Overview →
 * Interface theme, capture §12.1: System default / Light / Dark).
 */
@Composable
fun DevxyzTheme(themeMode: String = "system", content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content
    )
}
