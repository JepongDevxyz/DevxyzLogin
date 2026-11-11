package com.devxyz.login.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.devxyz.login.model.SharedProxy
import kotlinx.coroutines.launch
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Create/Edit profile — v2.3.0 visual rebuild against the REAL GoLogin
 * ANDROID app (ANDROID_UI_SPEC.md, spec screen-0): back arrow (gray) +
 * bold 20sp title; a plain white name field (thin border, 10dp radius,
 * NO floating label, NO "Folder:" line, NO Profile Summary card — that
 * card was a web-editor element); accordion bars (#F6F5F4, 10dp radius,
 * ~56dp, bold 16sp labels, light-gray chevrons) in the order
 * Operating system → Proxy → Timezone → WebRTC → Geolocation →
 * Advanced → Extensions → Bookmarks → Cookies, ALL collapsed by
 * default; fields in the label-above + white-box pattern (small gray
 * label, white rounded box); and a bottom-PINNED solid-green 52dp
 * "CREATE PROFILE" button (8dp radius, white uppercase label).
 * Section contents and behaviour are unchanged from 2.2.0 (web capture
 * GOLOGIN_CAPTURE.md §4 option lists via [CaptureData] / [Countries];
 * OS defaults to Linux and locks after creation).
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
                title = {
                    Text(
                        if (isNew) "Create profile" else "Edit profile",
                        style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack, contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Spec screen-0: the primary action is PINNED at the bottom
            // over a white background — full-width solid-green 52dp
            // button, 8dp radius, white uppercase letter-spaced label.
            Surface(color = MaterialTheme.colorScheme.surface) {
                Button(
                    onClick = { save() },
                    // Capture §4.2: Create Profile is disabled in Buy dedicated IP mode.
                    enabled = !(profile.proxy.mode == "devxyz" && profile.proxy.dedicated),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(52.dp)
                ) {
                    Text(
                        if (isNew) "CREATE PROFILE" else "SAVE PROFILE",
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---------- Name (plain white box, no floating label; the
            // "Folder:" line and the Profile Summary card were removed —
            // both are web-editor elements absent from the real app) --
            LabeledTextField(
                label = null,
                value = profile.name,
                onValueChange = { profile = profile.copy(name = it) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )

            // ---------- Operating system (web editor "Overview") ----------
            AccordionSection("Operating system") {
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
                LabeledTextField(
                    label = "Tags (comma separated)",
                    value = profile.tags.joinToString(", "),
                    onValueChange = { text ->
                        profile = profile.copy(
                            tags = text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                LabeledTextField(
                    label = "Notes",
                    value = profile.notes,
                    onValueChange = { profile = profile.copy(notes = it) },
                    modifier = Modifier.fillMaxWidth(), minLines = 2
                )
            }

            // ---------- Proxy ----------
            AccordionSection("Proxy") {
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
                                        Text("✓ $benefit", color = BrandGreen)
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
                                    color = if (checkFailed) MaterialTheme.colorScheme.error else BrandGreen
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
                            LabeledTextField(
                                label = "IP Address",
                                value = profile.proxy.host,
                                onValueChange = { profile = profile.copy(proxy = profile.proxy.copy(host = it)) },
                                modifier = Modifier.weight(1f), singleLine = true
                            )
                            LabeledTextField(
                                label = "Port",
                                value = if (profile.proxy.port == 0) "" else profile.proxy.port.toString(),
                                onValueChange = { profile = profile.copy(proxy = profile.proxy.copy(port = it.toIntOrNull() ?: 0)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(110.dp), singleLine = true
                            )
                        }
                        LabeledTextField(
                            label = "Proxy Username",
                            value = profile.proxy.username,
                            onValueChange = { profile = profile.copy(proxy = profile.proxy.copy(username = it)) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                        LabeledTextField(
                            label = "Proxy Password",
                            value = profile.proxy.password,
                            onValueChange = { profile = profile.copy(proxy = profile.proxy.copy(password = it)) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                        LabeledTextField(
                            label = "Change IP URL for mobile proxy",
                            value = profile.proxy.changeIpUrl,
                            onValueChange = { profile = profile.copy(proxy = profile.proxy.copy(changeIpUrl = it)) },
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
                                color = if (checkFailed) MaterialTheme.colorScheme.error else BrandGreen
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
                    LabeledTextField(
                        label = "Timezone (IANA, e.g. Asia/Manila)",
                        value = fp.timezone.zone,
                        onValueChange = { profile = profile.copy(fingerprint = fp.copy(timezone = fp.timezone.copy(zone = it))) },
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
                    LabeledTextField(
                        label = "Latitude",
                        value = fp.geolocation.lat.toString(),
                        onValueChange = { v -> profile = profile.copy(fingerprint = fp.copy(geolocation = fp.geolocation.copy(lat = v.toDoubleOrNull() ?: 0.0))) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                    LabeledTextField(
                        label = "Longitude",
                        value = fp.geolocation.lon.toString(),
                        onValueChange = { v -> profile = profile.copy(fingerprint = fp.copy(geolocation = fp.geolocation.copy(lon = v.toDoubleOrNull() ?: 0.0))) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                }
            }
            }

            // ---------- Advanced: Navigator ----------
            AccordionSection("Advanced") {
            SectionTitle("Navigator")
            // Spec screen-0 (Advanced expanded): small gray "User-agent"
            // label ABOVE a white rounded box, GREEN copy icon at the
            // box's right edge.
            FieldLabel("User-agent")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                OutlinedTextField(
                    value = nav.userAgent,
                    onValueChange = { profile = profile.copy(fingerprint = fp.copy(navigator = nav.copy(userAgent = it))) },
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
                        profile = profile.copy(
                            fingerprint = fp.copy(
                                navigator = nav.copy(
                                    languages = text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                )
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
            }
            SwitchRow("Based on IP", nav.languagesAuto) { on ->
                profile = profile.copy(fingerprint = fp.copy(navigator = nav.copy(languagesAuto = on)))
            }
            LabeledTextField(
                label = "Platform",
                value = nav.platform,
                onValueChange = { profile = profile.copy(fingerprint = fp.copy(navigator = nav.copy(platform = it))) },
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
                LabeledTextField(
                    label = "Specified pages (one URL per line)",
                    value = pagesText,
                    onValueChange = {
                        pagesText = it
                        profile = profile.copy(
                            browser = profile.browser.copy(
                                startPages = it.lines().map { l -> l.trim() }.filter { l -> l.isNotEmpty() }
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(), minLines = 2
                )
            }
            LabeledTextField(
                label = "Launch arguments",
                value = browser.launchArguments,
                onValueChange = { profile = profile.copy(browser = browser.copy(launchArguments = it)) },
                placeholder = "Example: --disable-gpu --start-maximized",
                modifier = Modifier.fillMaxWidth(), minLines = 2
            )
            LabeledTextField(
                label = "Custom DNS",
                value = browser.customDns,
                onValueChange = { profile = profile.copy(browser = browser.copy(customDns = it)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )

            }
            // ---------- end Advanced accordion ----------

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
                LabeledTextField(
                    label = "Extension name / ID",
                    value = newExt,
                    onValueChange = { newExt = it },
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
            LabeledTextField(
                label = "Edit Bookmarks — one per line: folder::name::url",
                value = bookmarksText,
                onValueChange = {
                    bookmarksText = it
                    profile = profile.copy(
                        bookmarks = it.lines().map { l -> l.trimEnd() }
                            .filter { l -> l.isNotBlank() }
                    )
                },
                modifier = Modifier.fillMaxWidth(), minLines = 6
            )
            }

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
        }
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
                    profile = profile.copy(cookies = text)
                    showCookiesImport = false
                }) { Text("Import") }
            },
            dismissButton = { TextButton(onClick = { showCookiesImport = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    // Spec screen-0: sub-headers inside an expanded accordion
    // ("Navigator") are bold, ~15sp.
    Text(
        text,
        style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

/**
 * Small gray field label rendered ABOVE its field — the field pattern
 * of the real GoLogin Android editor (spec screen-0: "User-agent",
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
 * Colors for the editor's white-box fields: surface-colored fill (white
 * in light mode), thin light border, primary-green focus border, 10dp
 * radius is set at each field's `shape`.
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
 * Label-above text field in the real app's editor pattern: small gray
 * [label] above a white rounded (10dp) box — no floating label. A null
 * [label] renders the box alone (the profile name field, spec screen-0).
 * The block carries a 10dp bottom gap so stacked fields breathe.
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
 * Collapsible editor section — the real GoLogin Android "Create
 * profile" screen is a stack of these accordions (spec screen-0):
 * light bar (#F6F5F4 via surfaceVariant), ~10dp radius, ~56dp tall,
 * bold 16sp label, light-gray chevron; expanded content renders inside
 * the same card.
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
