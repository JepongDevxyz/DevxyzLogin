package com.devxyz.login.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PushPin as OutlinedPushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devxyz.login.data.ProfileStore
import com.devxyz.login.model.CaptureData
import com.devxyz.login.model.DefaultBookmarks
import com.devxyz.login.model.FingerprintGenerator
import com.devxyz.login.model.Folder
import com.devxyz.login.model.OsOptions
import com.devxyz.login.model.Profile
import com.devxyz.login.model.ProxyConfig
import com.devxyz.login.model.SharedProxy

/**
 * Create/Edit profile — v2.4.0 rebuild against the REAL current
 * GoLogin Android app (ANDROID_UI_SPEC_V2.md): back arrow + profile
 * name title, pin + share icons top-right (edit only); outlined-box
 * fields Name (X clear) / Proxy (flag + name, X clear) / Folder
 * (chevron); full-width section ROWS with hairline dividers —
 * Profile Overview, Proxy & Location, Browser, Storage, Hardware
 * (+ History in edit) — each opening a full screen that holds the
 * SAME fields and handlers the v2.3.0 accordions had; red
 * "Delete profile" (edit only, custom confirm sheet); bottom
 * solid-green "Save" / "Add profile".
 *
 * The Proxy field opens the new Add proxy flow (AddProxyScreen);
 * a confirmed proxy keeps the v2.3.x behaviour of storing the
 * measured check on the profile and resolving an auto timezone.
 * OS stays locked after creation, as before.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    initial: Profile,
    isNew: Boolean,
    folders: List<Folder>,
    savedProxies: List<SharedProxy>,
    onSave: (Profile) -> Unit,
    onBack: () -> Unit,
    /** v2.4.0: delete flow (MainActivity owns the store write). */
    onDelete: (Profile) -> Unit = {},
    /** v2.4.0: pin state lives with the list (MainActivity). */
    pinned: Boolean = false,
    onTogglePin: () -> Unit = {},
    /** v2.4.0: folder creation from the Folders sheet, when the host can write the store. */
    onCreateFolder: ((String) -> Unit)? = null,
    /** v2.4.0: the Add proxy flow's "Buy proxy" action. */
    onBuyProxy: () -> Unit = {},
    /** v4: live server session for the REAL proxy check (null = local mode). */
    apiToken: String? = null,
    apiWorkspaceId: String? = null
) {
    val context = LocalContext.current
    var profile by remember { mutableStateOf(initial) }
    var section by remember { mutableStateOf<String?>(null) }
    var showAddProxy by remember { mutableStateOf(false) }
    var showFolders by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    fun applyProxy(cfg: ProxyConfig, info: com.devxyz.login.model.ProxyCheckInfo?) {
        var next = profile.copy(proxy = cfg)
        if (info != null) {
            // Same rule as the old editor's applyCheck: store the
            // measured result and resolve an auto timezone from it.
            next = next.copy(lastProxyCheck = info)
            if (next.fingerprint.timezone.auto && info.timezone.isNotBlank()) {
                next = next.copy(
                    fingerprint = next.fingerprint.copy(
                        timezone = next.fingerprint.timezone.copy(zone = info.timezone)
                    )
                )
            }
        }
        profile = next
    }

    fun save() {
        onSave(profile.copy(updatedAt = ProfileStore.nowIso()))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (section) {
            null -> EditorHome(
                profile = profile,
                isNew = isNew,
                folders = folders,
                pinned = pinned,
                onTogglePin = onTogglePin,
                onBack = onBack,
                onChange = { profile = it },
                onOpenSection = { section = it },
                onConnectProxy = { showAddProxy = true },
                onOpenFolders = { showFolders = true },
                onDeleteRequest = { showDeleteConfirm = true },
                onSave = { save() }
            )
            else -> {
                val key = section!!
                val title = when (key) {
                    "overview" -> "Profile Overview"
                    "proxy" -> "Proxy & Location"
                    "browser" -> "Browser"
                    "storage" -> "Storage"
                    "hardware" -> "Hardware"
                    else -> "History"
                }
                Column(modifier = Modifier.fillMaxSize()) {
                    BackHeader(title = title, onBack = { section = null })
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Spacer(Modifier.height(2.dp))
                        when (key) {
                            "overview" -> OverviewContent(profile, isNew) { profile = it }
                            "proxy" -> ProxyLocationContent(
                                profile = profile,
                                onChange = { profile = it },
                                onConnectProxy = { showAddProxy = true }
                            )
                            "browser" -> BrowserContent(profile, isNew) { profile = it }
                            "storage" -> StorageContent(profile) { profile = it }
                            "hardware" -> HardwareContent(profile) { profile = it }
                            else -> HistoryContent(profile) { profile = it }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    Surface(color = MaterialTheme.colorScheme.surface) {
                        GreenButton(text = "Save", onClick = { section = null })
                    }
                }
            }
        }

        if (showAddProxy) {
            AddProxyScreen(
                savedProxies = savedProxies,
                apiToken = apiToken,
                apiWorkspaceId = apiWorkspaceId,
                onBuyProxy = onBuyProxy,
                onConfirm = { cfg, info ->
                    showAddProxy = false
                    applyProxy(cfg, info)
                },
                onBack = { showAddProxy = false }
            )
        }
    }

    if (showFolders) {
        FoldersSheet(
            folders = folders,
            selectedIds = profile.folderIds.toSet(),
            onToggle = { folder ->
                profile = profile.copy(
                    folderIds = if (folder.id in profile.folderIds) profile.folderIds - folder.id
                    else profile.folderIds + folder.id
                )
            },
            onCreateFolder = onCreateFolder,
            onDismiss = { showFolders = false }
        )
    }

    if (showDeleteConfirm) {
        ConfirmSheet(
            title = "Delete profile",
            message = "Move \"${profile.name}\" to trash? You can restore it in Settings → Restore profiles.",
            confirmLabel = "Delete profile",
            onConfirm = {
                showDeleteConfirm = false
                onDelete(profile)
                onBack()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
private fun EditorHome(
    profile: Profile,
    isNew: Boolean,
    folders: List<Folder>,
    pinned: Boolean,
    onTogglePin: () -> Unit,
    onBack: () -> Unit,
    onChange: (Profile) -> Unit,
    onOpenSection: (String) -> Unit,
    onConnectProxy: () -> Unit,
    onOpenFolders: () -> Unit,
    onDeleteRequest: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize()) {
        BackHeader(
            title = if (isNew) "Add profile" else profile.name.ifBlank { "Profile" },
            onBack = onBack,
            actions = {
                if (!isNew) {
                    IconButton(onClick = onTogglePin) {
                        Icon(
                            if (pinned) Icons.Filled.PushPin else OutlinedPushPin,
                            contentDescription = if (pinned) "Unpin profile" else "Pin profile",
                            tint = if (pinned) BrandGreen
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        val summary = profile.name + " — " +
                            proxyFieldLabel(profile).ifBlank { "No proxy" } +
                            " • " + OsOptions.label(profile.os)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, summary)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share profile"))
                    }) {
                        Icon(
                            Icons.Filled.Share, contentDescription = "Share profile",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        )
        // One scroll column: fields, then the section rows directly
        // under them (spec frame), Delete below the rows; the Save
        // button stays pinned under it.
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(Modifier.height(2.dp))
                SpecTextField(
                    label = "Name",
                    value = profile.name,
                    onValueChange = { onChange(profile.copy(name = it)) },
                    clearable = true,
                    modifier = Modifier.fillMaxWidth()
                )
                val proxyLabel = proxyFieldLabel(profile)
                SpecValueField(
                    label = "Proxy",
                    value = proxyLabel,
                    placeholder = "Connect proxy",
                    trailing = {
                        if (proxyLabel.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    onChange(
                                        profile.copy(
                                            proxy = ProxyConfig(mode = "none"),
                                            lastProxyCheck = null
                                        )
                                    )
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove proxy",
                                    modifier = Modifier.size(18.dp))
                            }
                        } else {
                            Icon(
                                Icons.Filled.ExpandMore, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = onConnectProxy
                )
                SpecValueField(
                    label = "Folder",
                    value = folders.filter { it.id in profile.folderIds }
                        .joinToString(", ") { it.name },
                    placeholder = "Add to folder",
                    trailing = {
                        Icon(
                            Icons.Filled.ExpandMore, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = onOpenFolders
                )
            }
            Spacer(Modifier.height(14.dp))
            // Section rows (hairline dividers, chevrons) — full-width.
            SectionRow("Profile Overview") { onOpenSection("overview") }
            SectionRow("Proxy & Location") { onOpenSection("proxy") }
            SectionRow("Browser") { onOpenSection("browser") }
            SectionRow("Storage") { onOpenSection("storage") }
            SectionRow("Hardware") { onOpenSection("hardware") }
            if (!isNew) SectionRow("History") { onOpenSection("history") }
            if (!isNew) {
                TextButton(
                    onClick = onDeleteRequest,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        "Delete profile",
                        color = DangerRed,
                        style = TextStyle(fontSize = 16.sp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Surface(color = MaterialTheme.colorScheme.surface) {
            GreenButton(
                text = if (isNew) "Add profile" else "Save",
                onClick = onSave
            )
        }
    }
}

/** Profile Overview section — the old "Operating system" accordion
 *  body: OS picker (locked after creation), fingerprint refresh,
 *  tags and notes. Folder membership moved to the Folder field. */
@Composable
private fun OverviewContent(
    profile: Profile,
    isNew: Boolean,
    onChange: (Profile) -> Unit
) {
    Text(
        "Operating System${if (!isNew) " (locked after creation)" else ""}",
        style = MaterialTheme.typography.bodySmall
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        OsOptions.all.forEach { (value, label) ->
            FilterChip(
                selected = profile.os == value,
                enabled = isNew,
                onClick = {
                    onChange(
                        profile.copy(
                            os = value,
                            fingerprint = FingerprintGenerator.generate(value)
                        )
                    )
                },
                label = { Text(label) }
            )
        }
    }
    OutlinedButton(onClick = {
        onChange(profile.copy(fingerprint = FingerprintGenerator.generate(profile.os)))
    }) {
        Icon(Icons.Filled.Refresh, contentDescription = null)
        Spacer(Modifier.padding(4.dp))
        Text("New Fingerprint")
    }
    LabeledTextField(
        label = "Tags (comma separated)",
        value = profile.tags.joinToString(", "),
        onValueChange = { text ->
            onChange(
                profile.copy(
                    tags = text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                )
            )
        },
        modifier = Modifier.fillMaxWidth(), singleLine = true
    )
    LabeledTextField(
        label = "Notes",
        value = profile.notes,
        onValueChange = { onChange(profile.copy(notes = it)) },
        modifier = Modifier.fillMaxWidth(), minLines = 2
    )
}

/**
 * Proxy & Location section (spec v2 frame): outlined fields Proxy /
 * Timezone ("Fill based on IP") / Geolocation / WebRTC / Custom DNS,
 * wired to the existing profile fields. Timezone, Geolocation and
 * WebRTC open small sheets holding the same controls the old
 * accordions had; Custom DNS edits in place.
 */
@Composable
private fun ProxyLocationContent(
    profile: Profile,
    onChange: (Profile) -> Unit,
    onConnectProxy: () -> Unit
) {
    val fp = profile.fingerprint
    var showTz by remember { mutableStateOf(false) }
    var showGeo by remember { mutableStateOf(false) }
    var showWebRtc by remember { mutableStateOf(false) }

    val proxyLabel = proxyFieldLabel(profile)
    SpecValueField(
        label = "Proxy",
        value = proxyLabel,
        placeholder = "Connect proxy",
        trailing = {
            if (proxyLabel.isNotBlank()) {
                IconButton(
                    onClick = {
                        onChange(
                            profile.copy(
                                proxy = ProxyConfig(mode = "none"),
                                lastProxyCheck = null
                            )
                        )
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove proxy",
                        modifier = Modifier.size(18.dp))
                }
            }
        },
        onClick = onConnectProxy,
        modifier = Modifier.fillMaxWidth()
    )
    SpecValueField(
        label = "Timezone",
        value = if (fp.timezone.auto) "Fill based on IP" else fp.timezone.zone,
        onClick = { showTz = true },
        modifier = Modifier.fillMaxWidth()
    )
    SpecValueField(
        label = "Geolocation",
        value = when {
            fp.geolocation.auto -> "Geolocation based on IP"
            else -> fp.geolocation.mode.replaceFirstChar { it.uppercase() } +
                " • ${fp.geolocation.lat}, ${fp.geolocation.lon}"
        },
        trailing = {
            Icon(
                Icons.Filled.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        onClick = { showGeo = true },
        modifier = Modifier.fillMaxWidth()
    )
    SpecValueField(
        label = "WebRTC",
        value = if (fp.webrtcMode == "basedOnIp") "Based on ip" else "Off",
        trailing = {
            Icon(
                Icons.Filled.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        onClick = { showWebRtc = true },
        modifier = Modifier.fillMaxWidth()
    )
    SpecTextField(
        label = "Custom DNS",
        value = profile.browser.customDns,
        onValueChange = {
            onChange(profile.copy(browser = profile.browser.copy(customDns = it)))
        },
        placeholder = "Based on ip",
        modifier = Modifier.fillMaxWidth()
    )

    if (showTz) {
        AppBottomSheet(onDismiss = { showTz = false }) {
            Column(modifier = Modifier.padding(bottom = 10.dp)) {
                SheetTitle("Timezone")
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SwitchRow("Fill based on IP", fp.timezone.auto) { on ->
                        onChange(
                            profile.copy(
                                fingerprint = fp.copy(timezone = fp.timezone.copy(auto = on))
                            )
                        )
                    }
                    if (!fp.timezone.auto) {
                        SpecTextField(
                            label = "Timezone (IANA, e.g. Asia/Manila)",
                            value = fp.timezone.zone,
                            onValueChange = {
                                onChange(
                                    profile.copy(
                                        fingerprint = fp.copy(
                                            timezone = fp.timezone.copy(zone = it)
                                        )
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                GreenButton(text = "Done", onClick = { showTz = false })
            }
        }
    }

    if (showGeo) {
        AppBottomSheet(onDismiss = { showGeo = false }) {
            Column(modifier = Modifier.padding(bottom = 10.dp)) {
                SheetTitle("Geolocation")
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RadioChips(
                        options = listOf("prompt" to "Prompt", "allow" to "Allow", "block" to "Block"),
                        value = fp.geolocation.mode,
                        onSelect = {
                            onChange(
                                profile.copy(
                                    fingerprint = fp.copy(
                                        geolocation = fp.geolocation.copy(mode = it)
                                    )
                                )
                            )
                        }
                    )
                    SwitchRow("Fill based on IP", fp.geolocation.auto) { on ->
                        onChange(
                            profile.copy(
                                fingerprint = fp.copy(
                                    geolocation = fp.geolocation.copy(auto = on)
                                )
                            )
                        )
                    }
                    if (!fp.geolocation.auto) {
                        SpecTextField(
                            label = "Latitude",
                            value = fp.geolocation.lat.toString(),
                            onValueChange = { v ->
                                onChange(
                                    profile.copy(
                                        fingerprint = fp.copy(
                                            geolocation = fp.geolocation.copy(
                                                lat = v.toDoubleOrNull() ?: 0.0
                                            )
                                        )
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        SpecTextField(
                            label = "Longitude",
                            value = fp.geolocation.lon.toString(),
                            onValueChange = { v ->
                                onChange(
                                    profile.copy(
                                        fingerprint = fp.copy(
                                            geolocation = fp.geolocation.copy(
                                                lon = v.toDoubleOrNull() ?: 0.0
                                            )
                                        )
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                GreenButton(text = "Done", onClick = { showGeo = false })
            }
        }
    }

    if (showWebRtc) {
        AppBottomSheet(onDismiss = { showWebRtc = false }) {
            Column(modifier = Modifier.padding(bottom = 10.dp)) {
                SheetTitle("WebRTC")
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    RadioChips(
                        options = listOf("basedOnIp" to "Based on IP", "off" to "Off"),
                        value = if (fp.webrtcMode == "basedOnIp") "basedOnIp" else "off",
                        onSelect = {
                            onChange(profile.copy(fingerprint = fp.copy(webrtcMode = it)))
                        }
                    )
                }
                GreenButton(text = "Done", onClick = { showWebRtc = false })
            }
        }
    }
}

/**
 * Browser section — the old Advanced (Navigator / Fonts / Media
 * devices / Other) + Extensions + Bookmarks + Cookies accordion
 * bodies, unchanged fields and handlers.
 */
@Composable
private fun BrowserContent(
    profile: Profile,
    isNew: Boolean,
    onChange: (Profile) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val fp = profile.fingerprint
    val nav = fp.navigator
    val browser = profile.browser
    var showCookiesImport by remember { mutableStateOf(false) }

    SectionTitle("Navigator")
    // White box with a GREEN copy icon at the right edge (spec v1/v2).
    FieldLabel("User-agent")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    ) {
        OutlinedTextField(
            value = nav.userAgent,
            onValueChange = { onChange(profile.copy(fingerprint = fp.copy(navigator = nav.copy(userAgent = it)))) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            shape = RoundedCornerShape(10.dp),
            colors = fieldBoxColors(),
            trailingIcon = {
                IconButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(nav.userAgent))
                        android.widget.Toast.makeText(context, "User-Agent copied", android.widget.Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        Icons.Filled.ContentCopy, contentDescription = "Copy User-Agent",
                        tint = BrandGreen, modifier = Modifier.size(20.dp)
                    )
                }
            }
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            DropdownField(
                label = "Browser version",
                value = nav.browserVersion.substringBefore("."),
                options = CaptureData.browserVersionMajors.map {
                    it to CaptureData.browserVersionLabel(it)
                },
                onSelect = { major ->
                    val full = if (major == "156") "156.0.8078.47" else "$major.0.0.0"
                    onChange(
                        profile.copy(
                            fingerprint = fp.copy(
                                navigator = nav.copy(
                                    userAgent = CaptureData.uaFor(profile.os, major, full),
                                    browserVersion = full
                                )
                            )
                        )
                    )
                }
            )
        }
        IconButton(onClick = {
            onChange(profile.copy(fingerprint = FingerprintGenerator.generate(profile.os)))
        }) { Icon(Icons.Filled.Refresh, contentDescription = "Regenerate fingerprint") }
    }
    DropdownField(
        label = "Screen Resolution",
        value = nav.screenResolution,
        options = (CaptureData.screenResolutions + nav.screenResolution)
            .distinct().map { it to it },
        onSelect = { onChange(profile.copy(fingerprint = fp.copy(navigator = nav.copy(screenResolution = it)))) }
    )
    if (nav.languagesAuto) {
        // Capture §4.7: with "Based on IP" ON the Languages box reads
        // "Automatic" and is disabled.
        LabeledTextField(
            label = "Languages",
            value = "Automatic",
            onValueChange = {},
            readOnly = true,
            enabled = false,
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
    } else {
        LabeledTextField(
            label = "Languages (comma separated)",
            value = nav.languages.joinToString(", "),
            onValueChange = { text ->
                onChange(
                    profile.copy(
                        fingerprint = fp.copy(
                            navigator = nav.copy(
                                languages = text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            )
                        )
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
    }
    SwitchRow("Based on IP", nav.languagesAuto) { on ->
        onChange(profile.copy(fingerprint = fp.copy(navigator = nav.copy(languagesAuto = on))))
    }
    LabeledTextField(
        label = "Platform",
        value = nav.platform,
        onValueChange = { onChange(profile.copy(fingerprint = fp.copy(navigator = nav.copy(platform = it)))) },
        modifier = Modifier.fillMaxWidth(), singleLine = true
    )
    DropdownField(
        label = "CPU Threads",
        value = nav.cpuCores.toString(),
        options = CaptureData.cpuOptions.map { it.toString() to it.toString() },
        onSelect = { onChange(profile.copy(fingerprint = fp.copy(navigator = nav.copy(cpuCores = it.toInt())))) }
    )
    DropdownField(
        label = "RAM",
        value = nav.ram.toString(),
        options = CaptureData.ramOptions.map { it.toString() to "$it GB" },
        onSelect = { onChange(profile.copy(fingerprint = fp.copy(navigator = nav.copy(ram = it.toInt())))) }
    )
    SwitchRow("DoNotTrack", nav.doNotTrack) { on ->
        onChange(profile.copy(fingerprint = fp.copy(navigator = nav.copy(doNotTrack = on))))
    }

    SectionTitle("Fonts")
    SwitchRow("Enable fonts list masking", fp.fontsMasking) { on ->
        onChange(profile.copy(fingerprint = fp.copy(fontsMasking = on)))
    }

    SectionTitle("Media devices")
    SwitchRow("Mask Media Devices", fp.mediaMasking) { on ->
        onChange(profile.copy(fingerprint = fp.copy(mediaMasking = on)))
    }
    StepperField("Video Inputs", fp.mediaDevices.videoInputs) { v ->
        onChange(profile.copy(fingerprint = fp.copy(mediaDevices = fp.mediaDevices.copy(videoInputs = v))))
    }
    StepperField("Audio Inputs", fp.mediaDevices.audioInputs) { v ->
        onChange(profile.copy(fingerprint = fp.copy(mediaDevices = fp.mediaDevices.copy(audioInputs = v))))
    }
    StepperField("Audio Outputs", fp.mediaDevices.audioOutputs) { v ->
        onChange(profile.copy(fingerprint = fp.copy(mediaDevices = fp.mediaDevices.copy(audioOutputs = v))))
    }

    SectionTitle("Other")
    SwitchRow("Enable Google services", browser.googleServices) { on ->
        onChange(profile.copy(browser = browser.copy(googleServices = on)))
    }
    SwitchRow("Disable image loading", browser.disableImageLoading) { on ->
        onChange(profile.copy(browser = browser.copy(disableImageLoading = on)))
    }
    SwitchRow("Disable video autoplay", browser.disableVideoAutoplay) { on ->
        onChange(profile.copy(browser = browser.copy(disableVideoAutoplay = on)))
    }
    DropdownField(
        label = "Start URL",
        value = browser.startUrl,
        options = CaptureData.startUrlOptions,
        onSelect = { onChange(profile.copy(browser = browser.copy(startUrl = it))) }
    )
    if (browser.startUrl == "specified") {
        var pagesText by remember {
            mutableStateOf(profile.browser.startPages.joinToString("\n"))
        }
        LabeledTextField(
            label = "Specified pages (one URL per line)",
            value = pagesText,
            onValueChange = {
                pagesText = it
                onChange(
                    profile.copy(
                        browser = profile.browser.copy(
                            startPages = it.lines().map { l -> l.trim() }.filter { l -> l.isNotEmpty() }
                        )
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(), minLines = 2
        )
    }
    LabeledTextField(
        label = "Launch arguments",
        value = browser.launchArguments,
        onValueChange = { onChange(profile.copy(browser = browser.copy(launchArguments = it))) },
        placeholder = "Example: --disable-gpu --start-maximized",
        modifier = Modifier.fillMaxWidth(), minLines = 2
    )

    SectionTitle("Extensions")
    profile.extensions.forEachIndexed { index, ext ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(ext, modifier = Modifier.weight(1f))
            IconButton(onClick = {
                onChange(profile.copy(extensions = profile.extensions.filterIndexed { i, _ -> i != index }))
            }) { Icon(Icons.Filled.Delete, contentDescription = "Remove extension") }
        }
    }
    var newExt by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically) {
        LabeledTextField(
            label = "Extension name / ID",
            value = newExt,
            onValueChange = { newExt = it },
            modifier = Modifier.weight(1f), singleLine = true
        )
        IconButton(onClick = {
            if (newExt.isNotBlank()) {
                onChange(profile.copy(extensions = profile.extensions + newExt.trim()))
                newExt = ""
            }
        }) { Icon(Icons.Filled.Add, contentDescription = "Add extension") }
    }
    OutlinedButton(onClick = {
        if (newExt.isNotBlank()) {
            onChange(profile.copy(extensions = profile.extensions + newExt.trim()))
            newExt = ""
        }
    }) { Text("Add extensions") }
    Text(
        "Note: WebView cannot install Chromium extensions; this list is stored with the profile (GoLogin parity) for the future engine.",
        style = MaterialTheme.typography.bodySmall
    )

    SectionTitle("Bookmarks")
    var bookmarksText by remember {
        mutableStateOf(
            profile.bookmarks.joinToString("\n").ifBlank {
                if (isNew) DefaultBookmarks.editorText else ""
            }
        )
    }
    LabeledTextField(
        label = "Edit Bookmarks — one per line: folder::name::url",
        value = bookmarksText,
        onValueChange = {
            bookmarksText = it
            onChange(
                profile.copy(
                    bookmarks = it.lines().map { l -> l.trimEnd() }
                        .filter { l -> l.isNotBlank() }
                )
            )
        },
        modifier = Modifier.fillMaxWidth(), minLines = 6
    )

    SectionTitle("Cookies")
    OutlinedButton(onClick = { showCookiesImport = true }) {
        Text("Import Cookies")
    }
    if (profile.cookies.isNotBlank()) {
        Text(
            "Cookies stored with this profile (${profile.cookies.lines().count { it.isNotBlank() }} lines).",
            style = MaterialTheme.typography.bodySmall
        )
    }

    if (showCookiesImport) {
        var text by remember { mutableStateOf(profile.cookies) }
        AlertDialog(
            onDismissRequest = { showCookiesImport = false },
            title = { Text("Import Cookies") },
            text = {
                LabeledTextField(
                    label = "Paste cookies (JSON or name=value lines)",
                    value = text, onValueChange = { text = it },
                    minLines = 5
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onChange(profile.copy(cookies = text))
                    showCookiesImport = false
                }) { Text("Import") }
            },
            dismissButton = { TextButton(onClick = { showCookiesImport = false }) { Text("Cancel") } }
        )
    }
}

/** Storage section — the old Advanced "Storage" switches plus the
 *  quick-settings storage toggles, each bound to the same
 *  BrowserOptions fields as before. */
@Composable
private fun StorageContent(
    profile: Profile,
    onChange: (Profile) -> Unit
) {
    val browser = profile.browser
    SwitchRow("Save tabs", browser.saveTabs) { on ->
        onChange(profile.copy(browser = browser.copy(saveTabs = on)))
    }
    SwitchRow("Save history", browser.saveHistory) { on ->
        onChange(profile.copy(browser = browser.copy(saveHistory = on)))
    }
    SwitchRow("Save bookmarks", browser.saveBookmarks) { on ->
        onChange(profile.copy(browser = browser.copy(saveBookmarks = on)))
    }
    SwitchRow("Save passwords", browser.savePasswords) { on ->
        onChange(profile.copy(browser = browser.copy(savePasswords = on)))
    }
    SwitchRow("Enable Local Storage", browser.localStorage) { on ->
        onChange(profile.copy(browser = browser.copy(localStorage = on)))
    }
    SwitchRow("Enable Extension storage", browser.extensionStorage) { on ->
        onChange(profile.copy(browser = browser.copy(extensionStorage = on)))
    }
    SwitchRow("Enable IndexedDB", browser.indexedDb) { on ->
        onChange(profile.copy(browser = browser.copy(indexedDb = on)))
    }
    SwitchRow("Enable potentially vulnerable plugins", browser.vulnerablePlugins) { on ->
        onChange(profile.copy(browser = browser.copy(vulnerablePlugins = on)))
    }
    SwitchRow("Allow install extensions", browser.allowInstallExtensions) { on ->
        onChange(profile.copy(browser = browser.copy(allowInstallExtensions = on)))
    }
    SwitchRow("Enable System Extensions", browser.systemExtensions) { on ->
        onChange(profile.copy(browser = browser.copy(systemExtensions = on)))
    }
}

/** Hardware section — the old Advanced "Hardware" body, unchanged. */
@Composable
private fun HardwareContent(
    profile: Profile,
    onChange: (Profile) -> Unit
) {
    val fp = profile.fingerprint
    val hw = fp.hardware
    Text("Canvas", style = MaterialTheme.typography.bodySmall)
    RadioChips(
        options = listOf("noise" to "Noise", "off" to "Off", "block" to "Block"),
        value = hw.canvasMode,
        onSelect = { onChange(profile.copy(fingerprint = fp.copy(hardware = hw.copy(canvasMode = it)))) }
    )
    Text("Client Rects", style = MaterialTheme.typography.bodySmall)
    RadioChips(
        options = listOf("noise" to "Noise", "off" to "Off"),
        value = if (hw.clientRectsNoise) "noise" else "off",
        onSelect = { onChange(profile.copy(fingerprint = fp.copy(hardware = hw.copy(clientRectsNoise = it == "noise")))) }
    )
    Text("Audio Context", style = MaterialTheme.typography.bodySmall)
    RadioChips(
        options = listOf("noise" to "Noise", "off" to "Off"),
        value = if (hw.audioNoise) "noise" else "off",
        onSelect = { onChange(profile.copy(fingerprint = fp.copy(hardware = hw.copy(audioNoise = it == "noise")))) }
    )
    Text("WebGL Image", style = MaterialTheme.typography.bodySmall)
    RadioChips(
        options = listOf("noise" to "Noise", "off" to "Off"),
        value = if (hw.webglNoise) "noise" else "off",
        onSelect = { onChange(profile.copy(fingerprint = fp.copy(hardware = hw.copy(webglNoise = it == "noise")))) }
    )
    Text("WebGL Metadata", style = MaterialTheme.typography.bodySmall)
    RadioChips(
        options = listOf("mask" to "Mask", "real" to "Real"),
        value = if (hw.webglMetadataMask) "mask" else "real",
        onSelect = { onChange(profile.copy(fingerprint = fp.copy(hardware = hw.copy(webglMetadataMask = it == "mask")))) }
    )
    DropdownField(
        label = "WebGL Vendor",
        value = hw.webglVendor,
        options = (CaptureData.webglVendors + hw.webglVendor)
            .filter { it.isNotBlank() }.distinct().map { it to it },
        onSelect = { vendor ->
            val renderers = CaptureData.webglRenderersFor(vendor)
            onChange(
                profile.copy(
                    fingerprint = fp.copy(
                        hardware = hw.copy(
                            webglVendor = vendor,
                            webglRenderer = if (hw.webglRenderer in renderers) hw.webglRenderer
                            else renderers.firstOrNull() ?: hw.webglRenderer
                        )
                    )
                )
            )
        }
    )
    DropdownField(
        label = "WebGL Renderer",
        value = hw.webglRenderer,
        options = (CaptureData.webglRenderersFor(hw.webglVendor) + hw.webglRenderer)
            .filter { it.isNotBlank() }.distinct().map { it to it },
        onSelect = { onChange(profile.copy(fingerprint = fp.copy(hardware = hw.copy(webglRenderer = it)))) }
    )
}

/**
 * History section (edit only). Browsing history itself lives inside
 * the profile's browser (Orbita parity) — this screen carries the
 * one history control the profile owns (Save history) plus the
 * profile's timestamps.
 */
@Composable
private fun HistoryContent(
    profile: Profile,
    onChange: (Profile) -> Unit
) {
    Text(
        "History is recorded inside the profile browser while the profile runs (open it from the browser's ⋮ menu).",
        style = MaterialTheme.typography.bodyMedium
    )
    SwitchRow("Save history", profile.browser.saveHistory) { on ->
        onChange(profile.copy(browser = profile.browser.copy(saveHistory = on)))
    }
    Text("Status: ${profile.status}", style = MaterialTheme.typography.bodySmall)
    if (profile.createdAt.isNotBlank()) {
        Text("Created: ${profile.createdAt.take(10)}", style = MaterialTheme.typography.bodySmall)
    }
    if (profile.updatedAt.isNotBlank()) {
        Text("Last updated: ${profile.updatedAt.take(10)}", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun SectionTitle(text: String) {
    // Sub-headers inside a section screen ("Navigator") are bold, ~15sp.
    Text(
        text,
        style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

/**
 * Small gray field label rendered ABOVE its field — the field pattern
 * of the real GoLogin Android editor ("User-agent",
 * "Screen resolution"). Replaces M3 floating labels in the editor.
 */
@Composable
fun FieldLabel(text: String) {
    Text(
        text,
        style = TextStyle(fontSize = 12.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

/**
 * Colors for the editor's white-box fields: surface-colored fill,
 * thin light border, primary-green focus border, 10dp radius is set
 * at each field's `shape`.
 */
@Composable
fun fieldBoxColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surface,
    errorContainerColor = MaterialTheme.colorScheme.surface,
    focusedBorderColor = BrandGreen,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    disabledBorderColor = MaterialTheme.colorScheme.outline
)

/**
 * Label-above text field: small gray [label] above a white rounded
 * (10dp) box — no floating label. Used inside the section screens
 * (and the settings screens in MiscScreens).
 */
@Composable
fun LabeledTextField(
    label: String?,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minLines: Int = 1,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    placeholder: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column(modifier = modifier.padding(bottom = 10.dp)) {
        if (label != null) FieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            minLines = minLines,
            readOnly = readOnly,
            enabled = enabled,
            placeholder = placeholder?.let { text -> @Composable { Text(text) } },
            keyboardOptions = keyboardOptions,
            trailingIcon = trailingIcon,
            shape = RoundedCornerShape(10.dp),
            colors = fieldBoxColors()
        )
    }
}

/**
 * Collapsible section card (kept from the pre-v2.4.0 editor; the
 * editor itself now uses full section screens, but the helper stays
 * available to the other screens in this package).
 */
@Composable
fun AccordionSection(
    title: String,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 13.dp)
            ) {
                Text(
                    title,
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse section" else "Expand section",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (expanded) {
                content()
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun NumberField(label: String, value: Int, modifier: Modifier = Modifier, onChange: (Int) -> Unit) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { onChange(it.toIntOrNull() ?: 0) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier, singleLine = true
    )
}

/** Media-device stepper with − / ＋ buttons (capture §4.7 spinners). */
@Composable
fun StepperField(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, modifier = Modifier.weight(1f))
        IconButton(onClick = { if (value > 0) onChange(value - 1) }) {
            Icon(Icons.Filled.Remove, contentDescription = "Decrease")
        }
        Text(value.toString(), style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = { if (value < 9) onChange(value + 1) }) {
            Icon(Icons.Filled.Add, contentDescription = "Increase")
        }
    }
}

/** Radio-pill group (GoLogin hardware radios parity). */
@Composable
fun RadioChips(
    options: List<Pair<String, String>>,
    value: String,
    onSelect: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        options.forEach { (v, label) ->
            FilterChip(
                selected = value == v,
                onClick = { onSelect(v) },
                label = { Text(label) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val display = options.firstOrNull { it.first == value }?.second ?: value
    Column(modifier = Modifier.padding(bottom = 10.dp)) {
        FieldLabel(label)
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = display,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(10.dp),
                colors = fieldBoxColors()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (v, text) ->
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = { expanded = false; onSelect(v) }
                    )
                }
            }
        }
    }
}
