package com.devxyz.login.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
import com.devxyz.login.model.SharedProxy
import kotlinx.coroutines.launch
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Create/Edit profile — v3 layout follows the GoLogin ANDROID app
 * (Play screenshots): a "Create profile" screen of collapsible accordion
 * sections in the order Operating system → Proxy → Timezone → WebRTC →
 * Geolocation → Extensions → Bookmarks → Advanced → Cookies, with the
 * Profile Summary card under the name field and a full-width solid
 * "CREATE PROFILE" button. Section contents transcribe the web capture
 * (GOLOGIN_CAPTURE.md §4): Proxy is the default-expanded section (it is
 * the DEFAULT tab in the web editor), OS defaults to Linux and locks
 * after creation, and every option list comes from [CaptureData] /
 * [Countries] (capture-exact).
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
    /** v4: live server session for the REAL proxy check (null = local mode). */
    apiToken: String? = null,
    apiWorkspaceId: String? = null
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val api = remember { ServerApi() }
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf(initial) }
    // Real proxy-check state (POST /api/proxies/check via the server —
    // the server requires auth, so checks need a logged-in session).
    var checkingProxy by remember { mutableStateOf(false) }
    var checkText by remember { mutableStateOf<String?>(null) }
    var checkFailed by remember { mutableStateOf(false) }

    fun toast(msg: String) {
        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
    }

    /** Applies a successful check: stores the measured result on the
     *  profile (saved with it) and resolves the timezone when it is in
     *  "Based on IP" mode. */
    fun applyCheck(info: com.devxyz.login.model.ProxyCheckInfo) {
        var next = profile.copy(lastProxyCheck = info)
        if (next.fingerprint.timezone.auto && info.timezone.isNotBlank()) {
            next = next.copy(
                fingerprint = next.fingerprint.copy(
                    timezone = next.fingerprint.timezone.copy(zone = info.timezone)
                )
            )
        }
        profile = next
        checkFailed = false
        checkText = "✓ ${info.ip} — ${info.place.ifBlank { "Unknown location" }} • " +
            "${info.timezone} • ${info.latencyMs} ms"
    }

    fun runCheck(p: ProxyConfig) {
        if (apiToken == null) {
            toast("Log in to run a real proxy check — the DevxyzLogin server checks the proxy for you")
            return
        }
        if (p.host.isBlank() || p.port <= 0) {
            toast("Enter the proxy IP and port first")
            return
        }
        checkingProxy = true
        checkText = null
        scope.launch {
            when (val r = api.checkProxy(apiToken, p.type, p.host, p.port, p.username, p.password)) {
                is ApiResult.Success -> {
                    checkingProxy = false
                    applyCheck(r.data)
                }
                is ApiResult.Failure -> {
                    checkingProxy = false
                    checkFailed = true
                    checkText = "✗ ${r.message}"
                }
            }
        }
    }
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
                title = { Text(if (isNew) "Create profile" else "Edit profile") },
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

            // ---------- Profile Summary (capture §4.9) ----------
            ProfileSummaryCard(profile, folders)

            // ---------- Operating system (web editor "Overview") ----------
            AccordionSection("Operating system", initiallyExpanded = true) {
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
            }

            // ---------- Proxy (GoLogin's default editor tab) ----------
            AccordionSection("Proxy", initiallyExpanded = true) {
                Text("Connection type", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = profile.proxy.mode == "devxyz",
                        onClick = { profile = profile.copy(proxy = profile.proxy.copy(mode = "devxyz")) },
                        label = { Text("DevxyzLogin proxy") }
                    )
                    FilterChip(
                        selected = profile.proxy.mode == "custom",
                        onClick = {
                            val p = profile.proxy
                            profile = profile.copy(
                                proxy = p.copy(
                                    mode = "custom",
                                    type = if (p.type in listOf("http", "https", "socks4", "socks5")) p.type else "http"
                                )
                            )
                        },
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
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = !profile.proxy.dedicated,
                                onClick = { profile = profile.copy(proxy = profile.proxy.copy(dedicated = false)) },
                                label = { Text("Proxy per GB") }
                            )
                            FilterChip(
                                selected = profile.proxy.dedicated,
                                onClick = { profile = profile.copy(proxy = profile.proxy.copy(dedicated = true)) },
                                label = { Text("Buy dedicated IP") }
                            )
                        }
                        DropdownField(
                            label = "Country",
                            value = profile.proxy.buyCountry,
                            options = Countries.all.map {
                                it.code to "${Countries.flag(it.code)} ${it.name}"
                            },
                            onSelect = { code ->
                                profile = profile.copy(
                                    proxy = profile.proxy.copy(
                                        buyCountry = code,
                                        type = "http",
                                        host = "geo.devxyzlogin.local",
                                        port = 10080
                                    )
                                )
                            }
                        )
                        if (profile.proxy.dedicated) {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    listOf(
                                        "Same IP every time",
                                        "Looks like a home connection",
                                        "Just for you — no sharing",
                                        "Unlimited traffic"
                                    ).forEach { benefit ->
                                        Text("✓ $benefit", color = BrandTeal)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Button(onClick = {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Dedicated IP checkout runs through the DevxyzLogin server",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }, modifier = Modifier.fillMaxWidth()) {
                                        Text("Buy for $5/month")
                                    }
                                    Text(
                                        "Requires paid subscription",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "Create Profile is disabled in Buy dedicated IP mode (GoLogin parity).",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        } else {
                            Text("Type", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Residential", "Mobile", "Data Center").forEach { kind ->
                                    FilterChip(
                                        selected = profile.proxy.buyType == kind,
                                        onClick = {
                                            profile = profile.copy(proxy = profile.proxy.copy(buyType = kind))
                                        },
                                        label = { Text(kind) }
                                    )
                                }
                            }
                            OutlinedButton(
                                enabled = !checkingProxy,
                                onClick = {
                                    if (apiToken == null) {
                                        toast("Log in to get a built-in proxy from the DevxyzLogin server")
                                    } else {
                                        checkingProxy = true
                                        checkText = null
                                        scope.launch {
                                            val typeKey = when (profile.proxy.buyType) {
                                                "Mobile" -> "mobile"
                                                "Data Center" -> "datacenter"
                                                else -> "residential"
                                            }
                                            when (val r = api.builtinProxy(
                                                apiToken, apiWorkspaceId,
                                                profile.proxy.buyCountry, typeKey
                                            )) {
                                                is ApiResult.Success -> {
                                                    val proxyObj = r.data["proxy"]
                                                    val decoded = try {
                                                        proxyObj?.let {
                                                            kotlinx.serialization.json.Json {
                                                                ignoreUnknownKeys = true
                                                            }.decodeFromJsonElement(
                                                                ProxyConfig.serializer(), it
                                                            )
                                                        }
                                                    } catch (e: Exception) { null }
                                                    if (decoded == null || decoded.host.isBlank()) {
                                                        checkingProxy = false
                                                        checkFailed = true
                                                        checkText = "✗ The server returned an unusable proxy"
                                                    } else {
                                                        val assigned = decoded.copy(mode = "custom")
                                                        profile = profile.copy(proxy = assigned)
                                                        // Now verify the assigned endpoint for real.
                                                        when (val cr = api.checkProxy(
                                                            apiToken, assigned.type, assigned.host,
                                                            assigned.port, assigned.username, assigned.password
                                                        )) {
                                                            is ApiResult.Success -> {
                                                                checkingProxy = false
                                                                applyCheck(cr.data)
                                                            }
                                                            is ApiResult.Failure -> {
                                                                checkingProxy = false
                                                                checkFailed = true
                                                                checkText = "Proxy assigned (${assigned.host}:${assigned.port}) but the check failed: ${cr.message}"
                                                            }
                                                        }
                                                    }
                                                }
                                                is ApiResult.Failure -> {
                                                    checkingProxy = false
                                                    checkFailed = true
                                                    checkText = if (r.httpCode == 501)
                                                        "✗ No ${typeKey} provider is configured on the server yet — built-in proxies need a plugged-in supplier"
                                                    else "✗ ${r.message}"
                                                }
                                            }
                                        }
                                    }
                                }
                            ) { Text(if (checkingProxy) "Checking…" else "Check Proxy") }
                            if (checkText != null) {
                                Text(
                                    checkText!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (checkFailed) MaterialTheme.colorScheme.error else BrandTeal
                                )
                            }
                            Text(
                                "Built-in proxies are assigned by the DevxyzLogin server; country defaults to France (GoLogin parity). Use \"Your proxy\" for a proxy you already own.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    "custom" -> {
                        if (savedProxies.isNotEmpty()) {
                            DropdownField(
                                label = "Choose saved proxy",
                                value = "",
                                options = listOf("" to "Select saved proxy") + savedProxies.map {
                                    it.id to "${it.name} — ${it.proxy.host}:${it.proxy.port}"
                                },
                                onSelect = { id ->
                                    savedProxies.firstOrNull { it.id == id }?.let { sp ->
                                        profile = profile.copy(proxy = sp.proxy.copy(mode = "custom"))
                                    }
                                }
                            )
                            Text(
                                "Add or edit proxy — manage the list on the Proxies page.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        DropdownField(
                            label = "Proxy Type, Host and Port",
                            value = profile.proxy.type,
                            options = CaptureData.proxyTypeOptions,
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
                        OutlinedTextField(
                            value = profile.proxy.changeIpUrl,
                            onValueChange = { profile = profile.copy(proxy = profile.proxy.copy(changeIpUrl = it)) },
                            label = { Text("Change IP URL for mobile proxy") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                        Text(
                            "Note: Android WebView applies HTTP/HTTPS proxies only; SOCKS and proxy authentication cannot be applied (you'll be told at launch).",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedButton(
                            enabled = !checkingProxy,
                            onClick = { runCheck(profile.proxy) }
                        ) { Text(if (checkingProxy) "Checking…" else "Check Proxy") }
                        if (checkText != null) {
                            Text(
                                checkText!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (checkFailed) MaterialTheme.colorScheme.error else BrandTeal
                            )
                        }
                        if (apiToken == null) {
                            Text(
                                "Proxy checks run on the DevxyzLogin server — log in to check this proxy for real.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    else -> {
                        Text(
                            "This profile connects directly, without a proxy.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // ---------- Timezone ----------
            AccordionSection("Timezone") {
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
            }

            // ---------- WebRTC (own accordion in the Android app) ----------
            AccordionSection("WebRTC") {
                RadioChips(
                    options = listOf("basedOnIp" to "Based on IP", "off" to "Off"),
                    value = if (fp.webrtcMode == "basedOnIp") "basedOnIp" else "off",
                    onSelect = { profile = profile.copy(fingerprint = fp.copy(webrtcMode = it)) }
                )
            }

            // ---------- Geolocation ----------
            AccordionSection("Geolocation") {
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
            }

            // ---------- Extensions ----------
            AccordionSection("Extensions") {
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
            }

            // ---------- Bookmarks ----------
            AccordionSection("Bookmarks") {
            var bookmarksText by remember {
                mutableStateOf(
                    profile.bookmarks.joinToString("\n").ifBlank {
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
            }

            // ---------- Advanced: Navigator ----------
            AccordionSection("Advanced") {
            SectionTitle("Navigator")
            // Play screenshot: the UA field carries a copy icon at its right.
            Row(verticalAlignment = Alignment.Top) {
                OutlinedTextField(
                    value = nav.userAgent,
                    onValueChange = { profile = profile.copy(fingerprint = fp.copy(navigator = nav.copy(userAgent = it))) },
                    label = { Text("User-Agent") },
                    modifier = Modifier.weight(1f), minLines = 2
                )
                IconButton(onClick = {
                    clipboard.setText(AnnotatedString(nav.userAgent))
                    android.widget.Toast.makeText(context, "User-Agent copied", android.widget.Toast.LENGTH_SHORT).show()
                }) { Icon(Icons.Filled.ContentCopy, contentDescription = "Copy User-Agent") }
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
            if (nav.languagesAuto) {
                // Capture §4.7: with "Based on IP" ON the Languages box reads
                // "Automatic" and is disabled.
                OutlinedTextField(
                    value = "Automatic",
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Languages") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
            } else {
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
            }
            SwitchRow("Based on IP", nav.languagesAuto) { on ->
                profile = profile.copy(fingerprint = fp.copy(navigator = nav.copy(languagesAuto = on)))
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
            SwitchRow("Mask Media Devices", fp.mediaMasking) { on ->
                profile = profile.copy(fingerprint = fp.copy(mediaMasking = on))
            }
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
            SectionTitle("Hardware")
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
            SectionTitle("Storage")
            SwitchRow("Enable Local Storage", browser.localStorage) { on ->
                profile = profile.copy(browser = browser.copy(localStorage = on))
            }
            SwitchRow("Enable Extension storage", browser.extensionStorage) { on ->
                profile = profile.copy(browser = browser.copy(extensionStorage = on))
            }
            SwitchRow("Enable potentially vulnerable plugins", browser.vulnerablePlugins) { on ->
                profile = profile.copy(browser = browser.copy(vulnerablePlugins = on))
            }

            SectionTitle("Other")
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
                    mutableStateOf(profile.browser.startPages.joinToString("\n"))
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

            }
            // ---------- end Advanced accordion ----------

            // ---------- Cookies ----------
            AccordionSection("Cookies") {
            OutlinedButton(onClick = { showCookiesImport = true }) {
                Text("Import Cookies")
            }
            if (profile.cookies.isNotBlank()) {
                Text(
                    "Cookies stored with this profile (${profile.cookies.lines().count { it.isNotBlank() }} lines).",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { save() },
                // Capture §4.2: Create Profile is disabled in Buy dedicated IP mode.
                enabled = !(profile.proxy.mode == "devxyz" && profile.proxy.dedicated),
                modifier = Modifier.fillMaxWidth()
            ) {
                // GoLogin Android button label (Play screenshot): CREATE PROFILE.
                Text(if (isNew) "CREATE PROFILE" else "Save profile")
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
private fun ProfileSummaryCard(profile: Profile, folders: List<Folder>) {
    val fp = profile.fingerprint
    val nav = fp.navigator
    val hw = fp.hardware
    val rows = listOf(
        "Profile name" to profile.name,
        "Folder" to folders.filter { it.id in profile.folderIds }
            .joinToString { it.name }.ifBlank { "No folder" },
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
        "Plugins" to profile.browser.vulnerablePlugins.toString(),
        "User-Agent" to nav.userAgent
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

/**
 * Collapsible editor section — the GoLogin Android "Create profile"
 * screen is a stack of these accordions (Play screenshots 1-2), each
 * with a chevron header.
 */
@Composable
fun AccordionSection(
    title: String,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse section" else "Expand section"
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
