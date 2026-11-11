package com.devxyz.login.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared building blocks for the v2.4.0 UI (ANDROID_UI_SPEC_V2.md,
 * measured from the user's own recording of the real GoLogin app):
 * outlined field boxes with the small gray label INSIDE the top,
 * hairline section rows, the solid-green bottom action button, and
 * bottom sheets with ~20dp top corners. Everything reads its colors
 * from the theme tokens (dark mode included) — the only literal
 * colors are the measured brand green / red in Theme.kt.
 */

// ------------------------------------------------------------------
// Spec tints (light/dark picked through the theme, never hardcoded
// light-only values at call sites)
// ------------------------------------------------------------------

@Composable
fun runningTintColor(): Color =
    if (LocalDarkTheme.current) RunningTintDark else RunningTintLight

@Composable
fun proxyOkColor(): Color =
    if (LocalDarkTheme.current) ProxyOkDark else ProxyOkLight

@Composable
fun tagGreenBgColor(): Color =
    if (LocalDarkTheme.current) TagGreenBgDark else TagGreenBgLight

@Composable
fun tagGreenTextColor(): Color =
    if (LocalDarkTheme.current) TagGreenTextDark else TagGreenTextLight

@Composable
fun tagGrayBgColor(): Color =
    if (LocalDarkTheme.current) TagGrayBgDark else TagGrayBgLight

// ------------------------------------------------------------------
// Bottom sheet shell — rounded top corners ~20dp, drag handle
// ------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        content()
    }
}

/** Sheet title (spec: bold ~18sp, 16dp side padding). */
@Composable
fun SheetTitle(text: String) {
    Text(
        text,
        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

// ------------------------------------------------------------------
// Back header — back arrow + bold title + optional trailing actions
// ------------------------------------------------------------------

@Composable
fun BackHeader(
    title: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(
            title,
            style = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        actions()
    }
}

// ------------------------------------------------------------------
// Spec field boxes — outlined rounded box (~12dp radius, 1dp light
// border), small gray label inside the top, value ~16sp.
// ------------------------------------------------------------------

private val SpecFieldShape = RoundedCornerShape(12.dp)

@Composable
private fun SpecFieldShell(
    label: String,
    modifier: Modifier,
    onClick: (() -> Unit)?,
    highlighted: Boolean = false,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .clip(SpecFieldShape)
            .background(if (highlighted) proxyOkColor() else Color.Transparent)
            .border(
                1.dp,
                if (highlighted) BrandGreen else MaterialTheme.colorScheme.outline,
                SpecFieldShape
            )
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            label,
            style = TextStyle(fontSize = 12.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(1.dp))
        content()
    }
}

/** Tappable spec field showing a (possibly placeholder) value. */
@Composable
fun SpecValueField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val isPlaceholder = value.isBlank() && placeholder != null
    SpecFieldShell(label = label, modifier = modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(8.dp))
            }
            Text(
                if (isPlaceholder) placeholder!! else value,
                style = TextStyle(fontSize = 16.sp),
                color = if (isPlaceholder) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            if (trailing != null) trailing()
        }
    }
}

/** Editable spec field: label inside the top, free text below it. */
@Composable
fun SpecTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    clearable: Boolean = false,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    trailing: @Composable (() -> Unit)? = null,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation =
        androidx.compose.ui.text.input.VisualTransformation.None,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions =
        androidx.compose.foundation.text.KeyboardOptions.Default
) {
    SpecFieldShell(label = label, modifier = modifier, onClick = null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty() && placeholder != null) {
                    Text(
                        placeholder,
                        style = TextStyle(fontSize = 16.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = singleLine,
                    enabled = enabled,
                    visualTransformation = visualTransformation,
                    keyboardOptions = keyboardOptions,
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(BrandGreen),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (clearable && value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.Close, contentDescription = "Clear",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            if (trailing != null) trailing()
        }
    }
}

// ------------------------------------------------------------------
// Section row — full-width label + chevron, hairline divider below
// ------------------------------------------------------------------

@Composable
fun SectionRow(label: String, onClick: () -> Unit) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 17.dp)
        ) {
            Text(label, style = TextStyle(fontSize = 16.sp), modifier = Modifier.weight(1f))
            Icon(
                Icons.Filled.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

// ------------------------------------------------------------------
// Solid-green bottom action button (spec: ~54dp, radius ~12dp)
// ------------------------------------------------------------------

@Composable
fun GreenButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BrandGreen,
            contentColor = Color.White,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(54.dp)
    ) {
        Text(text, style = TextStyle(fontSize = 16.sp))
    }
}

// ------------------------------------------------------------------
// Custom confirm sheet (NO default dialogs): title, message, red
// confirm button + Cancel.
// ------------------------------------------------------------------

@Composable
fun ConfirmSheet(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AppBottomSheet(onDismiss = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            SheetTitle(title)
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DangerRed,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(52.dp)
            ) { Text(confirmLabel, style = TextStyle(fontSize = 16.sp)) }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.weight(1f))
                androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
            }
        }
    }
}

// ------------------------------------------------------------------
// Row glyphs (kept from the v2.3.0 list, now on flat white rows)
// ------------------------------------------------------------------

/** Capture state dot: ready = hollow grey, running = green, launching/syncing = blue. */
@Composable
fun StateDot(state: String) {
    Canvas(modifier = Modifier.size(10.dp)) {
        when (state) {
            "running" -> drawCircle(color = BrandGreen)
            "launching", "syncing" -> drawCircle(color = StatusBlue)
            else -> drawCircle(color = Color(0xFF9CA3AF), style = Stroke(width = 2f))
        }
    }
}

/**
 * Gray OS glyph for the row's right cluster (~22dp), drawn as simple
 * vector shapes — Material icons carry no Apple logo and the spec shows
 * plain gray brand glyphs (Windows / Apple / Android; Linux gets a
 * penguin silhouette). Coordinates are in a 24-unit box.
 */
@Composable
fun OsGlyph(os: String) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    val cutout = MaterialTheme.colorScheme.surface
    Canvas(modifier = Modifier.size(22.dp)) {
        fun x(v: Float) = v / 24f * size.width
        fun y(v: Float) = v / 24f * size.height
        when (os) {
            "windows10", "windows11" -> {
                val panes = Path().apply {
                    moveTo(x(3f), y(5.4f)); lineTo(x(10.9f), y(4.4f))
                    lineTo(x(10.9f), y(11f)); lineTo(x(3f), y(11f)); close()
                    moveTo(x(12.3f), y(4.2f)); lineTo(x(21f), y(3f))
                    lineTo(x(21f), y(11f)); lineTo(x(12.3f), y(11f)); close()
                    moveTo(x(3f), y(12.6f)); lineTo(x(10.9f), y(12.6f))
                    lineTo(x(10.9f), y(19.6f)); lineTo(x(3f), y(18.6f)); close()
                    moveTo(x(12.3f), y(12.6f)); lineTo(x(21f), y(12.6f))
                    lineTo(x(21f), y(21f)); lineTo(x(12.3f), y(19.8f)); close()
                }
                drawPath(panes, tint)
            }
            "macIntel", "macArm" -> {
                val body = Path().apply {
                    moveTo(x(12f), y(6.6f))
                    cubicTo(x(13.4f), y(4.6f), x(15.6f), y(4.2f), x(17.1f), y(5.6f))
                    cubicTo(x(20f), y(8.2f), x(19.6f), y(13f), x(17.6f), y(17f))
                    cubicTo(x(16.1f), y(19.9f), x(14.2f), y(21f), x(12f), y(19.9f))
                    cubicTo(x(9.8f), y(21f), x(7.9f), y(19.9f), x(6.4f), y(17f))
                    cubicTo(x(4.4f), y(13f), x(4f), y(8.2f), x(6.9f), y(5.6f))
                    cubicTo(x(8.4f), y(4.2f), x(10.6f), y(4.6f), x(12f), y(6.6f))
                    close()
                }
                val leaf = Path().apply {
                    moveTo(x(13.1f), y(5.6f))
                    cubicTo(x(12.9f), y(3.1f), x(14.4f), y(1.8f), x(16.6f), y(1.7f))
                    cubicTo(x(16.5f), y(4.1f), x(15.1f), y(5.4f), x(13.1f), y(5.6f))
                    close()
                }
                drawPath(body, tint)
                drawPath(leaf, tint)
            }
            "android" -> {
                drawLine(
                    tint, Offset(x(8.2f), y(6.8f)), Offset(x(6.4f), y(3.4f)),
                    strokeWidth = x(1.5f), cap = StrokeCap.Round
                )
                drawLine(
                    tint, Offset(x(15.8f), y(6.8f)), Offset(x(17.6f), y(3.4f)),
                    strokeWidth = x(1.5f), cap = StrokeCap.Round
                )
                val head = Path().apply {
                    moveTo(x(5f), y(11.5f))
                    arcTo(Rect(x(5f), y(6f), x(19f), y(17f)), 180f, 180f, false)
                    lineTo(x(19f), y(16f)); lineTo(x(5f), y(16f)); close()
                }
                drawPath(head, tint)
                drawCircle(cutout, radius = x(1.15f), center = Offset(x(9.4f), y(10.6f)))
                drawCircle(cutout, radius = x(1.15f), center = Offset(x(14.6f), y(10.6f)))
            }
            else -> {
                // linux — penguin silhouette
                val body = Path().apply {
                    moveTo(x(12f), y(2.8f))
                    cubicTo(x(16.2f), y(2.8f), x(17.8f), y(6.8f), x(17.8f), y(11f))
                    cubicTo(x(17.8f), y(14.8f), x(16.2f), y(17.2f), x(14.6f), y(18.8f))
                    lineTo(x(17.2f), y(21.2f)); lineTo(x(6.8f), y(21.2f))
                    lineTo(x(9.4f), y(18.8f))
                    cubicTo(x(7.8f), y(17.2f), x(6.2f), y(14.8f), x(6.2f), y(11f))
                    cubicTo(x(6.2f), y(6.8f), x(7.8f), y(2.8f), x(12f), y(2.8f))
                    close()
                }
                drawPath(body, tint)
                drawOval(
                    cutout,
                    topLeft = Offset(x(9f), y(10.5f)),
                    size = Size(x(6f), y(8.6f))
                )
                drawCircle(cutout, radius = x(1.25f), center = Offset(x(9.9f), y(7.6f)))
                drawCircle(cutout, radius = x(1.25f), center = Offset(x(14.1f), y(7.6f)))
                val beak = Path().apply {
                    moveTo(x(10.7f), y(9.4f)); lineTo(x(13.3f), y(9.4f))
                    lineTo(x(12f), y(11f)); close()
                }
                drawPath(beak, cutout)
            }
        }
    }
}
