package com.devxyz.login.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * DevxyzLogin theme — v2.4.0 palette measured from the REAL current
 * GoLogin ANDROID app (ANDROID_UI_SPEC_V2.md, from the user's own
 * screen recording of the app on her phone): primary green #00A886,
 * white pages, FLAT rows with hairline dividers (no gray cards),
 * running-row tint #F3FDF5, "Proxy is ok!" box #F5FCF7, pale-green tag
 * pills, solid red stop square. This REPLACES the v2.3.0 store-shot
 * palette (#5FB173 leaf green / #F6F5F4 cards / dark FAB) — those
 * screenshots were the OLD app UI.
 *
 * Every color role is set explicitly in both schemes so no Material3
 * baseline (purple-tinted) fallback can leak into chips, sheets or
 * containers. Dark-mode values keep the 2.2.0 dark structure
 * (#191A24 / #1E1F2B / #2C2D3D) with the primary swapped to the same
 * green family — the real app's dark palette is not captured.
 */
val BrandGreen = Color(0xFF00A886)
val BrandGreenLight = Color(0xFF33C9AC)
val BrandGreenDeep = Color(0xFF00755F)
val FabBackground = Color(0xFF293138)
val DarkSidebar = Color(0xFF191A24)
val DarkSurface = Color(0xFF1E1F2B)
val DarkVariant = Color(0xFF2C2D3D)
val StatusBlue = Color(0xFF2F7CF6)
val DangerRed = Color(0xFFE5484D)
val GreyText = Color(0xFF8A8A8E)

/** Spec tints that have no Material3 role — picked light/dark via
 *  [LocalDarkTheme] in the component helpers (Components.kt). */
val RunningTintLight = Color(0xFFF3FDF5)
val RunningTintDark = Color(0xFF10312A)
val ProxyOkLight = Color(0xFFF5FCF7)
val ProxyOkDark = Color(0xFF10312A)
val TagGreenBgLight = Color(0xFFE7F6EC)
val TagGreenBgDark = Color(0xFF1C4634)
val TagGreenTextLight = Color(0xFF177245)
val TagGreenTextDark = Color(0xFF7FD6A4)
val TagGrayBgLight = Color(0xFFF0F0F0)
val TagGrayBgDark = Color(0xFF2C2D3D)

/** True when the EFFECTIVE theme (incl. the Settings theme override)
 *  is dark — provided by [DevxyzTheme] so components can pick the
 *  spec tints above without hardcoding light-only colors. */
val LocalDarkTheme = staticCompositionLocalOf { false }

private val LightColors = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7F6EC),
    onPrimaryContainer = Color(0xFF00503F),
    secondary = BrandGreenDeep,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDFF5EE),
    onSecondaryContainer = Color(0xFF00503F),
    tertiary = Color(0xFF5B6066),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE8EAED),
    onTertiaryContainer = Color(0xFF1B1B1B),
    background = Color.White,
    onBackground = Color(0xFF1B1B1B),
    surface = Color.White,
    onSurface = Color(0xFF1B1B1B),
    surfaceVariant = Color(0xFFF6F5F4),
    onSurfaceVariant = GreyText,
    surfaceDim = Color(0xFFE3E1E0),
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF8F6F5),
    surfaceContainerHigh = Color(0xFFF6F5F4),
    surfaceContainerHighest = Color(0xFFEFEDEB),
    surfaceTint = BrandGreen,
    inverseSurface = Color(0xFF1B1B1B),
    inverseOnSurface = Color(0xFFF5F5F5),
    inversePrimary = BrandGreenLight,
    outline = Color(0xFFE7E7E7),
    outlineVariant = Color(0xFFECECEC),
    scrim = Color.Black,
    error = DangerRed,
    onError = Color.White,
    errorContainer = Color(0xFFFDECEC),
    onErrorContainer = Color(0xFF7A1F22)
)

private val DarkColors = darkColorScheme(
    primary = BrandGreenLight,
    onPrimary = Color(0xFF00382C),
    primaryContainer = Color(0xFF0E4A3C),
    onPrimaryContainer = Color(0xFFC9F2E4),
    secondary = BrandGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0E4A3C),
    onSecondaryContainer = Color(0xFFC9F2E4),
    tertiary = Color(0xFF9AA0A6),
    onTertiary = DarkSidebar,
    tertiaryContainer = DarkVariant,
    onTertiaryContainer = Color(0xFFE8EAED),
    background = DarkSurface,
    onBackground = Color(0xFFF3F4F6),
    surface = DarkSidebar,
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = DarkVariant,
    onSurfaceVariant = Color(0xFFB6BAC4),
    surfaceDim = Color(0xFF14151D),
    surfaceBright = DarkVariant,
    surfaceContainerLowest = Color(0xFF12131B),
    surfaceContainerLow = DarkSurface,
    surfaceContainer = Color(0xFF232433),
    surfaceContainerHigh = DarkVariant,
    surfaceContainerHighest = Color(0xFF35364A),
    surfaceTint = BrandGreenLight,
    inverseSurface = Color(0xFFF3F4F6),
    inverseOnSurface = Color(0xFF1B1B1B),
    inversePrimary = BrandGreen,
    outline = Color(0xFF3A3B4D),
    outlineVariant = DarkVariant,
    scrim = Color.Black,
    error = DangerRed,
    onError = Color.White,
    errorContainer = Color(0xFF5C2326),
    onErrorContainer = Color(0xFFFDECEC)
)

/**
 * Bold-led typography (the real app's UI is bold-led: profile names,
 * tab labels, accordion headers, screen titles). Only the styles the
 * spec speaks about are overridden; the rest keep the M3 defaults.
 */
private val BaseTypography = Typography()
private val AppTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = BaseTypography.titleLarge.fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = BaseTypography.titleMedium.fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = BaseTypography.titleSmall.fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    )
)

/**
 * Spec radii (v2 spec): outlined fields and bottom buttons ~12dp
 * (large), bottom sheets ~20dp top corners (set at the sheet call
 * sites). Buttons set their shapes explicitly at the call sites.
 */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(20.dp)
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
    CompositionLocalProvider(LocalDarkTheme provides dark) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
