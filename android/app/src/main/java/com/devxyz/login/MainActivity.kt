package com.devxyz.login

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Support
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devxyz.login.browser.BrowserActivityBase
import com.devxyz.login.data.ProfileStore
import com.devxyz.login.model.DefaultBookmarks
import com.devxyz.login.model.DefaultProfileSettings
import com.devxyz.login.model.FingerprintGenerator
import com.devxyz.login.model.Profile
import com.devxyz.login.model.Store
import com.devxyz.login.model.Workspace
import com.devxyz.login.ui.DevxyzTheme
import com.devxyz.login.ui.FoldersScreen
import com.devxyz.login.ui.ProfileEditScreen
import com.devxyz.login.ui.ProfileListScreen
import com.devxyz.login.ui.ProxiesScreen
import com.devxyz.login.ui.SettingsHubScreen
import com.devxyz.login.ui.SettingsSectionScreen
import com.devxyz.login.ui.SupportScreen
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed class Screen {
    data object List : Screen()
    data class Edit(val profile: Profile, val isNew: Boolean) : Screen()
    data object Folders : Screen()
    data object Proxies : Screen()
    data object Support : Screen()
    data object SettingsHub : Screen()
    data class SettingsSection(val key: String) : Screen()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val profileStore = ProfileStore(this)
        setContent {
            DevxyzLoginApp(profileStore, this)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevxyzLoginApp(profileStore: ProfileStore, context: Context) {
    var store by remember { mutableStateOf(profileStore.load()) }
    var screen by remember { mutableStateOf<Screen>(Screen.List) }

    val prefs = remember { context.getSharedPreferences("devxyz_prefs", Context.MODE_PRIVATE) }
    var accountEmail by remember { mutableStateOf(prefs.getString("email", "user@devxyzlogin.local") ?: "") }
    var plan by remember { mutableStateOf(prefs.getString("plan", "Free") ?: "Free") }
    var themeMode by remember { mutableStateOf(prefs.getString("theme", "system") ?: "system") }
    var uiLanguage by remember { mutableStateOf(prefs.getString("ui_language", "en") ?: "en") }
    var defaults by remember {
        mutableStateOf(
            try {
                Json { ignoreUnknownKeys = true }
                    .decodeFromString<DefaultProfileSettings>(
                        prefs.getString("default_settings", "") ?: ""
                    )
            } catch (e: Exception) {
                DefaultProfileSettings()
            }
        )
    }
    var sessions by remember {
        mutableStateOf(
            (prefs.getString("sessions", "This device • Android • active now") ?: "")
                .split("\n").filter { it.isNotBlank() }
        )
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showAddWorkspace by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var newWorkspaceName by remember { mutableStateOf("") }

    fun updateStore(next: Store) {
        store = next
        profileStore.save(next)
    }

    fun navigate(target: Screen) {
        screen = target
        scope.launch { drawerState.close() }
    }

    fun newProfile(): Profile {
        val now = ProfileStore.nowIso()
        // GoLogin editor parity (capture §4): name auto-increments
        // "profile N", OS default is Linux, bookmarks start from the
        // editor-variant default list.
        val nextNumber = store.profiles.count { it.deletedAt == null } + 1
        return Profile(
            id = ProfileStore.newId(),
            name = "profile $nextNumber",
            os = "linux",
            fingerprint = FingerprintGenerator.generate("linux"),
            bookmarks = DefaultBookmarks.editorLines,
            createdAt = now, updatedAt = now
        )
    }

    fun runProfile(profile: Profile, apiMode: Boolean = false) {
        updateStore(store.copy(profiles = store.profiles.map {
            if (it.id == profile.id) it.copy(status = "running", updatedAt = ProfileStore.nowIso()) else it
        }))
        val slot = profileStore.slotFor(profile.id)
        context.startActivity(BrowserActivityBase.intentFor(context, slot, profile.id, apiMode))
    }

    fun stopProfile(profile: Profile) {
        updateStore(store.copy(profiles = store.profiles.map {
            if (it.id == profile.id) it.copy(status = "ready", updatedAt = ProfileStore.nowIso()) else it
        }))
        BrowserActivityBase.stopSlot(context, profileStore.slotFor(profile.id))
    }

    DevxyzTheme(themeMode = themeMode) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                // Account header: green square avatar + workspace name
                // (GoLogin workspace-switcher parity, capture §7)
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(9.dp))
                                .background(com.devxyz.login.ui.BrandTeal),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text(
                                (store.workspaces.firstOrNull()?.name ?: "D")
                                    .firstOrNull()?.uppercase() ?: "D",
                                color = androidx.compose.ui.graphics.Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(Modifier.padding(6.dp))
                        Column {
                            Text(
                                store.workspaces.firstOrNull()?.name ?: "My workspace",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                accountEmail.ifBlank { "Not signed in" },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "$plan · ${store.profiles.count { it.deletedAt == null }} profiles",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                HorizontalDivider()
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Upgrade, contentDescription = null) },
                    label = { Text("Change plan") },
                    selected = false,
                    onClick = { navigate(Screen.SettingsSection("billing")) }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = null) },
                    label = { Text("Buy proxy") },
                    selected = false,
                    onClick = { navigate(Screen.Proxies) }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    label = { Text("New profile") },
                    selected = false,
                    onClick = { navigate(Screen.Edit(newProfile(), isNew = true)) }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Folder, contentDescription = null) },
                    label = { Text("Folders manager") },
                    selected = false,
                    onClick = { navigate(Screen.Folders) }
                )
                HorizontalDivider()
                Text("Workspaces", style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 4.dp))
                store.workspaces.forEach { ws: Workspace ->
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Workspaces, contentDescription = null) },
                        label = { Text(ws.name) },
                        selected = false,
                        onClick = {
                            Toast.makeText(context, "Workspace: ${ws.name} (active)", Toast.LENGTH_SHORT).show()
                            scope.launch { drawerState.close() }
                        }
                    )
                }
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    label = { Text("Add workspace") },
                    selected = false,
                    onClick = { showAddWorkspace = true }
                )
                HorizontalDivider()
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Support, contentDescription = null) },
                    label = { Text("Support") },
                    selected = false,
                    onClick = { navigate(Screen.Support) }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = { navigate(Screen.SettingsHub) }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Logout, contentDescription = null) },
                    label = { Text("Log out") },
                    selected = false,
                    onClick = { showLogoutConfirm = true }
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (screen is Screen.List) {
                    TopAppBar(
                        title = { Text("DevxyzLogin") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Menu")
                            }
                        },
                        actions = {
                            IconButton(onClick = { navigate(Screen.Proxies) }) {
                                Icon(Icons.Filled.Language, contentDescription = "Proxies")
                            }
                        }
                    )
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                when (val s = screen) {
                    is Screen.List -> ProfileListScreen(
                        profiles = store.profiles,
                        folders = store.folders,
                        onRunMode = { p, api -> runProfile(p, api) },
                        onStop = { stopProfile(it) },
                        onUpdate = { updated ->
                            updateStore(store.copy(profiles = store.profiles.map {
                                if (it.id == updated.id) updated else it
                            }))
                        },
                        onEdit = { navigate(Screen.Edit(it, isNew = false)) },
                        onClone = { p ->
                            val now = ProfileStore.nowIso()
                            val copy = p.copy(
                                id = ProfileStore.newId(),
                                name = p.name + " copy",
                                status = "ready",
                                createdAt = now, updatedAt = now, deletedAt = null
                            )
                            updateStore(store.copy(profiles = store.profiles + copy))
                        },
                        onDelete = { p ->
                            if (p.status == "running") stopProfile(p)
                            updateStore(store.copy(profiles = store.profiles.map {
                                if (it.id == p.id) it.copy(
                                    status = "ready",
                                    deletedAt = ProfileStore.nowIso(),
                                    updatedAt = ProfileStore.nowIso()
                                ) else it
                            }))
                            Toast.makeText(context, "Moved to trash — restore it in Settings → Restore profiles", Toast.LENGTH_SHORT).show()
                        },
                        onNewProfile = { navigate(Screen.Edit(newProfile(), isNew = true)) }
                    )
                    is Screen.Edit -> ProfileEditScreen(
                        initial = s.profile,
                        isNew = s.isNew,
                        folders = store.folders,
                        onSave = { saved ->
                            updateStore(
                                if (s.isNew) store.copy(profiles = store.profiles + saved)
                                else store.copy(profiles = store.profiles.map { if (it.id == saved.id) saved else it })
                            )
                            screen = Screen.List
                        },
                        onBack = { screen = Screen.List }
                    )
                    is Screen.Folders -> FoldersScreen(
                        store = store,
                        onStoreChange = { updateStore(it) },
                        onBack = { screen = Screen.List }
                    )
                    is Screen.Proxies -> ProxiesScreen(
                        store = store,
                        onStoreChange = { updateStore(it) },
                        onBack = { screen = Screen.List }
                    )
                    is Screen.Support -> SupportScreen(onBack = { screen = Screen.List })
                    is Screen.SettingsHub -> SettingsHubScreen(
                        onOpen = { key -> screen = Screen.SettingsSection(key) },
                        onBack = { screen = Screen.List }
                    )
                    is Screen.SettingsSection -> SettingsSectionScreen(
                        section = s.key,
                        store = store,
                        accountEmail = accountEmail,
                        plan = plan,
                        sessions = sessions,
                        themeMode = themeMode,
                        uiLanguage = uiLanguage,
                        defaults = defaults,
                        onStoreChange = { updateStore(it) },
                        onEmailChange = {
                            accountEmail = it
                            prefs.edit().putString("email", it).apply()
                        },
                        onPlanChange = {
                            plan = it
                            prefs.edit().putString("plan", it).apply()
                            Toast.makeText(context, "Plan changed to $it (local, no payment taken)", Toast.LENGTH_SHORT).show()
                        },
                        onSessionsChange = {
                            sessions = it
                            prefs.edit().putString("sessions", it.joinToString("\n")).apply()
                        },
                        onThemeChange = {
                            themeMode = it
                            prefs.edit().putString("theme", it).apply()
                        },
                        onLanguageChange = {
                            uiLanguage = it
                            prefs.edit().putString("ui_language", it).apply()
                        },
                        onDefaultsChange = {
                            defaults = it
                            prefs.edit().putString(
                                "default_settings",
                                Json { ignoreUnknownKeys = true }.encodeToString(it)
                            ).apply()
                        },
                        onBack = { screen = Screen.SettingsHub }
                    )
                }
            }
        }
    }

    if (showAddWorkspace) {
        AlertDialog(
            onDismissRequest = { showAddWorkspace = false },
            title = { Text("Add workspace") },
            text = {
                OutlinedTextField(
                    value = newWorkspaceName,
                    onValueChange = { newWorkspaceName = it },
                    label = { Text("Workspace name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newWorkspaceName.isNotBlank()) {
                        updateStore(store.copy(
                            workspaces = store.workspaces + Workspace(ProfileStore.newId(), newWorkspaceName.trim())
                        ))
                        newWorkspaceName = ""
                    }
                    showAddWorkspace = false
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddWorkspace = false }) { Text("Cancel") } }
        )
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Log out") },
            text = { Text("Log out of DevxyzLogin on this device? Your profiles stay stored locally.") },
            confirmButton = {
                TextButton(onClick = {
                    accountEmail = ""
                    prefs.edit().remove("email").apply()
                    showLogoutConfirm = false
                    scope.launch { drawerState.close() }
                    screen = Screen.List
                    Toast.makeText(context, "Logged out (local session cleared)", Toast.LENGTH_SHORT).show()
                }) { Text("Log out") }
            },
            dismissButton = { TextButton(onClick = { showLogoutConfirm = false }) { Text("Cancel") } }
        )
    }
    }
}
