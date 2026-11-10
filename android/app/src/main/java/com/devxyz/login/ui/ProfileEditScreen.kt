package com.devxyz.login.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
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
import com.devxyz.login.model.FingerprintGenerator
import com.devxyz.login.model.Folder
import com.devxyz.login.model.OsOptions
import com.devxyz.login.model.Profile

/**
 * Create/Edit profile — same sections as the web editor:
 * Overview (OS picker + New Fingerprint), Proxy, Timezone, Extensions,
 * Bookmarks, Geolocation, Advanced fingerprint (Navigator / Fonts /
 * Media devices / Hardware / WebRTC). OS is locked after creation.
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

    fun save() {
        onSave(profile.copy(updatedAt = ProfileStore.nowIso()))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "New profile" else "Profile settings") },
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
            // ---------- Overview ----------
            SectionTitle("Overview")
            OutlinedTextField(
                value = profile.name,
                onValueChange = { profile = profile.copy(name = it) },
                label = { Text("Profile name") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Text("Operating system${if (!isNew) " (locked after creation)" else ""}",
                style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OsOptions.all.take(3).forEach { (value, label) ->
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
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OsOptions.all.drop(3).forEach { (value, label) ->
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

            // ---------- Proxy ----------
            SectionTitle("Proxy")
            DropdownField(
                label = "Proxy mode",
                value = profile.proxy.mode,
                options = listOf("none" to "Without proxy", "devxyz" to "DevxyzLogin proxy", "custom" to "Your proxy"),
                onSelect = { profile = profile.copy(proxy = profile.proxy.copy(mode = it)) }
            )
            if (profile.proxy.mode != "none") {
                DropdownField(
                    label = "Proxy type",
                    value = profile.proxy.type,
                    options = listOf("http" to "HTTP", "https" to "HTTPS", "socks4" to "SOCKS4", "socks5" to "SOCKS5"),
                    onSelect = { profile = profile.copy(proxy = profile.proxy.copy(type = it)) }
                )
                OutlinedTextField(
                    value = profile.proxy.host,
                    onValueChange = { profile = profile.copy(proxy = profile.proxy.copy(host = it)) },
                    label = { Text("Host") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = if (profile.proxy.port == 0) "" else profile.proxy.port.toString(),
                    onValueChange = { profile = profile.copy(proxy = profile.proxy.copy(port = it.toIntOrNull() ?: 0)) },
                    label = { Text("Port") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = profile.proxy.username,
                    onValueChange = { profile = profile.copy(proxy = profile.proxy.copy(username = it)) },
                    label = { Text("Login (optional)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = profile.proxy.password,
                    onValueChange = { profile = profile.copy(proxy = profile.proxy.copy(password = it)) },
                    label = { Text("Password (optional)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
            }

            // ---------- Timezone ----------
            SectionTitle("Timezone")
            SwitchRow("Fill timezone based on external IP", fp.timezone.auto) { on ->
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
            DropdownField(
                label = "Geolocation mode",
                value = fp.geolocation.mode,
                options = listOf("prompt" to "Prompt", "allow" to "Allow", "block" to "Block"),
                onSelect = { profile = profile.copy(fingerprint = fp.copy(geolocation = fp.geolocation.copy(mode = it))) }
            )
            SwitchRow("Fill geolocation based on external IP", fp.geolocation.auto) { on ->
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
            Text("Note: WebView cannot install Chromium extensions in v1; this list is stored with the profile (parity with GoLogin) for the future engine.",
                style = MaterialTheme.typography.bodySmall)

            // ---------- Bookmarks ----------
            SectionTitle("Bookmarks")
            var bookmarksText by remember { mutableStateOf(initial.bookmarks.joinToString("\n")) }
            OutlinedTextField(
                value = bookmarksText,
                onValueChange = {
                    bookmarksText = it
                    profile = profile.copy(bookmarks = it.lines().map { l -> l.trim() }.filter { l -> l.isNotEmpty() })
                },
                label = { Text("One per line: folder::name::url / name::url / url") },
                modifier = Modifier.fillMaxWidth(), minLines = 3
            )

            // ---------- Advanced fingerprint ----------
            SectionTitle("Advanced fingerprint — Navigator")
            OutlinedTextField(
                value = nav.userAgent,
                onValueChange = { profile = profile.copy(fingerprint = fp.copy(navigator = nav.copy(userAgent = it))) },
                label = { Text("User-Agent") },
                modifier = Modifier.fillMaxWidth(), minLines = 2
            )
            OutlinedTextField(
                value = nav.browserVersion,
                onValueChange = { profile = profile.copy(fingerprint = fp.copy(navigator = nav.copy(browserVersion = it))) },
                label = { Text("Browser version") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            DropdownField(
                label = "Screen resolution",
                value = nav.screenResolution,
                options = listOf("1920x1080", "1366x768", "1536x864", "2560x1440", "1440x900", "1080x2400", "1080x2340")
                    .map { it to it },
                onSelect = { profile = profile.copy(fingerprint = fp.copy(navigator = nav.copy(screenResolution = it))) }
            )
            OutlinedTextField(
                value = nav.languages.joinToString(", "),
                onValueChange = { text ->
                    profile = profile.copy(fingerprint = fp.copy(navigator = nav.copy(
                        languages = text.split(",").map { it.trim() }.filter { it.isNotEmpty() })))
                },
                label = { Text("Languages (comma separated)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = nav.platform,
                onValueChange = { profile = profile.copy(fingerprint = fp.copy(navigator = nav.copy(platform = it))) },
                label = { Text("Platform") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nav.cpuCores.toString(),
                    onValueChange = { v -> profile = profile.copy(fingerprint = fp.copy(navigator = nav.copy(cpuCores = v.toIntOrNull() ?: 0))) },
                    label = { Text("CPU cores") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f), singleLine = true
                )
                OutlinedTextField(
                    value = nav.ram.toString(),
                    onValueChange = { v -> profile = profile.copy(fingerprint = fp.copy(navigator = nav.copy(ram = v.toIntOrNull() ?: 0))) },
                    label = { Text("RAM (GB)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f), singleLine = true
                )
            }
            SwitchRow("Do Not Track", nav.doNotTrack) { on ->
                profile = profile.copy(fingerprint = fp.copy(navigator = nav.copy(doNotTrack = on)))
            }

            SectionTitle("Fonts")
            SwitchRow("Fonts masking", fp.fontsMasking) { on ->
                profile = profile.copy(fingerprint = fp.copy(fontsMasking = on))
            }

            SectionTitle("Media devices")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Video in", fp.mediaDevices.videoInputs, Modifier.weight(1f)) { v ->
                    profile = profile.copy(fingerprint = fp.copy(mediaDevices = fp.mediaDevices.copy(videoInputs = v)))
                }
                NumberField("Audio in", fp.mediaDevices.audioInputs, Modifier.weight(1f)) { v ->
                    profile = profile.copy(fingerprint = fp.copy(mediaDevices = fp.mediaDevices.copy(audioInputs = v)))
                }
                NumberField("Audio out", fp.mediaDevices.audioOutputs, Modifier.weight(1f)) { v ->
                    profile = profile.copy(fingerprint = fp.copy(mediaDevices = fp.mediaDevices.copy(audioOutputs = v)))
                }
            }

            SectionTitle("Hardware")
            DropdownField(
                label = "Canvas mode",
                value = hw.canvasMode,
                options = listOf("off" to "Off", "block" to "Block", "noise" to "Add Noise"),
                onSelect = { profile = profile.copy(fingerprint = fp.copy(hardware = hw.copy(canvasMode = it))) }
            )
            OutlinedTextField(
                value = hw.webglVendor,
                onValueChange = { profile = profile.copy(fingerprint = fp.copy(hardware = hw.copy(webglVendor = it))) },
                label = { Text("WebGL vendor") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = hw.webglRenderer,
                onValueChange = { profile = profile.copy(fingerprint = fp.copy(hardware = hw.copy(webglRenderer = it))) },
                label = { Text("WebGL renderer") },
                modifier = Modifier.fillMaxWidth(), minLines = 2
            )
            SwitchRow("WebGL noise", hw.webglNoise) { on ->
                profile = profile.copy(fingerprint = fp.copy(hardware = hw.copy(webglNoise = on)))
            }
            SwitchRow("AudioContext noise", hw.audioNoise) { on ->
                profile = profile.copy(fingerprint = fp.copy(hardware = hw.copy(audioNoise = on)))
            }
            SwitchRow("ClientRects noise", hw.clientRectsNoise) { on ->
                profile = profile.copy(fingerprint = fp.copy(hardware = hw.copy(clientRectsNoise = on)))
            }

            SectionTitle("WebRTC")
            DropdownField(
                label = "WebRTC mode",
                value = fp.webrtcMode,
                options = listOf("basedOnIp" to "Based on IP", "off" to "Off", "disabled" to "Disabled"),
                onSelect = { profile = profile.copy(fingerprint = fp.copy(webrtcMode = it)) }
            )

            Spacer(Modifier.height(8.dp))
            Button(onClick = { save() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (isNew) "Create profile" else "Save profile")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp))
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
