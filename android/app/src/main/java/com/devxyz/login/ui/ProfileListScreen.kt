package com.devxyz.login.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.devxyz.login.data.ProfileStore
import com.devxyz.login.model.FingerprintGenerator
import com.devxyz.login.model.Folder
import com.devxyz.login.model.OsOptions
import com.devxyz.login.model.Profile
import kotlinx.coroutines.delay

/**
 * Profile list (GoLogin capture parity):
 * - State dot semantics: ready = hollow grey circle, running = green dot,
 *   launching = blue dot + "downloading profile..." (capture §0/§3).
 * - Run is a green-outlined pill opening the Manual/API control choice
 *   (capture §0); running rows show View (green outline) + Stop (red outline).
 * - "Settings ›" opens the quick-settings sheet (Profile essentials, §5).
 * - Row ⋮ menu in the exact capture §6 order.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileListScreen(
    profiles: List<Profile>,
    folders: List<Folder>,
    onRunMode: (Profile, Boolean) -> Unit,
    onStop: (Profile) -> Unit,
    onEdit: (Profile) -> Unit,
    onClone: (Profile) -> Unit,
    onDelete: (Profile) -> Unit,
    onUpdate: (Profile) -> Unit,
    onNewProfile: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var folderFilter by remember { mutableStateOf<String?>(null) }
    var pinnedIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val visible = profiles.filter { p ->
        p.deletedAt == null &&
            (folderFilter == null || folderFilter in p.folderIds) &&
            (query.isBlank() || p.name.contains(query, ignoreCase = true) || p.id.contains(query, ignoreCase = true))
    }.sortedByDescending { it.id in pinnedIds }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewProfile,
                containerColor = BrandTeal,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add profile")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = folderFilter == null,
                        onClick = { folderFilter = null },
                        label = { Text("All profiles") }
                    )
                }
                items(folders) { folder ->
                    FilterChip(
                        selected = folderFilter == folder.id,
                        onClick = { folderFilter = if (folderFilter == folder.id) null else folder.id },
                        label = { Text(folder.name) }
                    )
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
                    Text(
                        "No profiles yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onNewProfile) { Text("+ Add profile") }
                }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visible, key = { it.id }) { profile ->
                    ProfileRow(
                        profile = profile,
                        folders = folders,
                        pinned = profile.id in pinnedIds,
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
            "running" -> drawCircle(color = BrandTeal)
            "launching", "syncing" -> drawCircle(color = StatusBlue)
            else -> drawCircle(color = Color(0xFF9CA3AF), style = Stroke(width = 2f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileRow(
    profile: Profile,
    folders: List<Folder>,
    pinned: Boolean,
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

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(profile.name, style = MaterialTheme.typography.titleMedium)
                        if (pinned) {
                            Icon(
                                Icons.Filled.PushPin, contentDescription = "Pinned",
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .size(14.dp),
                                tint = BrandTeal
                            )
                        }
                        TextButton(onClick = { showQuick = true }) {
                            Text("Settings ›", color = BrandTeal)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StateDot(
                            when {
                                launching -> "launching"
                                running -> "running"
                                else -> "ready"
                            }
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            when {
                                launching -> "downloading profile..."
                                running -> "running"
                                else -> "ready"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                launching -> StatusBlue
                                running -> BrandTeal
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(
                            " • ${OsOptions.label(profile.os)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "Proxy: ${profile.proxy.listLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (profile.tags.isNotEmpty()) {
                        Text(
                            profile.tags.joinToString("  ") { "#$it" },
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandTeal
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
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (running) {
                    OutlinedButton(
                        onClick = { onRunMode(profile, false) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandTeal),
                        border = BorderStroke(1.dp, BrandTeal)
                    ) { Text("View") }
                    OutlinedButton(
                        onClick = onStop,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                        border = BorderStroke(1.dp, DangerRed)
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Stop")
                    }
                } else {
                    OutlinedButton(
                        onClick = { showRunChoice = true },
                        enabled = !launching,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandTeal),
                        border = BorderStroke(1.dp, BrandTeal)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (launching) "Starting…" else "Run")
                    }
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
            }
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
            Icons.Filled.PlayArrow, contentDescription = null, tint = BrandTeal,
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
 * Quick-settings sheet — the Android echo of GoLogin's "Settings ›" drawer
 * (capture §5): header card, "Profile essentials" rows, collapsible
 * Storage / Hardware sections. Edits apply on Done.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSettingsSheet(
    profile: Profile,
    folders: List<Folder>,
    onDismiss: () -> Unit,
    onUpdate: (Profile) -> Unit,
    onOpenEditor: () -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var note by remember { mutableStateOf(profile.notes) }
    var folderIds by remember { mutableStateOf(profile.folderIds.toSet()) }
    var browser by remember { mutableStateOf(profile.browser) }
    var cookies by remember { mutableStateOf(profile.cookies) }
    var showCookiesEdit by remember { mutableStateOf(false) }
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
                        Text("+ Add folder", color = BrandTeal)
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
                        Text(profile.proxy.listLabel)
                    }
                    TextButton(onClick = onOpenEditor) { Text("Choose location or proxy") }
                }
            }
            EssentialRow("Languages") {
                val fp = profile.fingerprint
                Text(
                    fp.navigator.languages.joinToString(" ") +
                        if (fp.timezone.auto) " | Based on IP" else ""
                )
            }
            EssentialRow("Timezone") {
                val tz = profile.fingerprint.timezone
                Text(if (tz.auto) "Based on IP" else tz.zone)
            }
            EssentialRow("Geolocation") {
                val geo = profile.fingerprint.geolocation
                Text(geo.mode.replaceFirstChar { it.uppercase() } + if (geo.auto) " | Based on IP" else "")
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
                }) { Text("↻ Refresh fingerprint", color = BrandTeal) }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SectionHeader("Storage", storageOpen) { storageOpen = !storageOpen }
            if (storageOpen) {
                SwitchRow("Save history", browser.saveHistory) { browser = browser.copy(saveHistory = it) }
                SwitchRow("Save bookmarks", browser.saveBookmarks) { browser = browser.copy(saveBookmarks = it) }
                SwitchRow("Save passwords", browser.savePasswords) { browser = browser.copy(savePasswords = it) }
                SwitchRow("Enable Local Storage", browser.localStorage) { browser = browser.copy(localStorage = it) }
                SwitchRow("Enable IndexedDB", browser.indexedDb) { browser = browser.copy(indexedDb = it) }
                SwitchRow("Enable Google services", browser.googleServices) { browser = browser.copy(googleServices = it) }
                SwitchRow("Disable image loading", browser.disableImageLoading) { browser = browser.copy(disableImageLoading = it) }
                SwitchRow("Disable video autoplay", browser.disableVideoAutoplay) { browser = browser.copy(disableVideoAutoplay = it) }
            }
            SectionHeader("Hardware", hardwareOpen) { hardwareOpen = !hardwareOpen }
            if (hardwareOpen) {
                val fp = profile.fingerprint
                EssentialRow("Resolution") { Text(fp.navigator.screenResolution) }
                EssentialRow("Device memory") { Text("${fp.navigator.ram} GB") }
                EssentialRow("Threads") { Text("${fp.navigator.cpuCores} threads") }
                EssentialRow("Media Devices") {
                    Text("Cam. ${fp.mediaDevices.videoInputs} / Mic. ${fp.mediaDevices.audioInputs} / Sp. ${fp.mediaDevices.audioOutputs}")
                }
                EssentialRow("Canvas") { Text(fp.hardware.canvasMode.replaceFirstChar { it.uppercase() }) }
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
