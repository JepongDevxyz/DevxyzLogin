package com.devxyz.login.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devxyz.login.data.ApiResult
import com.devxyz.login.data.ProfileStore
import com.devxyz.login.data.ServerApi
import com.devxyz.login.model.Countries
import com.devxyz.login.model.DefaultBookmarks
import com.devxyz.login.model.FingerprintGenerator
import com.devxyz.login.model.Folder
import com.devxyz.login.model.OsOptions
import com.devxyz.login.model.Profile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Profile list — v2.4.0 rebuild against the REAL current GoLogin
 * Android app (ANDROID_UI_SPEC_V2.md, from the user's own phone
 * recording; this supersedes the v2.3.0 store-screenshot layout):
 * - Header: green rounded-square avatar tile with the account
 *   initial + workspace name + chevron (opens the account sheet),
 *   connection status text at the right. NO burger, NO search field.
 * - A folder chip button under the header opens the folder filter
 *   (All / Shared / folders) as a bottom sheet.
 * - Trial banner card: white, thin border, blue clock icon and a
 *   blue first line (our own plan state text), dismissible.
 * - Rows are FLAT on white with hairline dividers: bold name (GREEN
 *   + #F3FDF5 row tint while running), a proxy line only when a
 *   proxy is attached (dot + flag + "CC"), tag/note chips, the gray
 *   OS glyph, and the action cluster: green-outlined CIRCLE with a
 *   play triangle when stopped; eye-in-green-circle + solid RED
 *   stop square while running.
 * Behaviour preserved from 2.3.1: tap = edit, play = the Manual/API
 * run-choice sheet then the existing browser launch, stop = the
 * existing stop flow; the full row menu (clone, cookies/quick
 * settings, folders, copy id, pin, delete) moved to LONG-PRESS so
 * no feature was dropped. Pinned profiles still sort first.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileListScreen(
    profiles: List<Profile>,
    folders: List<Folder>,
    accountName: String,
    connectionText: String,
    plan: String,
    pinnedIds: Set<String>,
    onTogglePin: (Profile) -> Unit,
    onOpenAccount: () -> Unit,
    onChoosePlan: () -> Unit,
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
    // Folder filter: "all" | "shared" | a folder id
    var filter by remember { mutableStateOf("all") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var bannerDismissed by remember { mutableStateOf(false) }

    val visible = profiles.filter { p ->
        p.deletedAt == null &&
            when (filter) {
                // Profiles shared WITH you live on the account, not on this
                // device — the local store has none, so the filter is empty.
                "shared" -> false
                "all" -> true
                else -> filter in p.folderIds
            }
    }.sortedByDescending { it.id in pinnedIds }

    Column(modifier = Modifier.fillMaxSize()) {
        // ---- Header: avatar tile + workspace name + status ----
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenAccount() }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BrandGreen),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    accountName.firstOrNull()?.uppercase() ?: "D",
                    color = Color.White,
                    style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                accountName,
                style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = false)
            )
            Icon(
                Icons.Filled.ExpandMore, contentDescription = "Account",
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (connectionText == "Connected") BrandGreen else MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    connectionText,
                    style = TextStyle(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---- Folder filter chip ----
        Row(modifier = Modifier.padding(horizontal = 16.dp)) {
            val filterLabel = when (filter) {
                "all" -> null
                "shared" -> "Shared profiles"
                else -> folders.firstOrNull { it.id == filter }?.name
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .clickable { showFilterSheet = true }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(
                    Icons.Filled.Folder, contentDescription = "Filter by folder",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                if (filterLabel != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(filterLabel, style = TextStyle(fontSize = 14.sp), maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {
            if (plan == "Free" && !bannerDismissed) {
                item {
                    TrialBanner(
                        onChoosePlan = onChoosePlan,
                        onDismiss = { bannerDismissed = true }
                    )
                }
            }
            if (visible.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp, start = 16.dp, end = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (filter == "shared") {
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
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("+ Add profile") }
                        }
                    }
                }
            }
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
                    onTogglePin = { onTogglePin(profile) }
                )
            }
        }
    }

    if (showFilterSheet) {
        FolderFilterSheet(
            folders = folders,
            filter = filter,
            onSelect = {
                filter = it
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false }
        )
    }
}

/** Trial/plan banner (spec v2): white card, thin border, blue clock
 *  icon, blue first sentence, "Choose plan" link, dismiss X. Text is
 *  our own plan state, not the reference app's copy. */
@Composable
private fun TrialBanner(onChoosePlan: () -> Unit, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Filled.Schedule, contentDescription = null,
                tint = StatusBlue, modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = StatusBlue)) {
                        append("You're on the Free plan.")
                    }
                    append(" Choose a plan now to keep using DevxyzLogin without interruption.")
                },
                style = TextStyle(fontSize = 15.sp, lineHeight = 21.sp),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        TextButton(onClick = onChoosePlan) {
            Text("Choose plan", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FolderFilterSheet(
    folders: List<Folder>,
    filter: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AppBottomSheet(onDismiss = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            SheetTitle("Show profiles")
            Spacer(Modifier.height(4.dp))
            FilterRow("All profiles", filter == "all") { onSelect("all") }
            FilterRow("Shared profiles", filter == "shared") { onSelect("shared") }
            folders.forEach { folder ->
                FilterRow(folder.name, filter == folder.id) { onSelect(folder.id) }
            }
        }
    }
}

@Composable
private fun FilterRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        Text(
            label,
            style = TextStyle(fontSize = 16.sp),
            color = if (selected) BrandGreen else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (selected) Icon(Icons.Filled.Check, contentDescription = "Selected", tint = BrandGreen)
    }
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

/**
 * Second line of a row (spec v2): green/grey dot + flag + country
 * code, plus " • X.X GB" when the profile's proxy carries a real
 * server-assigned balance (proxy.dataGb — provider-declared on the
 * server, v6). The GB segment appears ONLY when dataGb != null; the
 * number is never invented. Falls back to the proxy endpoint label
 * when no country is known.
 */
private fun Profile.proxyLineText(): String {
    val chk = lastProxyCheck
    val base = when {
        chk != null && chk.countryCode.isNotBlank() ->
            "${Countries.flag(chk.countryCode)} ${chk.countryCode.uppercase()}"
        proxy.mode == "devxyz" ->
            "${Countries.flag(proxy.buyCountry)} ${proxy.buyCountry.uppercase()}"
        else -> proxy.listLabel
    }
    val gb = proxy.dataGb ?: return base
    return base + " • " + String.format(java.util.Locale.US, "%.1f", gb) + " GB"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val proxyAttached = profile.proxy.isSet || profile.proxy.mode == "devxyz"

    LaunchedEffect(launching) {
        if (launching) {
            // Capture §0: the row spins on "downloading profile..." before the session is live.
            delay(900)
            onRunMode(profile, pendingApi)
            launching = false
        }
    }

    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (running) runningTintColor() else Color.Transparent)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = onEdit,
                        onLongClick = { menuOpen = true }
                    )
                    .padding(horizontal = 16.dp, vertical = 13.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        profile.name,
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        color = if (running) BrandGreen else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (proxyAttached) {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!running) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (profile.proxy.isSet) BrandGreen
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                )
                                Spacer(Modifier.width(7.dp))
                            }
                            Text(
                                profile.proxyLineText(),
                                style = TextStyle(fontSize = 13.sp),
                                color = if (running) BrandGreen
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                    if (profile.tags.isNotEmpty() || profile.notes.isNotBlank()) {
                        Spacer(Modifier.height(7.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            profile.tags.forEach { tag -> TagChip(tag, green = true) }
                            if (profile.notes.isNotBlank()) {
                                TagChip(profile.notes, green = false)
                            }
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                OsGlyph(profile.os)
                Spacer(Modifier.width(12.dp))
                if (running) {
                    // Eye in a green-outlined circle = open the running browser
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, BrandGreen, CircleShape)
                            .clickable { onRunMode(profile, false) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Visibility, contentDescription = "View running profile",
                            tint = BrandGreen, modifier = Modifier.size(23.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    // Solid RED rounded-square Stop with a white square glyph
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DangerRed)
                            .clickable { onStop() },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White)
                        )
                    }
                } else {
                    // Green-outlined CIRCLE with a play triangle (~46dp)
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, BrandGreen, CircleShape)
                            .clickable(enabled = !launching) { showRunChoice = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (launching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = BrandGreen
                            )
                        } else {
                            Icon(
                                Icons.Filled.PlayArrow, contentDescription = "Run profile",
                                tint = BrandGreen, modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        // Full row menu on long-press — the exact capture §6 order,
        // unchanged from the previous builds (no feature dropped).
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

    // Capture §0 — Run choice sheet
    if (showRunChoice) {
        AppBottomSheet(onDismiss = { showRunChoice = false }) {
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
        FoldersSheet(
            folders = folders,
            selectedIds = profile.folderIds.toSet(),
            onToggle = { folder ->
                val next = if (folder.id in profile.folderIds) profile.folderIds - folder.id
                else profile.folderIds + folder.id
                onUpdate(profile.copy(folderIds = next, updatedAt = ProfileStore.nowIso()))
            },
            onCreateFolder = null,
            onDismiss = { showFolders = false }
        )
    }
}

/** Tag chip (spec v2): pale pill — green family for tags, gray for notes. */
@Composable
private fun TagChip(text: String, green: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (green) tagGreenBgColor() else tagGrayBgColor())
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            style = TextStyle(fontSize = 12.sp),
            color = if (green) tagGreenTextColor()
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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

/**
 * Quick-settings sheet — the Android echo of GoLogin's quick-settings
 * drawer (capture §5), reached via the row's long-press menu →
 * Cookies: header card, "Profile essentials" rows, collapsible
 * Storage / Hardware sections. Edits apply on Done. Content and
 * behaviour unchanged in v2.4.0; only the sheet shell follows the
 * new spec (20dp top corners).
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

    AppBottomSheet(onDismiss = onDismiss) {
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
                            androidx.compose.material3.Checkbox(
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
                        com.devxyz.login.model.CaptureData.startUrlOptions
                            .firstOrNull { it.first == browser.startUrl }?.second
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
        androidx.compose.material3.AlertDialog(
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
