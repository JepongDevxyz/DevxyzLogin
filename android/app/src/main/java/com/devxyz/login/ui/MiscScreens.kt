package com.devxyz.login.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.devxyz.login.data.ProfileStore
import com.devxyz.login.model.Folder
import com.devxyz.login.model.Profile
import com.devxyz.login.model.ProxyConfig
import com.devxyz.login.model.SharedProxy
import com.devxyz.login.model.Store

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

/** Proxies page + Buy proxy (GoLogin parity: shared list, add/edit, buy cards). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxiesScreen(
    store: Store,
    onStoreChange: (Store) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("http") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(topBar = { SimpleTopBar("Proxies", onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionTitle("Buy proxy")
            Text("Residential • Mobile • Datacenter — priced per GB. Checkout is not wired to a payment backend in v1; \"Buy\" adds a free datacenter proxy to your list so the flow is testable end-to-end.",
                style = MaterialTheme.typography.bodySmall)
            listOf(
                "Residential" to "$4.50 / GB",
                "Mobile" to "$7.00 / GB",
                "Datacenter" to "$1.20 / GB"
            ).forEach { (kind, price) ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(kind, style = MaterialTheme.typography.titleSmall)
                            Text(price, style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = {
                            val free = SharedProxy(
                                id = ProfileStore.newId(),
                                name = "Free datacenter (US)",
                                proxy = ProxyConfig(mode = "devxyz", type = "http", host = "free-dc.devxyzlogin.local", port = 8080)
                            )
                            onStoreChange(store.copy(proxies = store.proxies + free))
                        }) { Text("Buy") }
                    }
                }
            }

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

/** Personal Settings hub — sections mirror web/Android GoLogin settings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHubScreen(onOpen: (String) -> Unit, onBack: () -> Unit) {
    val sections = listOf(
        "overview" to "Overview",
        "billing" to "Billing plans",
        "members" to "Add members",
        "twofa" to "Two-step verification",
        "sessions" to "Active sessions",
        "restore" to "Restore profiles"
    )
    Scaffold(topBar = { SimpleTopBar("Settings", onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sections.forEach { (key, label) ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { onOpen(key) }, modifier = Modifier.fillMaxWidth()) {
                        Text(label)
                    }
                }
            }
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
    twoFaEnabled: Boolean,
    sessions: List<String>,
    onStoreChange: (Store) -> Unit,
    onEmailChange: (String) -> Unit,
    onPlanChange: (String) -> Unit,
    onTwoFaChange: (Boolean) -> Unit,
    onSessionsChange: (List<String>) -> Unit,
    onBack: () -> Unit
) {
    val title = when (section) {
        "overview" -> "Overview"
        "billing" -> "Billing plans"
        "members" -> "Add members"
        "twofa" -> "Two-step verification"
        "sessions" -> "Active sessions"
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
                "overview" -> {
                    var email by remember { mutableStateOf(accountEmail) }
                    OutlinedTextField(value = email, onValueChange = { email = it },
                        label = { Text("Account email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Button(onClick = { onEmailChange(email) }) { Text("Save") }
                    Text("Current plan: $plan")
                    Text("Profiles used: ${store.profiles.count { it.deletedAt == null }}")
                    Text("Workspaces: ${store.workspaces.joinToString { it.name }}")
                }
                "billing" -> {
                    Text("Current plan: $plan", style = MaterialTheme.typography.titleSmall)
                    listOf(
                        "Forever Free" to "3 profiles • core features",
                        "Professional" to "100 profiles • $24/mo billed annually",
                        "Business" to "300 profiles • $49/mo billed annually",
                        "Enterprise" to "1000 profiles • $99/mo billed annually"
                    ).forEach { (name, desc) ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, style = MaterialTheme.typography.titleSmall)
                                    Text(desc, style = MaterialTheme.typography.bodySmall)
                                }
                                if (plan == name) Text("Current")
                                else Button(onClick = { onPlanChange(name) }) { Text("Select") }
                            }
                        }
                    }
                    var promo by remember { mutableStateOf("") }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = promo, onValueChange = { promo = it },
                            label = { Text("Promo code") }, modifier = Modifier.weight(1f), singleLine = true)
                        TextButton(onClick = { promo = "" }) { Text("Apply") }
                    }
                    Text("Billing history: no invoices yet.", style = MaterialTheme.typography.bodySmall)
                    Text("Note: plan selection is local in v1 — no payment backend is connected.",
                        style = MaterialTheme.typography.bodySmall)
                }
                "members" -> {
                    var memberEmail by remember { mutableStateOf("") }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = memberEmail, onValueChange = { memberEmail = it },
                            label = { Text("Member email") }, modifier = Modifier.weight(1f), singleLine = true)
                        IconButton(onClick = {
                            if (memberEmail.isNotBlank()) {
                                onStoreChange(store.copy(members = store.members + memberEmail.trim()))
                                memberEmail = ""
                            }
                        }) { Icon(Icons.Filled.Add, contentDescription = "Add member") }
                    }
                    if (store.members.isEmpty()) Text("No members yet. Members get workspace access (can view / can edit per folder).")
                    store.members.forEach { member ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(member, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                onStoreChange(store.copy(members = store.members - member))
                            }) { Icon(Icons.Filled.Delete, contentDescription = "Remove member") }
                        }
                    }
                }
                "twofa" -> {
                    SwitchRow("Two-step verification (2FA)", twoFaEnabled) { onTwoFaChange(it) }
                    Text("When on, a verification code is required at sign-in. Code delivery is a backend feature; the toggle state is stored locally in v1.",
                        style = MaterialTheme.typography.bodySmall)
                }
                "sessions" -> {
                    if (sessions.isEmpty()) Text("No active sessions.")
                    sessions.forEach { session ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(session, modifier = Modifier.weight(1f))
                            TextButton(onClick = { onSessionsChange(sessions - session) }) { Text("Revoke") }
                        }
                    }
                }
                "restore" -> {
                    val deleted = store.profiles.filter { it.deletedAt != null }
                    if (deleted.isEmpty()) Text("No deleted profiles.")
                    deleted.forEach { profile: Profile ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(profile.name, modifier = Modifier.weight(1f))
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
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
