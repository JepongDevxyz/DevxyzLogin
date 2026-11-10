package com.devxyz.login.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.devxyz.login.data.ProfileStore
import com.devxyz.login.model.CaptureData
import com.devxyz.login.model.DefaultBookmarks
import com.devxyz.login.model.FingerprintGenerator
import com.devxyz.login.model.Folder
import com.devxyz.login.model.OsOptions
import com.devxyz.login.model.Profile

/**
 * Create/Edit profile — section order follows the GoLogin editor with the
 * Proxy tab prominent (it is the DEFAULT tab in GoLogin, capture §4):
 * Name/Folder → Proxy → Overview (OS + New Fingerprint) → Profile Summary →
 * Timezone → Geolocation → Extensions → Bookmarks → Advanced (Navigator /
 * Fonts / Media devices / Hardware / Storage / Other / WebRTC) → Cookies.
 * All option lists come from [CaptureData] (capture-exact). OS locked after
 * creation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    initial: Profile,
    isNew: Boolean,
    folders: List<Folder>,
    onSave: (Profile) -> Unit,
    onBack: () -> Unit
) {
    var profile by remember { mutableStateOf(initial) }
    val fp = profile.fingerprint
    val nav = fp.navigator
    val hw = fp.hardware
    val browser = profile.browser
    var showCookiesImport by remember { mutableStateOf(false) }

    fun save() {
        onSave(profile.copy(updatedAt = ProfileStore.nowIso()))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "New Browser Profile" else "Profile settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ---------- Name / folder ----------
            OutlinedTextField(
                value = profile.name,
                onValueChange = { profile = profile.copy(name = it) },
                label = { Text("Profile Name") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            val folderLabel = folders.filter { it.id in profile.folderIds }
                .joinToString { it.name }.ifBlank { "No folder" }
            Text("Folder: $folderLabel", style = MaterialTheme.typography.bodySmall)

            // ---------- Proxy (GoLogin's default editor tab) ----------
            SectionTitle("Proxy")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = profile.proxy.mode == "devxyz",
                    onClick = { profile = profile.copy(proxy = profile.proxy.copy(mode = "devxyz")) },
                    label = { Text("DevxyzLogin proxy") }
                )
                FilterChip(
                    selected = profile.proxy.mode == "custom",
                    onClick = { profile = profile.copy(proxy = profile.proxy.copy(mode = "custom")) },
                    label = { Text("Your proxy") }
                )
                FilterChip(
                    selected = profile.proxy.mode == "none",
                    onClick = { profile = profile.copy(proxy = profile.proxy.copy(mode = "none")) },
                    label = { Text("Without proxy") }
                )
            }
            when (profile.proxy.mode) {
                "devxyz" -> {
                    Text("Proxy per GB", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CaptureData.proxyCountries.forEach { (code, country) ->
                            FilterChip(
                                selected = profile.proxy.host.contains(code, ignoreCase = true),
                                onClick = {
                                    profile = profile.copy(
                                        proxy = profile.proxy.copy(
                                            type = "http",
                                            host = "geo.devxyzlogin.local",
                                            port = 10080
                                        )
                                    )
                                },
                                label = { Text(country) }
                            )
                        }
                    }
                    Text("Type", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Residential", "Mobile", "Data Center").forEach { kind ->
                            FilterChip(
                                selected = kind == "Residential",
                                onClick = { },
                                label = { Text(kind) }
                            )
                        }
                    }
                    Text(
                        "Built-in proxies are assigned by the DevxyzLogin server; country defaults to France (GoLogin parity). Use \"Your proxy\" for a proxy you already own.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                "custom" -> {
                    DropdownField(
                        label = "Proxy Type, Host and Port",
                        value = profile.proxy.type,
                        options = CaptureData.proxyTypeOptions + ("https" to "HTTPS Proxy"),
                        onSelect = { profile = profile.copy(proxy = profile.proxy.copy(type = it)) }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = profile.proxy.host,
                            onValueChange = { profile = profile.copy(proxy = profile.proxy.copy(host = it)) },
                            label = { Text("IP Address") },
                            modifier = Modifier.weight(1f), singleLine = true
                        )
                        OutlinedTextField(
                            value = if (profile.proxy.port == 0) "" else profile.proxy.port.toString(),
                            onValueChange = { profile = profile.copy(proxy = profile.proxy.copy(port = it.toIntOrNull() ?: 0)) },
                            label = { Text("Port") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(110.dp), singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = profile.proxy.username,
                        onValueChange = { profile = profile.copy(proxy = profile.proxy.copy(username = it)) },
                        label = { Text("Proxy Username") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    OutlinedTextField(
                        value = profile.proxy.password,
                        onValueChange = { profile = profile.copy(proxy = profile.proxy.copy(password = it)) },
                        label = { Text("Proxy Password") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Text(
                        "Note: Android WebView applies HTTP/HTTPS proxies only; SOCKS and proxy authentication cannot be applied (you'll be told at launch).",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                else -> {
                    Text(
                        "This profile connects directly, without a proxy.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            OutlinedButton(onClick = { /* format check only in-app */ }) {
                Text("Check Proxy")
            }

            // ---------- Overview ----------
            SectionTitle("Overview")
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
                            profile = profile.copy(
                                os = value,
                                fingerprint = FingerprintGenerator.generate(value)
                            )
                        },
                        label = { Text(label) }
                    )
                }
            }
            OutlinedButton(onClick = {
                profile = profile.copy(fingerprint = FingerprintGenerator.generate(profile.os))
            }) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.padding(4.dp))
                Text("New Fingerprint")
            }
            // Folders membership
            Text("Folders", style = MaterialTheme.typography.bodySmall)
            if (folders.isEmpty()) {
                Text("No folders yet — create one in Folders manager.", style = MaterialTheme.typography.bodySmall)
            }
            folders.forEach { folder ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = folder.id in profile.folderIds,
                        onCheckedChange = { on ->
                            profile = profile.copy(
                                folderIds = if (on) profile.folderIds + folder.id
                                else profile.folderIds - folder.id
                            )
                        }
                    )
                    Text(folder.name)
                }
            }
            OutlinedTextField(
                value = profile.tags.joinToString(", "),
                onValueChange = { text ->
                    profile = profile.copy(
                        tags = text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    )
                },
                label = { Text("Tags (comma separated)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = profile.notes,
                onValueChange = { profile = profile.copy(notes = it) },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(), minLines = 2
            )

            // ---------- Profile Summary (capture §4.9) ----------
            ProfileSummaryCard(profile)

            // ---------- Timezone ----------
            SectionTitle("Timezone")
            SwitchRow("Fill timezone based on the external IP", fp.timezone.auto) { on ->
                profile = profile.copy(fingerprint = fp.copy(timezone = fp.timezone.copy(auto = on)))
            }
            if (!fp.timezone.auto) {
                OutlinedTextField(
                    value = fp.timezone.zone,
                    onValueChange = { profile = profile.copy(fingerprint = fp.copy(timezone = fp.timezone.copy(zone = it))) },
                    label = { Text("Timezone (IANA, e.g. Asia/Manila)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
            }

            // ---------- Geolocation ----------
            SectionTitle("Geolocation")
            RadioChips(
                options = listOf("prompt" to "Prompt", "allow" to "Allow", "block" to "Block"),
                value = fp.geolocation.mode,
                onSelect = { profile = profile.copy(fingerprint = fp.copy(geolocation = fp.geolocation.copy(mode = it))) }
            )
            SwitchRow("Fill geolocation based on the external IP", fp.geolocation.auto) { on ->
                profile = profile.copy(fingerprint = fp.copy(geolocation = fp.geolocation.copy(auto = on)))
            }
            if (!fp.geolocation.auto) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fp.geolocation.lat.toString(),
                        onValueChange = { v -> profile = profile.copy(fingerprint = fp.copy(geolocation = fp.geolocation.copy(lat = v.toDoubleOrNull() ?: 0.0))) },
                        label = { Text("Latitude") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                    OutlinedTextField(
                        value = fp.geolocation.lon.toString(),
                        onValueChange = { v -> profile = profile.copy(fingerprint = fp.copy(geolocation = fp.geolocation.copy(lon = v.toDoubleOrNull() ?: 0.0))) },
                        label = { Text("Longitude") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                }
            }

            // ---------- Extensions ----------
            SectionTitle("Extensions")
            profile.extensions.forEachIndexed { index, ext ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ext, modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        profile = profile.copy(extensions = profile.extensions.filterIndexed { i, _ -> i != index })
                    }) { Icon(Icons.Filled.Delete, contentDescription = "Remove extension") }
                }
            }
            var newExt by remember { mutableStateOf("") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newExt,
                    onValueChange = { newExt = it },
                    label = { Text("Extension name / ID") },
                    modifier = Modifier.weight(1f), singleLine = true
                )
                IconButton(onClick = {
                    if (newExt.isNotBlank()) {
                        profile = profile.copy(extensions = profile.extensions + newExt.trim())
                        newExt = ""
                    }
                }) { Icon(Icons.Filled.Add, contentDescription = "Add extension") }
            }
            OutlinedButton(onClick = {
                if (newExt.isNotBlank()) {
                    profile = profile.copy(extensions = profile.extensions + newExt.trim())
                    newExt = ""
                }
            }) { Text("Add extensions") }
            Text(
                "Note: WebView cannot install Chromium extensions; this list is stored with the profile (GoLogin parity) for the future engine.",
                style = MaterialTheme.typography.bodySmall
            )

            // ---------- Bookmarks ----------
            SectionTitle("Bookmarks")
            var bookmarksText by remember {
                mutableStateOf(
                    initial.bookmarks.joinToString("\n").ifBlank {
                        if (isNew) DefaultBookmarks.editorText else ""
                    }
                )
            }
            OutlinedTextField(
                value = bookmarksText,
                onValueChange = {
                    bookmarksText = it
                    profile = profile.copy(
                        bookmarks = it.lines().map { l -> l.trimEnd() }
                            .filter { l -> l.isNotBlank() }
                    )
                },
                label = { Text("Edit Bookmarks — one per line: folder::name::url") },
                modifier = Modifier.fillMaxWidth(), minLines = 6
            )

            // ---------- Advanced: Navigator ----------
            SectionTitle("Advanced — Navigator")
            OutlinedTextField(
                value = nav.userAgent,
                onValueChange = { profile = profile.copy(fingerprint = fp.copy(navigator = nav.copy(userAgent = it))) },
                label = { Text("User-Agent") },
                modifier = Modifier.fillMaxWidth(), minLines = 2
            )
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
                            profile = profile.copy(
                                fingerprint = fp.copy(
                                    navigator = nav.copy(
                                        userAgent = CaptureData.uaFor(profile.os, major, full),
                                        browserVersion = full
                                    )
                                )
                            )
                        }
                    )
                }
                IconButton(onClick = {
                    profile = profile.copy(fingerprint = FingerprintGenerator.generate(profile.os))
                }) { Icon(Icons.Filled.Refresh, contentDescription = "Regenerate fingerprint") }
            }
            DropdownField(
                label = "Screen Resolution",
                value = nav.screenResolution,
                options = (CaptureData.screenResolutions + nav.screenResolution)
                    .distinct().map { it to it },
                onSelect = { profile = profile.copy(fingerprint = fp.copy(navigator = nav.copy(screenResolution = it))) }
            )
            OutlinedTextField(
                value = nav.languages.joinToString(", "),
                onValueChange = { text ->
                    profile = profile.copy(
                        fingerprint = fp.copy(
                            navigator = nav.copy(
                                languages = text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            )
                        )
                    )
                },
                label = { Text("Languages (comma separated)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            SwitchRow("Languages based on IP", fp.timezone.auto) { on ->
                profile = profile.copy(fingerprint = fp.copy(timezone = fp.timezone.copy(auto = on)))
            }
            OutlinedTextField(
                value = nav.platform,
                onValueChange = { profile = profile.copy(fingerprint = fp.copy(navigator = nav.copy(platform = it))) },
                label = { Text("Platform") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            DropdownField(
                label = "CPU Threads",
                value = nav.cpuCores.toString(),
                options = CaptureData.cpuOptions.map { it.toString() to it.toString() },
                onSelect = { profile = profile.copy(fingerprint = fp.copy(navigator = nav.copy(cpuCores = it.toInt()))) }
            )
            DropdownField(
                label = "RAM",
                value = nav.ram.toString(),
                options = CaptureData.ramOptions.map { it.toString() to "$it GB" },
                onSelect = { profile = profile.copy(fingerprint = fp.copy(navigator = nav.copy(ram = it.toInt()))) }
            )
            SwitchRow("DoNotTrack", nav.doNotTrack) { on ->
                profile = profile.copy(fingerprint = fp.copy(navigator = nav.copy(doNotTrack = on)))
            }

            SectionTitle("Fonts")
            SwitchRow("Enable fonts list masking", fp.fontsMasking) { on ->
                profile = profile.copy(fingerprint = fp.copy(fontsMasking = on))
            }

            SectionTitle("Media devices")
            StepperField("Video Inputs", fp.mediaDevices.videoInputs) { v ->
                profile = profile.copy(fingerprint = fp.copy(mediaDevices = fp.mediaDevices.copy(videoInputs = v)))
            }
            StepperField("Audio Inputs", fp.mediaDevices.audioInputs) { v ->
                profile = profile.copy(fingerprint = fp.copy(mediaDevices = fp.mediaDevices.copy(audioInputs = v)))
            }
            StepperField("Audio Outputs", fp.mediaDevices.audioOutputs) { v ->
                profile = profile.copy(fingerprint = fp.copy(mediaDevices = fp.mediaDevices.copy(audioOutputs = v)))
            }

            // ---------- Advanced: Hardware ----------
            SectionTitle("Advanced — Hardware")
            Text("Canvas", style = MaterialTheme.typography.bodySmall)
            RadioChips(
                options = listOf("noise" to "Noise", "off" to "Off", "block" to "Block"),
                value = hw.canvasMode,
                onSelect = { profile = profile.copy(fingerprint = fp.copy(hardware = hw.copy(canvasMode = it))) }
            )
            Text("Client Rects", style = MaterialTheme.typography.bodySmall)
            RadioChips(
                options = listOf("noise" to "Noise", "off" to "Off"),
                value = if (hw.clientRectsNoise) "noise" else "off",
                onSelect = { profile = profile.copy(fingerprint = fp.copy(hardware = hw.copy(clientRectsNoise = it == "noise"))) }
            )
            Text("Audio Context", style = MaterialTheme.typography.bodySmall)
            RadioChips(
                options = listOf("noise" to "Noise", "off" to "Off"),
                value = if (hw.audioNoise) "noise" else "off",
                onSelect = { profile = profile.copy(fingerprint = fp.copy(hardware = hw.copy(audioNoise = it == "noise"))) }
            )
            Text("WebGL Image", style = MaterialTheme.typography.bodySmall)
            RadioChips(
                options = listOf("noise" to "Noise", "off" to "Off"),
                value = if (hw.webglNoise) "noise" else "off",
                onSelect = { profile = profile.copy(fingerprint = fp.copy(hardware = hw.copy(webglNoise = it == "noise"))) }
            )
            Text("WebGL Metadata", style = MaterialTheme.typography.bodySmall)
            RadioChips(
                options = listOf("mask" to "Mask", "real" to "Real"),
                value = if (hw.webglMetadataMask) "mask" else "real",
                onSelect = { profile = profile.copy(fingerprint = fp.copy(hardware = hw.copy(webglMetadataMask = it == "mask"))) }
            )
            DropdownField(
                label = "WebGL Vendor",
                value = hw.webglVendor,
                options = (CaptureData.webglVendors + hw.webglVendor)
                    .filter { it.isNotBlank() }.distinct().map { it to it },
                onSelect = { vendor ->
                    val renderers = CaptureData.webglRenderersFor(vendor)
                    profile = profile.copy(
                        fingerprint = fp.copy(
                            hardware = hw.copy(
                                webglVendor = vendor,
                                webglRenderer = if (hw.webglRenderer in renderers) hw.webglRenderer
                                else renderers.firstOrNull() ?: hw.webglRenderer
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
                onSelect = { profile = profile.copy(fingerprint = fp.copy(hardware = hw.copy(webglRenderer = it))) }
            )

            // ---------- Advanced: Storage / Other ----------
            SectionTitle("Advanced — Storage Options")
            SwitchRow("Enable Local Storage", browser.localStorage) { on ->
                profile = profile.copy(browser = browser.copy(localStorage = on))
            }
            SwitchRow("Enable Extension storage", browser.extensionStorage) { on ->
                profile = profile.copy(browser = browser.copy(extensionStorage = on))
            }
            SwitchRow("Enable potentially vulnerable plugins", browser.vulnerablePlugins) { on ->
                profile = profile.copy(browser = browser.copy(vulnerablePlugins = on))
            }

            SectionTitle("Advanced — Other")
            SwitchRow("Enable Google services", browser.googleServices) { on ->
                profile = profile.copy(browser = browser.copy(googleServices = on))
            }
            SwitchRow("Disable image loading", browser.disableImageLoading) { on ->
                profile = profile.copy(browser = browser.copy(disableImageLoading = on))
            }
            SwitchRow("Disable video autoplay", browser.disableVideoAutoplay) { on ->
                profile = profile.copy(browser = browser.copy(disableVideoAutoplay = on))
            }
            SwitchRow("Enable browser bookmark saving", browser.saveBookmarks) { on ->
                profile = profile.copy(browser = browser.copy(saveBookmarks = on))
            }
            SwitchRow("Enable browser history saving", browser.saveHistory) { on ->
                profile = profile.copy(browser = browser.copy(saveHistory = on))
            }
            SwitchRow("Enable password saving", browser.savePasswords) { on ->
                profile = profile.copy(browser = browser.copy(savePasswords = on))
            }
            SwitchRow("Enable System Extensions", browser.systemExtensions) { on ->
                profile = profile.copy(browser = browser.copy(systemExtensions = on))
            }
            DropdownField(
                label = "Start URL",
                value = browser.startUrl,
                options = CaptureData.startUrlOptions,
                onSelect = { profile = profile.copy(browser = browser.copy(startUrl = it)) }
            )
            if (browser.startUrl == "specified") {
                var pagesText by remember {
                    mutableStateOf(initial.browser.startPages.joinToString("\n"))
                }
                OutlinedTextField(
                    value = pagesText,
                    onValueChange = {
                        pagesText = it
                        profile = profile.copy(
                            browser = profile.browser.copy(
                                startPages = it.lines().map { l -> l.trim() }.filter { l -> l.isNotEmpty() }
                            )
                        )
                    },
                    label = { Text("Specified pages (one URL per line)") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2
                )
            }
            OutlinedTextField(
                value = browser.launchArguments,
                onValueChange = { profile = profile.copy(browser = browser.copy(launchArguments = it)) },
                label = { Text("Launch arguments") },
                placeholder = { Text("Example: --disable-gpu --start-maximized") },
                modifier = Modifier.fillMaxWidth(), minLines = 2
            )
            OutlinedTextField(
                value = browser.customDns,
                onValueChange = { profile = profile.copy(browser = browser.copy(customDns = it)) },
                label = { Text("Custom DNS") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )

            SectionTitle("WebRTC")
            RadioChips(
                options = listOf("basedOnIp" to "Based on IP", "off" to "Off"),
                value = if (fp.webrtcMode == "basedOnIp") "basedOnIp" else "off",
                onSelect = { profile = profile.copy(fingerprint = fp.copy(webrtcMode = it)) }
            )

            // ---------- Cookies ----------
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

            Spacer(Modifier.height(8.dp))
            Button(onClick = { save() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (isNew) "Create Profile" else "Save profile")
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showCookiesImport) {
        var text by remember { mutableStateOf(profile.cookies) }
        AlertDialog(
            onDismissRequest = { showCookiesImport = false },
            title = { Text("Import Cookies") },
            text = {
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    label = { Text("Paste cookies (JSON or name=value lines)") },
                    minLines = 5
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    profile = profile.copy(cookies = text)
                    showCookiesImport = false
                }) { Text("Import") }
            },
            dismissButton = { TextButton(onClick = { showCookiesImport = false }) { Text("Cancel") } }
        )
    }
}

/** Profile Summary card (capture §4.9 rows). */
@Composable
private fun ProfileSummaryCard(profile: Profile) {
    val fp = profile.fingerprint
    val nav = fp.navigator
    val hw = fp.hardware
    val rows = listOf(
        "Profile name" to profile.name,
        "Proxy" to when (profile.proxy.mode) {
            "devxyz" -> "DevxyzLogin"
            "custom" -> profile.proxy.type
            else -> "none"
        },
        "Languages" to nav.languages.joinToString(" "),
        "Timezone" to if (fp.timezone.auto) "Based on IP" else fp.timezone.zone,
        "Geolocation" to if (fp.geolocation.auto) "Based on IP" else fp.geolocation.mode,
        "WebGL Info" to if (hw.webglMetadataMask) "Masked" else "Real",
        "Media Devices" to "Masked",
        "Canvas" to when (hw.canvasMode) {
            "noise" -> "Masked with noise"
            "block" -> "Blocked"
            else -> "off"
        },
        "Client Rects" to if (hw.clientRectsNoise) "noise" else "off",
        "WebGL Metadata" to if (hw.webglMetadataMask) "mask" else "real",
        "WebGL Image" to if (hw.webglNoise) "Masked with noise" else "off",
        "Audio Context" to if (hw.audioNoise) "Masked with noise" else "off",
        "Fonts" to if (fp.fontsMasking) "Masked" else "Real",
        "UA" to "Chrome ${nav.browserVersion.substringBefore(".")}",
        "Resolution" to nav.screenResolution,
        "OS" to OsOptions.label(profile.os),
        "Browser" to "Orbita",
        "Platform" to nav.platform,
        "Media devices" to "(${fp.mediaDevices.videoInputs}|${fp.mediaDevices.audioInputs}|${fp.mediaDevices.audioOutputs})",
        "Local Storage" to profile.browser.localStorage.toString(),
        "Ext. Storage" to profile.browser.extensionStorage.toString(),
        "Plugins" to profile.browser.vulnerablePlugins.toString()
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Profile Summary", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            rows.forEach { (label, value) ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(110.dp)
                    )
                    Text(value, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text, style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
    )
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
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
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
