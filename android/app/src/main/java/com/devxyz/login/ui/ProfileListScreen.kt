package com.devxyz.login.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devxyz.login.data.ApiResult
import com.devxyz.login.data.ProfileStore
import com.devxyz.login.data.ServerApi
import com.devxyz.login.model.CaptureData
import com.devxyz.login.model.Countries
import com.devxyz.login.model.DefaultBookmarks
import com.devxyz.login.model.FingerprintGenerator
import com.devxyz.login.model.Folder
import com.devxyz.login.model.OsOptions
import com.devxyz.login.model.Profile
import com.devxyz.login.model.ProxyConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Profile list — v2.3.0 visual rebuild against the REAL GoLogin Android
 * app (ANDROID_UI_SPEC.md, pixel-measured from v3.1.151 screenshots):
 * - NO app bar: the burger sits in the same row as the search field
 *   (white, thin border, 10dp radius, placeholder "Search...").
 * - Tabs: ALL / SHARED PROFILES / folder names, uppercase bold dark
 *   text; the selected tab is marked ONLY by a primary-green underline
 *   spanning the label width.
 * - Rows are light cards (#F6F5F4, 10dp radius, 16dp padding): bold
 *   name + flag/lowercase descriptor on the left; on the right a green
 *   pin (when pinned), a gray OS glyph (~22dp) and the Run button
 *   (solid #5FB173, white "Run", ~76x40dp, 8dp radius). Running rows
 *   show View + Stop (outlined) in the same right cluster. No state
 *   dot, no status text, no host:port, no tag line in the row.
 * - The "+" FAB is a dark (#293138) CIRCLE.
 * Behaviour is unchanged from 2.2.0: Run opens the Manual/API choice
 * sheet (capture §0); the quick-settings sheet is reached via ⋮ →
 * Cookies; the row ⋮ menu keeps the exact capture §6 order. The state
 * dot survives only inside the quick-settings sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileListScreen(
    profiles: List<Profile>,
    folders: List<Folder>,
    onOpenDrawer: () -> Unit = {},
    onRunMode: (Profile, Boolean) -> Unit,
    onStop: (Profile) -> Unit,
    onEdit: (Profile) -> Unit,
    onClone: (Profile) -> Unit,
    onDelete: (Profile) -> Unit,
    onUpdate: (Profile) -> Unit,
    onNewProfile: () -> Unit,
    /** v4: session token for real proxy checks in the quick sheet (null = local mode). */
    apiToken: String? = null
) {
    var query by remember { mutableStateOf("") }
    // Android-app tabs: "all" | "shared" | a folder id
    var tab by remember { mutableStateOf("all") }
    var pinnedIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val visible = profiles.filter { p ->
        p.deletedAt == null &&
            when (tab) {
                // Profiles shared WITH you live on the account, not on this
                // device — the local store has none, so the tab is empty.
                "shared" -> false
                "all" -> true
                else -> tab in p.folderIds
            } &&
            (query.isBlank() || p.name.contains(query, ignoreCase = true) || p.id.contains(query, ignoreCase = true))
    }.sortedByDescending { it.id in pinnedIds }

    Scaffold(
        // The outer MainActivity scaffold already applied the system-bar
        // insets; zeroing them here keeps the search row tight under the
        // status bar (the real app has no app bar eating that space).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewProfile,
                containerColor = FabBackground,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add profile")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            // Spec: burger inline with the search field, no app title.
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Filled.Menu, contentDescription = "Menu")
                }
                Spacer(Modifier.width(4.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedBorderColor = BrandGreen
                    )
                )
            }
            Spacer(Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                item {
                    ListTab("ALL", tab == "all") { tab = "all" }
                }
                item {
                    ListTab("SHARED PROFILES", tab == "shared") { tab = "shared" }
                }
                items(folders) { folder ->
                    ListTab(folder.name.uppercase(), tab == folder.id) { tab = folder.id }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (visible.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (tab == "shared") {
                        Text(
                            "No shared profiles",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Profiles other people share with you will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            "No profiles yet",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onNewProfile,
                            shape = RoundedCornerShape(8.dp)
                        ) { Text("+ Add profile") }
                    }
                }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(visible, key = { it.id }) { profile ->
                    ProfileRow(
                        profile = profile,
                        folders = folders,
                        pinned = profile.id in pinnedIds,
                        apiToken = apiToken,
                        onRunMode = onRunMode,
                        onStop = { onStop(profile) },
                        onEdit = { onEdit(profile) },
                        onClone = { onClone(profile) },
                        onDelete = { onDelete(profile) },
                        onUpdate = onUpdate,
                        onTogglePin = {
                            pinnedIds = if (profile.id in pinnedIds) pinnedIds - profile.id
                            else pinnedIds + profile.id
                        }
                    )
                }
            }
        }
    }
}

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
 * penguin silhouette). Details knocked out of a glyph (Android eyes,
 * penguin belly/eyes/beak) use the row-card color (surfaceVariant).
 * Coordinates are in a 24-unit box, scaled to the canvas.
 */
@Composable
private fun OsGlyph(os: String) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    val cutout = MaterialTheme.colorScheme.surfaceVariant
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

/**
 * Android-app tab (spec screen-2): uppercase ~13sp BOLD dark text for
 * every tab; selection is shown ONLY by a primary-green underline that
 * spans the label's own width.
 */
@Composable
private fun ListTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .width(IntrinsicSize.Max),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            label,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (selected) BrandGreen else Color.Transparent)
        )
    }
}

/**
 * Profile-row descriptor, Android-app style (spec screen-2): flag +
 * lowercase descriptor and NOTHING else — "🇺🇸 http", "geolocation".
 * The descriptor is the proxy type (lowercase) when the profile has a
 * proxy; profiles without one read "geolocation" (the matched field
 * the real app shows). No host:port, no "No proxy" text.
 */
private fun ProxyConfig.specDescriptor(): String = when (mode) {
    "devxyz" -> "${Countries.flag(buyCountry)} ${type.lowercase()}".trim()
    "custom" -> if (isSet) type.lowercase() else "geolocation"
    else -> "geolocation"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileRow(
    profile: Profile,
    folders: List<Folder>,
    pinned: Boolean,
    apiToken: String? = null,
    onRunMode: (Profile, Boolean) -> Unit,
    onStop: () -> Unit,
    onEdit: () -> Unit,
    onClone: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: (Profile) -> Unit,
    onTogglePin: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var menuOpen by remember { mutableStateOf(false) }
    var showRunChoice by remember { mutableStateOf(false) }
    var showQuick by remember { mutableStateOf(false) }
    var showFolders by remember { mutableStateOf(false) }
    var launching by remember { mutableStateOf(false) }
    var pendingApi by remember { mutableStateOf(false) }

    val running = profile.status == "running"

    LaunchedEffect(launching) {
        if (launching) {
            // Capture §0: the row spins on "downloading profile..." before the session is live.
            delay(900)
            onRunMode(profile, pendingApi)
            launching = false
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    profile.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    profile.proxy.specDescriptor(),
                    style = TextStyle(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (pinned) {
                Icon(
                    Icons.Filled.PushPin, contentDescription = "Pinned",
                    modifier = Modifier.size(18.dp),
                    tint = BrandGreen
                )
                Spacer(Modifier.width(8.dp))
            }
            OsGlyph(profile.os)
            Spacer(Modifier.width(10.dp))
            if (running) {
                OutlinedButton(
                    onClick = { onRunMode(profile, false) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandGreen),
                    border = BorderStroke(1.dp, BrandGreen),
                    modifier = Modifier.height(40.dp)
                ) { Text("View") }
                Spacer(Modifier.width(6.dp))
                OutlinedButton(
                    onClick = onStop,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                    border = BorderStroke(1.dp, DangerRed),
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Stop")
                }
            } else {
                // Spec screen-2: Run is a SOLID green filled button,
                // ~76x40dp, 8dp radius, white "Run", in the right cluster.
                Button(
                    onClick = { showRunChoice = true },
                    enabled = !launching,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandGreen,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier
                        .width(76.dp)
                        .height(40.dp)
                ) {
                    Text(
                        if (launching) "Starting…" else "Run",
                        style = TextStyle(fontSize = if (launching) 13.sp else 15.sp),
                        maxLines = 1
                    )
                }
            }
            Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Profile menu")
                    }
                    // Capture §6 — exact 3-dots menu order
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                            onClick = { menuOpen = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Folders") },
                            leadingIcon = { Icon(Icons.Filled.Folder, contentDescription = null) },
                            onClick = { menuOpen = false; showFolders = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Share") },
                            enabled = false,
                            onClick = {}
                        )
                        DropdownMenuItem(
                            text = { Text("Clone") },
                            leadingIcon = { Icon(Icons.Filled.FileCopy, contentDescription = null) },
                            onClick = { menuOpen = false; onClone() }
                        )
                        DropdownMenuItem(
                            text = { Text("Transfer") },
                            enabled = false,
                            onClick = {}
                        )
                        DropdownMenuItem(
                            text = { Text("Cookies") },
                            leadingIcon = { Icon(Icons.Filled.Cookie, contentDescription = null) },
                            onClick = { menuOpen = false; showQuick = true }
                        )
                        DropdownMenuItem(
                            text = { Text("History") },
                            leadingIcon = { Icon(Icons.Filled.History, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                android.widget.Toast.makeText(
                                    context,
                                    "History lives inside the profile browser (⋮ → History)",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy \"Run\" link") },
                            leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                clipboard.setText(AnnotatedString("devxyzlogin://run/${profile.id}"))
                                android.widget.Toast.makeText(context, "Run link copied", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy ID") },
                            leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                clipboard.setText(AnnotatedString(profile.id))
                                android.widget.Toast.makeText(context, "Profile ID copied", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (pinned) "Unpin" else "Pin") },
                            leadingIcon = { Icon(Icons.Filled.PushPin, contentDescription = null) },
                            onClick = { menuOpen = false; onTogglePin() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = DangerRed) },
                            leadingIcon = {
                                Icon(Icons.Filled.Delete, contentDescription = null, tint = DangerRed)
                            },
                            onClick = { menuOpen = false; onDelete() }
                        )
                        DropdownMenuItem(
                            text = { Text("Automation") },
                            leadingIcon = { Icon(Icons.Filled.Bolt, contentDescription = null) },
                            enabled = false,
                            onClick = {}
                        )
                    }
                }
        }
    }

    // Capture §0 — Run choice sheet
    if (showRunChoice) {
        ModalBottomSheet(onDismissRequest = { showRunChoice = false }) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                RunChoiceRow(
                    title = "Manual control",
                    subtitle = "Open the profile browser on this device",
                    onClick = {
                        showRunChoice = false
                        pendingApi = false
                        launching = true
                    }
                )
                RunChoiceRow(
                    title = "API control",
                    subtitle = "Run Puppeteer against this session",
                    onClick = {
                        showRunChoice = false
                        pendingApi = true
                        launching = true
                    }
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showQuick) {
        QuickSettingsSheet(
            profile = profile,
            folders = folders,
            onDismiss = { showQuick = false },
            onUpdate = onUpdate,
            onOpenEditor = {
                showQuick = false
                onEdit()
            },
            apiToken = apiToken
        )
    }

    if (showFolders) {
        FolderPickerDialog(
            profile = profile,
            folders = folders,
            onDismiss = { showFolders = false },
            onUpdate = onUpdate
        )
    }
}

@Composable
private fun RunChoiceRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.PlayArrow, contentDescription = null, tint = BrandGreen,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FolderPickerDialog(
    profile: Profile,
    folders: List<Folder>,
    onDismiss: () -> Unit,
    onUpdate: (Profile) -> Unit
) {
    var selected by remember { mutableStateOf(profile.folderIds.toSet()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Folders") },
        text = {
            Column {
                if (folders.isEmpty()) Text("No folders yet — create one in Folders manager.")
                folders.forEach { folder ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selected = if (folder.id in selected) selected - folder.id
                                else selected + folder.id
                            }
                    ) {
                        Checkbox(
                            checked = folder.id in selected,
                            onCheckedChange = {
                                selected = if (it) selected + folder.id else selected - folder.id
                            }
                        )
                        Text(folder.name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onUpdate(profile.copy(folderIds = selected.toList(), updatedAt = ProfileStore.nowIso()))
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * Quick-settings sheet — the Android echo of GoLogin's quick-settings
 * drawer (capture §5), reached via the row's ⋮ → Cookies since v2.3.0
 * removed the row's "Settings ›" button to match the real app: header
 * card, "Profile essentials" rows, collapsible Storage / Hardware
 * sections. Edits apply on Done.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSettingsSheet(
    profile: Profile,
    folders: List<Folder>,
    onDismiss: () -> Unit,
    onUpdate: (Profile) -> Unit,
    onOpenEditor: () -> Unit,
    /** v4: session token for the REAL proxy check (null = local mode). */
    apiToken: String? = null
) {
    val api = remember { ServerApi() }
    val scope = rememberCoroutineScope()
    var checkingProxy by remember { mutableStateOf(false) }
    var checkText by remember { mutableStateOf<String?>(null) }
    var checkFailed by remember { mutableStateOf(false) }

    fun runProxyCheck() {
        if (apiToken == null) {
            checkFailed = true
            checkText = "Log in to run a real proxy check — checks run on the DevxyzLogin server"
            return
        }
        val p = profile.proxy
        checkingProxy = true
        checkText = null
        scope.launch {
            when (val r = api.checkProxy(apiToken, p.type, p.host, p.port, p.username, p.password)) {
                is ApiResult.Success -> {
                    checkingProxy = false
                    checkFailed = false
                    val info = r.data
                    checkText = "✓ ${info.ip} — ${info.place.ifBlank { "Unknown location" }} • " +
                        "${info.timezone} • ${info.latencyMs} ms"
                    var next = profile.copy(lastProxyCheck = info)
                    if (next.fingerprint.timezone.auto && info.timezone.isNotBlank()) {
                        next = next.copy(
                            fingerprint = next.fingerprint.copy(
                                timezone = next.fingerprint.timezone.copy(zone = info.timezone)
                            )
                        )
                    }
                    onUpdate(next)
                }
                is ApiResult.Failure -> {
                    checkingProxy = false
                    checkFailed = true
                    checkText = "✗ ${r.message}"
                }
            }
        }
    }

    var name by remember { mutableStateOf(profile.name) }
    var note by remember { mutableStateOf(profile.notes) }
    var folderIds by remember { mutableStateOf(profile.folderIds.toSet()) }
    var browser by remember { mutableStateOf(profile.browser) }
    var cookies by remember { mutableStateOf(profile.cookies) }
    var showCookiesEdit by remember { mutableStateOf(false) }
    var overviewOpen by remember { mutableStateOf(false) }
    var proxyLocOpen by remember { mutableStateOf(false) }
    var browserOpen by remember { mutableStateOf(false) }
    var storageOpen by remember { mutableStateOf(false) }
    var hardwareOpen by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Set up your browsing identity", style = MaterialTheme.typography.titleMedium)
            Text(
                "Configure your IP, device, and browser settings to create a consistent profile.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Profile essentials",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))

            EssentialRow("Name") {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
            EssentialRow("Note") {
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    placeholder = { Text("+ Add note") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
            EssentialRow("Folders") {
                Column {
                    if (folders.isEmpty()) {
                        Text("+ Add folder", color = BrandGreen)
                    }
                    folders.forEach { folder ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                folderIds = if (folder.id in folderIds) folderIds - folder.id
                                else folderIds + folder.id
                            }
                        ) {
                            Checkbox(
                                checked = folder.id in folderIds,
                                onCheckedChange = {
                                    folderIds = if (it) folderIds + folder.id else folderIds - folder.id
                                }
                            )
                            Text(folder.name)
                        }
                    }
                }
            }
            EssentialRow("Proxy") {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StateDot(if (profile.proxy.isSet) "running" else "ready")
                        Spacer(Modifier.width(6.dp))
                        Text(
                            (if (profile.proxy.mode == "devxyz")
                                Countries.flag(profile.proxy.buyCountry) + " " else "") +
                                profile.proxy.listLabel
                        )
                    }
                    TextButton(onClick = onOpenEditor) { Text("Choose location or proxy") }
                }
            }
            EssentialRow("Languages") {
                val fp = profile.fingerprint
                Text(
                    fp.navigator.languages.joinToString(" ") +
                        if (fp.navigator.languagesAuto) " | Based on IP" else ""
                )
            }
            EssentialRow("Timezone") {
                val tz = profile.fingerprint.timezone
                val chk = profile.lastProxyCheck
                Text(
                    when {
                        // v4: after a real check, "Based on IP" resolves to
                        // the measured zone/place instead of staying vague.
                        tz.auto && chk != null && chk.timezone.isNotBlank() ->
                            "Based on IP • ${chk.timezone}" +
                                if (chk.place.isNotBlank()) " (${chk.place})" else ""
                        tz.auto -> "Based on IP"
                        else -> tz.zone
                    }
                )
            }
            EssentialRow("Geolocation") {
                val geo = profile.fingerprint.geolocation
                val chk = profile.lastProxyCheck
                Text(
                    when {
                        geo.auto && chk != null && chk.place.isNotBlank() ->
                            "Based on IP • ${chk.place}"
                        geo.auto -> "Based on IP"
                        else -> geo.mode.replaceFirstChar { it.uppercase() } + " • ${geo.lat}, ${geo.lon}"
                    }
                )
            }
            EssentialRow("Resolution") {
                Text(profile.fingerprint.navigator.screenResolution)
            }
            EssentialRow("Cookies") {
                TextButton(onClick = { showCookiesEdit = true }) {
                    Text(if (cookies.isBlank()) "Add cookies" else "Cookies added ✓")
                }
            }
            EssentialRow("New fingerprint") {
                TextButton(onClick = {
                    onUpdate(
                        profile.copy(
                            fingerprint = FingerprintGenerator.generate(profile.os),
                            updatedAt = ProfileStore.nowIso()
                        )
                    )
                    onDismiss()
                }) { Text("↻ Refresh fingerprint", color = BrandGreen) }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            // Capture §5 collapsed sections, in order: Profile Overview /
            // Proxy & Location / Browser / Storage / Hardware.
            SectionHeader("Profile Overview", overviewOpen) { overviewOpen = !overviewOpen }
            if (overviewOpen) {
                val fp = profile.fingerprint
                EssentialRow("OS") { Text(OsOptions.label(profile.os)) }
                EssentialRow("Browser") {
                    Text("Chrome ${fp.navigator.browserVersion.substringBefore(".")}")
                }
                EssentialRow("Platform") { Text(fp.navigator.platform) }
                EssentialRow("Status") { Text(profile.status) }
                EssentialRow("Created") { Text(profile.createdAt.take(10)) }
            }
            SectionHeader("Proxy & Location", proxyLocOpen) { proxyLocOpen = !proxyLocOpen }
            if (proxyLocOpen) {
                val fp = profile.fingerprint
                val chk = profile.lastProxyCheck
                EssentialRow("Proxy") { Text(profile.proxy.listLabel) }
                if (profile.proxy.mode == "custom" && profile.proxy.isSet) {
                    EssentialRow("Check proxy") {
                        Column {
                            TextButton(
                                enabled = !checkingProxy,
                                onClick = { runProxyCheck() }
                            ) {
                                Text(
                                    if (checkingProxy) "Checking…" else "Check proxy",
                                    color = BrandGreen
                                )
                            }
                            if (checkText != null) {
                                Text(
                                    checkText!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (checkFailed) MaterialTheme.colorScheme.error else BrandGreen
                                )
                            }
                            if (apiToken == null) {
                                Text(
                                    "Checks run on the server — log in to check for real.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
                EssentialRow("Timezone") {
                    Text(
                        when {
                            fp.timezone.auto && chk != null && chk.timezone.isNotBlank() ->
                                "Based on IP • ${chk.timezone}"
                            fp.timezone.auto -> "Based on IP"
                            else -> fp.timezone.zone
                        }
                    )
                }
                EssentialRow("Geolocation") {
                    Text(
                        when {
                            fp.geolocation.auto && chk != null && chk.place.isNotBlank() ->
                                "Based on IP • ${chk.place}"
                            fp.geolocation.auto -> "Based on IP"
                            else -> "${fp.geolocation.lat}, ${fp.geolocation.lon}"
                        }
                    )
                }
                EssentialRow("WebRTC") {
                    Text(if (fp.webrtcMode == "basedOnIp") "Based on IP" else "Off")
                }
                EssentialRow("Custom DNS") {
                    Text(browser.customDns.ifBlank { "+ Add custom DNS" })
                }
            }
            SectionHeader("Browser", browserOpen) { browserOpen = !browserOpen }
            if (browserOpen) {
                val fp = profile.fingerprint
                EssentialRow("User Agent") {
                    Text(fp.navigator.userAgent, maxLines = 2)
                }
                EssentialRow("Start URL") {
                    Text(
                        CaptureData.startUrlOptions.firstOrNull { it.first == browser.startUrl }?.second
                            ?: browser.startUrl
                    )
                }
                EssentialRow("Bookmarks") {
                    Text("${DefaultBookmarks.links(profile.bookmarks).size} bookmarks")
                }
                EssentialRow("Extensions") { Text("${profile.extensions.size} extensions") }
                EssentialRow("Fonts") { Text(if (fp.fontsMasking) "Masked" else "Real") }
                EssentialRow("Launch arguments") {
                    Text(browser.launchArguments.ifBlank { "—" })
                }
            }
            SectionHeader("Storage", storageOpen) { storageOpen = !storageOpen }
            if (storageOpen) {
                // Capture §5 Storage toggle order
                SwitchRow("Save tabs", browser.saveTabs) { browser = browser.copy(saveTabs = it) }
                SwitchRow("Save history", browser.saveHistory) { browser = browser.copy(saveHistory = it) }
                SwitchRow("Save bookmarks", browser.saveBookmarks) { browser = browser.copy(saveBookmarks = it) }
                SwitchRow("Google services", browser.googleServices) { browser = browser.copy(googleServices = it) }
                SwitchRow("Disable image loading", browser.disableImageLoading) { browser = browser.copy(disableImageLoading = it) }
                SwitchRow("Disable video autoplay", browser.disableVideoAutoplay) { browser = browser.copy(disableVideoAutoplay = it) }
                SwitchRow("Save passwords", browser.savePasswords) { browser = browser.copy(savePasswords = it) }
                SwitchRow("Local storage", browser.localStorage) { browser = browser.copy(localStorage = it) }
                SwitchRow("IndexedDB", browser.indexedDb) { browser = browser.copy(indexedDb = it) }
                SwitchRow("Browser plugins", browser.vulnerablePlugins) { browser = browser.copy(vulnerablePlugins = it) }
                SwitchRow("Allow install extensions", browser.allowInstallExtensions) { browser = browser.copy(allowInstallExtensions = it) }
                SwitchRow("System Extensions", browser.systemExtensions) { browser = browser.copy(systemExtensions = it) }
            }
            SectionHeader("Hardware", hardwareOpen) { hardwareOpen = !hardwareOpen }
            if (hardwareOpen) {
                val fp = profile.fingerprint
                EssentialRow("Resolution") { Text(fp.navigator.screenResolution) }
                EssentialRow("Device memory") { Text("${fp.navigator.ram} GB") }
                EssentialRow("Threads") { Text("${fp.navigator.cpuCores} threads") }
                EssentialRow("Media Devices") {
                    Text(
                        (if (fp.mediaMasking) "Masked " else "") +
                            "Cam. ${fp.mediaDevices.videoInputs} / Mic. ${fp.mediaDevices.audioInputs} / Sp. ${fp.mediaDevices.audioOutputs}"
                    )
                }
                EssentialRow("Canvas") { Text(fp.hardware.canvasMode.replaceFirstChar { it.uppercase() }) }
                EssentialRow("WebGL Image") {
                    Text(if (fp.hardware.webglNoise) "Masked with noise" else "Off")
                }
                EssentialRow("WebGL info") { Text(fp.hardware.webglVendor) }
                EssentialRow("Audio Context") {
                    Text(if (fp.hardware.audioNoise) "Masked with noise" else "Real")
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenEditor, modifier = Modifier.weight(1f)) {
                    Text("Full editor")
                }
                androidx.compose.material3.Button(
                    onClick = {
                        onUpdate(
                            profile.copy(
                                name = name.ifBlank { profile.name },
                                notes = note,
                                folderIds = folderIds.toList(),
                                browser = browser,
                                cookies = cookies,
                                updatedAt = ProfileStore.nowIso()
                            )
                        )
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Done") }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showCookiesEdit) {
        var text by remember { mutableStateOf(cookies) }
        AlertDialog(
            onDismissRequest = { showCookiesEdit = false },
            title = { Text("Add cookies") },
            text = {
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    label = { Text("Paste cookies (JSON or name=value lines)") },
                    minLines = 4
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    cookies = text
                    showCookiesEdit = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showCookiesEdit = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun EssentialRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp)
        )
        Column(modifier = Modifier.weight(1f)) { content() }
    }
}

@Composable
private fun SectionHeader(title: String, open: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Text(if (open) "▾" else "›", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
