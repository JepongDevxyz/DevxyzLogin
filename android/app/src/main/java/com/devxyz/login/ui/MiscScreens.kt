package com.devxyz.login.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devxyz.login.data.ProfileStore
import com.devxyz.login.model.CaptureData
import com.devxyz.login.model.DefaultProfileSettings
import com.devxyz.login.model.Folder
import com.devxyz.login.model.OsOptions
import com.devxyz.login.model.Profile
import com.devxyz.login.model.ProxyConfig
import com.devxyz.login.model.SharedProxy
import com.devxyz.login.model.Store
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
        }
    )
}

/** Folders manager: create / rename / delete / rearrange (move up-down). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    store: Store,
    onStoreChange: (Store) -> Unit,
    onBack: () -> Unit
) {
    var newName by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<Folder?>(null) }
    var renameText by remember { mutableStateOf("") }

    Scaffold(topBar = { SimpleTopBar("Folders manager", onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newName, onValueChange = { newName = it },
                    label = { Text("New folder name") },
                    modifier = Modifier.weight(1f), singleLine = true
                )
                IconButton(onClick = {
                    if (newName.isNotBlank()) {
                        onStoreChange(store.copy(folders = store.folders + Folder(ProfileStore.newId(), newName.trim())))
                        newName = ""
                    }
                }) { Icon(Icons.Filled.Add, contentDescription = "Add folder") }
            }
            store.folders.forEachIndexed { index, folder ->
                val count = store.profiles.count { it.deletedAt == null && folder.id in it.folderIds }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(folder.name, style = MaterialTheme.typography.titleSmall)
                            Text("$count profile(s)", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(
                            enabled = index > 0,
                            onClick = {
                                val list = store.folders.toMutableList()
                                val tmp = list[index - 1]; list[index - 1] = list[index]; list[index] = tmp
                                onStoreChange(store.copy(folders = list))
                            }
                        ) { Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up") }
                        IconButton(
                            enabled = index < store.folders.lastIndex,
                            onClick = {
                                val list = store.folders.toMutableList()
                                val tmp = list[index + 1]; list[index + 1] = list[index]; list[index] = tmp
                                onStoreChange(store.copy(folders = list))
                            }
                        ) { Icon(Icons.Filled.ArrowDownward, contentDescription = "Move down") }
                        TextButton(onClick = { renameTarget = folder; renameText = folder.name }) { Text("Rename") }
                        IconButton(onClick = {
                            // Deleting a folder also unlinks it from profiles.
                            val cleaned = store.profiles.map { p -> p.copy(folderIds = p.folderIds - folder.id) }
                            onStoreChange(store.copy(
                                folders = store.folders - folder,
                                profiles = cleaned
                            ))
                        }) { Icon(Icons.Filled.Delete, contentDescription = "Delete folder") }
                    }
                }
            }
            if (store.folders.isEmpty()) Text("No folders yet.")
        }
    }

    renameTarget?.let { folder ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename folder") },
            text = {
                OutlinedTextField(value = renameText, onValueChange = { renameText = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameText.isNotBlank()) {
                        onStoreChange(store.copy(folders = store.folders.map {
                            if (it.id == folder.id) it.copy(name = renameText.trim()) else it
                        }))
                    }
                    renameTarget = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Cancel") } }
        )
    }
}

/** Proxies page (capture §8): tabs, 6 pastel cards, shared proxy list, add form, device-as-proxy modal. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxiesScreen(
    store: Store,
    onStoreChange: (Store) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf("all") }
    var addMenuOpen by remember { mutableStateOf(false) }
    var showDeviceModal by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("http") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    fun buyPlaceholder(kind: String) {
        val entry = SharedProxy(
            id = ProfileStore.newId(),
            name = "$kind (pending checkout)",
            proxy = ProxyConfig(mode = "devxyz", type = "http", host = "geo.devxyzlogin.local", port = 10080)
        )
        onStoreChange(store.copy(proxies = store.proxies + entry))
        Toast.makeText(context, "$kind proxy reserved — checkout runs through the DevxyzLogin server", Toast.LENGTH_SHORT).show()
    }

    Scaffold(topBar = { SimpleTopBar("Proxies", onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                listOf("all" to "All proxies", "errors" to "Errors", "archive" to "Archive")
                    .forEach { (key, label) ->
                        FilterChip(
                            selected = tab == key,
                            onClick = { tab = key },
                            label = { Text(label) }
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                Spacer(Modifier.weight(1f))
                Box {
                    OutlinedButton(onClick = { addMenuOpen = true }) { Text("+ Add Proxy") }
                    DropdownMenu(expanded = addMenuOpen, onDismissRequest = { addMenuOpen = false }) {
                        DropdownMenuItem(text = { Text("Residential proxy") },
                            onClick = { addMenuOpen = false; buyPlaceholder("Residential") })
                        DropdownMenuItem(text = { Text("Custom proxy") },
                            onClick = {
                                addMenuOpen = false
                                Toast.makeText(context, "Use the Add proxy form below", Toast.LENGTH_SHORT).show()
                            })
                        DropdownMenuItem(text = { Text("Import Proxies") },
                            onClick = {
                                addMenuOpen = false
                                Toast.makeText(context, "Paste proxies one per line in the form below (host:port)", Toast.LENGTH_SHORT).show()
                            })
                        DropdownMenuItem(text = { Text("My device as proxy") },
                            onClick = { addMenuOpen = false; showDeviceModal = true })
                    }
                }
            }

            // Six GoLogin cards (capture §8)
            ProxyKindCard(
                title = "Residential", action = "+ Buy",
                badge = "0.49 GB ⚠", badgeColor = Color(0xFFD97706),
                desc = "Residential proxies that look like real home internet.",
                container = Color(0xFFDCEBFD)
            ) { buyPlaceholder("Residential") }
            ProxyKindCard(
                title = "Mobile", action = "+ Buy", badge = null, badgeColor = BrandTeal,
                desc = "Show up like a local using fresh mobile IP addresses.",
                container = Color(0xFFE9E4FC)
            ) { buyPlaceholder("Mobile") }
            ProxyKindCard(
                title = "Data Center", action = "+ Buy", badge = null, badgeColor = BrandTeal,
                desc = "Stable, cost-efficient data center IPs when local presence isn't needed.",
                container = Color(0xFFFBE3EC)
            ) { buyPlaceholder("Data Center") }
            ProxyKindCard(
                title = "Dedicated IP", action = "+ Buy", badge = null, badgeColor = BrandTeal,
                desc = "Private IP for stable fingerprints, saved sessions, and long-term tasks.",
                container = Color(0xFFEFF1F4)
            ) { buyPlaceholder("Dedicated IP") }
            ProxyKindCard(
                title = "Custom proxy", action = "+ Add", badge = null, badgeColor = BrandTeal,
                desc = "Bring your own proxy of any type and use it in your profiles.",
                container = Color(0xFFEFF1F4)
            ) {
                Toast.makeText(context, "Use the Add proxy form below", Toast.LENGTH_SHORT).show()
            }
            ProxyKindCard(
                title = "Device as proxy", action = "+ New device", badge = null, badgeColor = BrandTeal,
                desc = "Keep your IP address and use proxy traffic to connect to the Internet.",
                container = Color(0xFFEFF1F4)
            ) { showDeviceModal = true }

            Spacer(Modifier.height(8.dp))
            SectionTitle("Your proxies")
            if (store.proxies.isEmpty()) Text("No shared proxies yet.")
            store.proxies.forEach { shared ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(shared.name, style = MaterialTheme.typography.titleSmall)
                            Text(shared.proxy.listLabel, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            "● Connected",
                            color = BrandTeal,
                            style = MaterialTheme.typography.bodySmall
                        )
                        IconButton(onClick = {
                            onStoreChange(store.copy(proxies = store.proxies - shared))
                        }) { Icon(Icons.Filled.Delete, contentDescription = "Delete proxy") }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            SectionTitle("Add proxy")
            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            DropdownField(
                label = "Type", value = type,
                options = listOf("http" to "HTTP", "https" to "HTTPS", "socks4" to "SOCKS4", "socks5" to "SOCKS5"),
                onSelect = { type = it }
            )
            OutlinedTextField(value = host, onValueChange = { host = it },
                label = { Text("Host") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = port, onValueChange = { port = it },
                label = { Text("Port") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = username, onValueChange = { username = it },
                label = { Text("Login (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = password, onValueChange = { password = it },
                label = { Text("Password (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Button(
                onClick = {
                    if (host.isNotBlank() && (port.toIntOrNull() ?: 0) > 0) {
                        val entry = SharedProxy(
                            id = ProfileStore.newId(),
                            name = name.ifBlank { host },
                            proxy = ProxyConfig(mode = "custom", type = type, host = host.trim(),
                                port = port.toInt(), username = username, password = password)
                        )
                        onStoreChange(store.copy(proxies = store.proxies + entry))
                        name = ""; host = ""; port = ""; username = ""; password = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Add proxy") }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDeviceModal) {
        AlertDialog(
            onDismissRequest = { showDeviceModal = false },
            title = { Text("Add Android device") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. Download DevxyzLogin Connect App")
                    Text("2. Register in mobile app")
                    Text("3. Device will appear in your device list automatically")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEFF1F4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("QR code", color = Color(0xFF6B7280))
                    }
                    Text("GET IT ON Google Play", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = { showDeviceModal = false }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun ProxyKindCard(
    title: String,
    action: String,
    badge: String?,
    badgeColor: Color,
    desc: String,
    container: Color,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = Color(0xFF111827))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                if (badge != null) {
                    Text(badge, color = badgeColor, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(8.dp))
                }
                TextButton(onClick = onAction) { Text(action, color = BrandTeal) }
            }
            Text(desc, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Support screen: real actions (email + docs link). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Scaffold(topBar = { SimpleTopBar("Support", onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Need help with DevxyzLogin?")
            Button(onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:support@devxyzlogin.local")
                    putExtra(Intent.EXTRA_SUBJECT, "DevxyzLogin support")
                }
                context.startActivity(intent)
            }, modifier = Modifier.fillMaxWidth()) { Text("Email support") }
            OutlinedButton(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://gologin.com/docs")))
            }, modifier = Modifier.fillMaxWidth()) { Text("Open documentation (GoLogin reference)") }
        }
    }
}

/**
 * Personal Area hub — order matches the captured GoLogin sub-menu
 * (§12): Overview, Default profile settings, Affiliate program,
 * Active sessions, API, Vault chrome extension, Cloud Browser Usage,
 * then the "Workspace" section: Settings & members, Billing Plans,
 * Restore profiles. NOTE: GoLogin has NO two-factor setting anywhere —
 * v1's invented 2FA page is removed; the Vault page replaces it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHubScreen(onOpen: (String) -> Unit, onBack: () -> Unit) {
    val personal = listOf(
        "overview" to "Overview",
        "defaults" to "Default profile settings",
        "affiliate" to "Affiliate program",
        "sessions" to "Active sessions",
        "api" to "API",
        "vault" to "Vault chrome extension",
        "usage" to "Cloud Browser Usage"
    )
    val workspace = listOf(
        "members" to "Settings & members",
        "billing" to "Billing Plans",
        "restore" to "Restore profiles"
    )
    Scaffold(topBar = { SimpleTopBar("Settings", onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            personal.forEach { (key, label) -> SettingsHubRow(label) { onOpen(key) } }
            Text(
                "Workspace",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )
            workspace.forEach { (key, label) -> SettingsHubRow(label) { onOpen(key) } }
        }
    }
}

@Composable
private fun SettingsHubRow(label: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(label)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSectionScreen(
    section: String,
    store: Store,
    accountEmail: String,
    plan: String,
    sessions: List<String>,
    themeMode: String,
    uiLanguage: String,
    defaults: DefaultProfileSettings,
    onStoreChange: (Store) -> Unit,
    onEmailChange: (String) -> Unit,
    onPlanChange: (String) -> Unit,
    onSessionsChange: (List<String>) -> Unit,
    onThemeChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onDefaultsChange: (DefaultProfileSettings) -> Unit,
    onBack: () -> Unit
) {
    val title = when (section) {
        "overview" -> "Overview"
        "defaults" -> "Default profile settings"
        "affiliate" -> "Affiliate Program"
        "sessions" -> "Active sessions"
        "api" -> "API"
        "vault" -> "Vault password manager"
        "usage" -> "Cloud Browser Usage"
        "members" -> "Settings & members"
        "billing" -> "Billing Plans"
        "restore" -> "Restore profiles"
        else -> "Settings"
    }
    Scaffold(topBar = { SimpleTopBar(title, onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (section) {
                "overview" -> OverviewSection(
                    store = store,
                    accountEmail = accountEmail,
                    themeMode = themeMode,
                    uiLanguage = uiLanguage,
                    onEmailChange = onEmailChange,
                    onThemeChange = onThemeChange,
                    onLanguageChange = onLanguageChange
                )
                "defaults" -> DefaultsSection(defaults, onDefaultsChange)
                "affiliate" -> AffiliateSection()
                "sessions" -> SessionsSection(sessions, onSessionsChange)
                "api" -> ApiSection()
                "vault" -> VaultSection()
                "usage" -> UsageSection()
                "members" -> MembersSection(store, accountEmail, onStoreChange)
                "billing" -> BillingSection(plan, onPlanChange)
                "restore" -> RestoreSection(store, onStoreChange)
                else -> Text("Unknown section.")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun OverviewSection(
    store: Store,
    accountEmail: String,
    themeMode: String,
    uiLanguage: String,
    onEmailChange: (String) -> Unit,
    onThemeChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf(accountEmail) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    Text("Account e-mail", style = MaterialTheme.typography.bodySmall)
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = email, onValueChange = { email = it },
            modifier = Modifier.weight(1f), singleLine = true
        )
        TextButton(onClick = { onEmailChange(email) }) { Text("✎ Save") }
    }
    Text("Password", style = MaterialTheme.typography.bodySmall)
    OutlinedButton(onClick = { showPasswordDialog = true }) { Text("Change your password") }
    DropdownField(
        label = "Select language",
        value = uiLanguage,
        options = CaptureData.uiLanguages,
        onSelect = onLanguageChange
    )
    Text("Transfer profiles to Gologin", style = MaterialTheme.typography.bodySmall)
    OutlinedButton(onClick = {
        Toast.makeText(context, "Transfer runs through the DevxyzLogin server", Toast.LENGTH_SHORT).show()
    }) { Text("Start") }
    DropdownField(
        label = "Interface theme",
        value = themeMode,
        options = listOf("system" to "System", "light" to "Light", "dark" to "Dark"),
        onSelect = onThemeChange
    )
    DropdownField(
        label = "Default workspace when transferring profiles to you",
        value = store.workspaces.firstOrNull()?.name ?: "",
        options = store.workspaces.map { it.name to it.name },
        onSelect = { }
    )
    Text("Profiles used: ${store.profiles.count { it.deletedAt == null }}")

    if (showPasswordDialog) {
        var current by remember { mutableStateOf("") }
        var newPass by remember { mutableStateOf("") }
        var confirm by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Change your password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = current, onValueChange = { current = it },
                        label = { Text("Your password") }, singleLine = true)
                    OutlinedTextField(value = newPass, onValueChange = { newPass = it },
                        label = { Text("New password") }, singleLine = true)
                    OutlinedTextField(value = confirm, onValueChange = { confirm = it },
                        label = { Text("Confirm new password") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    showPasswordDialog = false
                    Toast.makeText(
                        context,
                        if (newPass == confirm && newPass.isNotBlank()) "Password changed (local)"
                        else "Passwords do not match",
                        Toast.LENGTH_SHORT
                    ).show()
                }, modifier = Modifier.fillMaxWidth()) { Text("Submit") }
            },
            dismissButton = { TextButton(onClick = { showPasswordDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun DefaultsSection(
    defaults: DefaultProfileSettings,
    onDefaultsChange: (DefaultProfileSettings) -> Unit
) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { onDefaultsChange(DefaultProfileSettings()) }) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Restore default profile settings")
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = {
            Toast.makeText(context, "Templates are provided by the DevxyzLogin server", Toast.LENGTH_SHORT).show()
        }) { Text("From templates") }
    }
    SwitchRow("Language — Auto (based on IP)", defaults.languageAuto) {
        onDefaultsChange(defaults.copy(languageAuto = it))
    }
    Text(
        "Accept-Language header: en-PH,en;q=0.9,en-US;q=0.8",
        style = MaterialTheme.typography.bodySmall
    )
    DropdownField(
        label = "Minimal screen resolution",
        value = defaults.minResolution,
        options = CaptureData.screenResolutions.map { it to it },
        onSelect = { onDefaultsChange(defaults.copy(minResolution = it)) }
    )
    DropdownField(
        label = "Maximum screen resolution",
        value = defaults.maxResolution,
        options = CaptureData.screenResolutions.map { it to it },
        onSelect = { onDefaultsChange(defaults.copy(maxResolution = it)) }
    )
    Text("Choose OS randomly", style = MaterialTheme.typography.bodySmall)
    RadioChips(
        options = listOf("device" to "Device OS") + OsOptions.all,
        value = defaults.osMode,
        onSelect = { onDefaultsChange(defaults.copy(osMode = it)) }
    )
    Text("Profiles will be distributed evenly", style = MaterialTheme.typography.bodySmall)
    OutlinedTextField(
        value = defaults.profileNameTemplate,
        onValueChange = { onDefaultsChange(defaults.copy(profileNameTemplate = it)) },
        label = { Text("Profile name") },
        modifier = Modifier.fillMaxWidth(), singleLine = true
    )
    SectionTitle("Proxy & Location")
    RadioChips(
        options = listOf("devxyz" to "DevxyzLogin proxy", "none" to "Without proxy"),
        value = defaults.proxyMode,
        onSelect = { onDefaultsChange(defaults.copy(proxyMode = it)) }
    )
    OutlinedTextField(
        value = defaults.customDns,
        onValueChange = { onDefaultsChange(defaults.copy(customDns = it)) },
        label = { Text("Custom DNS") },
        placeholder = { Text("Custom DNS") },
        modifier = Modifier.fillMaxWidth(), singleLine = true
    )
    SwitchRow("Disable image loading", defaults.disableImageLoading) {
        onDefaultsChange(defaults.copy(disableImageLoading = it))
    }
    SwitchRow("Disable video autoplay", defaults.disableVideoAutoplay) {
        onDefaultsChange(defaults.copy(disableVideoAutoplay = it))
    }
    SectionTitle("Browser")
    DropdownField(
        label = "Browser version for new profiles",
        value = defaults.browserVersion,
        options = CaptureData.browserVersionMajors.map { it to CaptureData.browserVersionLabel(it) },
        onSelect = { onDefaultsChange(defaults.copy(browserVersion = it)) }
    )
    DropdownField(
        label = "Start URL",
        value = defaults.startUrl,
        options = CaptureData.startUrlOptions,
        onSelect = { onDefaultsChange(defaults.copy(startUrl = it)) }
    )
    var bookmarksText by remember(defaults.bookmarksText) { mutableStateOf(defaults.bookmarksText) }
    OutlinedTextField(
        value = bookmarksText,
        onValueChange = {
            bookmarksText = it
            onDefaultsChange(defaults.copy(bookmarksText = it))
        },
        label = { Text("Bookmarks") },
        modifier = Modifier.fillMaxWidth(), minLines = 6
    )
    OutlinedTextField(
        value = defaults.launchArguments,
        onValueChange = { onDefaultsChange(defaults.copy(launchArguments = it)) },
        label = { Text("Launch arguments") },
        placeholder = { Text("Example: --disable-gpu --start-maximized") },
        modifier = Modifier.fillMaxWidth(), minLines = 2
    )
    SectionTitle("Storage")
    SwitchRow("Enable browser history saving", defaults.saveHistory) {
        onDefaultsChange(defaults.copy(saveHistory = it))
    }
    SwitchRow("Enable browser bookmark saving", defaults.saveBookmarks) {
        onDefaultsChange(defaults.copy(saveBookmarks = it))
    }
    SwitchRow("Enable password saving", defaults.savePasswords) {
        onDefaultsChange(defaults.copy(savePasswords = it))
    }
    SwitchRow("Enable Local Storage", defaults.localStorage) {
        onDefaultsChange(defaults.copy(localStorage = it))
    }
    SwitchRow("Enable IndexedDB", defaults.indexedDb) {
        onDefaultsChange(defaults.copy(indexedDb = it))
    }
    SwitchRow("Enable Extension storage", defaults.extensionStorage) {
        onDefaultsChange(defaults.copy(extensionStorage = it))
    }
    SwitchRow("System Extensions", defaults.systemExtensions) {
        onDefaultsChange(defaults.copy(systemExtensions = it))
    }
}

@Composable
private fun AffiliateSection() {
    val context = LocalContext.current
    Text("Recommend DevxyzLogin to your friends and get 10% of each purchase they've made.")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Withdrawn" to "$0", "Earned" to "$0", "Available" to "$0", "Affiliate" to "10%")
            .forEach { (label, value) ->
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(label, style = MaterialTheme.typography.bodySmall)
                        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
    }
    SectionTitle("Store")
    var prefix by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("https://devxyzlogin.com/join/") }
    Text(link, style = MaterialTheme.typography.bodySmall)
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = prefix, onValueChange = { prefix = it },
            label = { Text("Your prefix") },
            modifier = Modifier.weight(1f), singleLine = true
        )
        Spacer(Modifier.width(8.dp))
        Button(onClick = {
            link = "https://devxyzlogin.com/join/" + prefix.trim()
            Toast.makeText(context, "Referral link created", Toast.LENGTH_SHORT).show()
        }) { Text("Create referral link") }
    }
    Row {
        listOf("#", "Link", "Clicks", "Registrations", "Buyers", "Sum amount").forEach {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 10.dp)
            )
        }
    }
    Text("No referrals yet.", style = MaterialTheme.typography.bodySmall)
    Row {
        TextButton(onClick = { }) { Text("Payment history >") }
        Spacer(Modifier.weight(1f))
        Button(onClick = {
            Toast.makeText(context, "Nothing to withdraw yet", Toast.LENGTH_SHORT).show()
        }) { Text("Withdrawal") }
    }
}

@Composable
private fun SessionsSection(
    sessions: List<String>,
    onSessionsChange: (List<String>) -> Unit
) {
    Text("This device", style = MaterialTheme.typography.titleSmall)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("DevxyzLogin Android", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("online", color = BrandTealLink, style = MaterialTheme.typography.bodySmall)
            }
            Text("Android • active now", style = MaterialTheme.typography.bodySmall)
            Text("Logs out all devices except for this one.", style = MaterialTheme.typography.bodySmall)
        }
    }
    sessions.filter { !it.contains("This device") }.forEach { session ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(session, modifier = Modifier.weight(1f))
            TextButton(onClick = { onSessionsChange(sessions - session) }) { Text("Revoke") }
        }
    }
}

@Composable
private fun ApiSection() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("devxyz_api", Context.MODE_PRIVATE)
    }
    var tokens by remember {
        mutableStateOf(
            (prefs.getString("tokens", "") ?: "")
                .lines().filter { it.isNotBlank() }
                .mapNotNull {
                    val parts = it.split("|")
                    if (parts.size == 3) Triple(parts[0], parts[1], parts[2]) else null
                }
        )
    }
    fun persist(next: List<Triple<String, String, String>>) {
        tokens = next
        prefs.edit().putString(
            "tokens",
            next.joinToString("\n") { "${it.first}|${it.second}|${it.third}" }
        ).apply()
    }
    Text(
        "You can connect to API and integrate DevxyzLogin with other programs to, " +
            "for instance, automate some processes by using self-written bots. Keep in mind " +
            "that you must be able to program on your own to use our API."
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Postman", "GitHub", "MCP", "API").forEach { label ->
            OutlinedButton(onClick = {
                if (label == "MCP") {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/gologinapp/gologin-mcp"))
                    )
                } else {
                    Toast.makeText(context, "$label resources open on the DevxyzLogin server", Toast.LENGTH_SHORT).show()
                }
            }) { Text(label) }
        }
    }
    SectionTitle("Access tokens")
    if (tokens.isEmpty()) Text("No tokens yet.")
    tokens.forEachIndexed { index, (name, token, created) ->
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${index + 1}. $name", style = MaterialTheme.typography.titleSmall)
                    Text(
                        token.take(10) + "••••••••••" + token.takeLast(4),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(created, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { persist(tokens - tokens[index]) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete token")
                }
            }
        }
    }
    Button(onClick = {
        val alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val raw = (1..40).map { alphabet[Random.nextInt(alphabet.length)] }.joinToString("")
        val created = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date())
        persist(tokens + Triple("Token ${tokens.size + 1}", "dl1_$raw", created))
    }) { Text("New Token") }
}

@Composable
private fun VaultSection() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("devxyz_prefs", Context.MODE_PRIVATE)
    }
    var enabled by remember { mutableStateOf(prefs.getBoolean("vault_ext", true)) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Vault chrome extension", modifier = Modifier.weight(1f))
        Switch(
            checked = enabled,
            onCheckedChange = {
                enabled = it
                prefs.edit().putBoolean("vault_ext", it).apply()
            }
        )
    }
    Text(
        "The Vault password manager extension is offered inside DevxyzLogin profiles.",
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun UsageSection() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            "Total launches" to "0",
            "Total hours" to "0.0",
            "Hours limit" to "300",
            "Max parallel launches" to "3"
        ).forEach { (label, value) ->
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(label, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Current period usage", modifier = Modifier.weight(1f))
        Text("0.0 / 300 h", fontWeight = FontWeight.Bold)
    }
    LinearProgressIndicator(progress = { 0f }, modifier = Modifier.fillMaxWidth())
    Text("Daily usage", style = MaterialTheme.typography.titleSmall)
    OutlinedButton(onClick = { }, enabled = false) { Text("Export CSV") }
    Text("No data", style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun MembersSection(
    store: Store,
    accountEmail: String,
    onStoreChange: (Store) -> Unit
) {
    val workspace = store.workspaces.firstOrNull()
    Text(workspace?.name ?: "My workspace", style = MaterialTheme.typography.titleMedium)
    Text("${store.members.size + 1}/${store.members.size + 1} users joined", style = MaterialTheme.typography.bodySmall)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text("Profiles Cloud Sync", modifier = Modifier.weight(1f))
        Switch(checked = true, onCheckedChange = null, enabled = false)
    }
    Text("Access to workspace — manage members here or invite new members below.")
    var memberEmail by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = memberEmail, onValueChange = { memberEmail = it },
            label = { Text("Member email") },
            modifier = Modifier.weight(1f), singleLine = true
        )
        IconButton(onClick = {
            if (memberEmail.isNotBlank()) {
                onStoreChange(store.copy(members = store.members + memberEmail.trim()))
                memberEmail = ""
            }
        }) { Icon(Icons.Filled.Add, contentDescription = "Add member") }
    }
    if (accountEmail.isNotBlank()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(accountEmail, modifier = Modifier.weight(1f))
            Text("Owner", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    store.members.forEach { member ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(member, modifier = Modifier.weight(1f))
            IconButton(onClick = {
                onStoreChange(store.copy(members = store.members - member))
            }) { Icon(Icons.Filled.Delete, contentDescription = "Remove member") }
        }
    }
    OutlinedButton(onClick = { }) { Text("Delete workspace") }
}

private data class PlanInfo(
    val name: String,
    val profiles: String,
    val monthly: Double,
    val popular: Boolean = false,
    val dark: Boolean = false
)

/** Billing Plans with capture §13 pricing: monthly 9/99/179/279, annual = half. */
@Composable
private fun BillingSection(plan: String, onPlanChange: (String) -> Unit) {
    var annual by remember { mutableStateOf(true) } // GoLogin defaults to "Annually 50% discount"
    Text("Current plan: $plan", style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(selected = !annual, onClick = { annual = false }, label = { Text("Monthly") })
        FilterChip(
            selected = annual, onClick = { annual = true },
            label = { Text("Annually 50% discount") }
        )
    }
    val plans = listOf(
        PlanInfo("Professional", "10 profiles", 9.0),
        PlanInfo("Business", "300 profiles", 99.0, popular = true),
        PlanInfo("Enterprise", "1000 profiles", 179.0),
        PlanInfo("Custom", "2000 profiles", 279.0, dark = true)
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Free", style = MaterialTheme.typography.titleSmall)
            Text("3 profiles • all benefits except Profile Sharing, Cloud Launch and Team Members",
                style = MaterialTheme.typography.bodySmall)
            if (plan == "Free") Text("Current", color = BrandTeal)
            else Button(onClick = { onPlanChange("Free") }) { Text("Try Free plan") }
        }
    }
    plans.forEach { p ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = if (p.dark) CardDefaults.cardColors(
                containerColor = Color(0xFF2B2F36), contentColor = Color.White
            ) else CardDefaults.cardColors()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    if (p.popular) {
                        Text(
                            "Popular plan",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(BrandTeal)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                val price = if (annual) p.monthly / 2 else p.monthly
                Text(
                    "$${trimPrice(price)}/mo" + if (annual) " (was $${trimPrice(p.monthly)}/mo)" else "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("✓ ${p.profiles}", style = MaterialTheme.typography.bodySmall)
                Text("✓ 24/7 expert support", style = MaterialTheme.typography.bodySmall)
                Text("Members: 1 • Gb resident proxy: 2", style = MaterialTheme.typography.bodySmall)
                if (plan == p.name) Text("Current", color = BrandTeal)
                else Button(onClick = { onPlanChange(p.name) }) { Text("Try 7 days for free") }
                Text("No card required", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    SectionTitle("Payment history")
    Text("No payments yet.", style = MaterialTheme.typography.bodySmall)
    SectionTitle("Coupon")
    var coupon by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = coupon, onValueChange = { coupon = it },
            label = { Text("Enter a code") },
            modifier = Modifier.weight(1f), singleLine = true
        )
        Spacer(Modifier.width(8.dp))
        Button(onClick = { coupon = "" }, enabled = coupon.isNotBlank()) { Text("Apply") }
    }
    SectionTitle("Invoice details")
    var fullName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var vat by remember { mutableStateOf("") }
    OutlinedTextField(value = fullName, onValueChange = { fullName = it },
        label = { Text("Full name or company name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    OutlinedTextField(value = address, onValueChange = { address = it },
        label = { Text("Address") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    OutlinedTextField(value = vat, onValueChange = { vat = it },
        label = { Text("VAT or TaxId") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    OutlinedButton(onClick = { }) { Text("Apply") }
    Text(
        "Note: plan selection is local — checkout and payments run through the DevxyzLogin server.",
        style = MaterialTheme.typography.bodySmall
    )
}

private fun trimPrice(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()

@Composable
private fun RestoreSection(store: Store, onStoreChange: (Store) -> Unit) {
    Text(
        "Profiles that have been here for 120 hours will be automatically deleted.",
        style = MaterialTheme.typography.bodySmall
    )
    val deleted = store.profiles.filter { it.deletedAt != null }
    if (deleted.isEmpty()) {
        Text(
            "No profiles yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 24.dp)
        )
    }
    deleted.forEach { profile: Profile ->
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(profile.name)
                    Text(
                        "Deleted at ${profile.deletedAt ?: ""}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(onClick = {
                    onStoreChange(store.copy(profiles = store.profiles.map {
                        if (it.id == profile.id) it.copy(deletedAt = null, updatedAt = ProfileStore.nowIso()) else it
                    }))
                }) { Text("Restore") }
                TextButton(onClick = {
                    onStoreChange(store.copy(profiles = store.profiles - profile))
                }) { Text("Delete forever") }
            }
        }
    }
}
