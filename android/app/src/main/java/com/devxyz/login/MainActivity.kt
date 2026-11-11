package com.devxyz.login

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Support
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devxyz.login.browser.BrowserActivityBase
import com.devxyz.login.data.ProfileStore
import com.devxyz.login.data.ServerApi
import com.devxyz.login.data.SessionStore
import com.devxyz.login.data.SyncEngine
import com.devxyz.login.model.DefaultBookmarks
import com.devxyz.login.model.DefaultProfileSettings
import com.devxyz.login.model.FingerprintGenerator
import com.devxyz.login.model.Folder
import com.devxyz.login.model.Profile
import com.devxyz.login.model.ProfileNames
import com.devxyz.login.model.Store
import com.devxyz.login.model.Workspace
import com.devxyz.login.ui.AddProfileSheet
import com.devxyz.login.ui.AddProxyScreen
import com.devxyz.login.ui.AppBottomSheet
import com.devxyz.login.ui.BrandGreen
import com.devxyz.login.ui.ConfirmSheet
import com.devxyz.login.ui.DangerRed
import com.devxyz.login.ui.DevxyzTheme
import com.devxyz.login.ui.FoldersScreen
import com.devxyz.login.ui.GreenButton
import com.devxyz.login.ui.LoginScreen
import com.devxyz.login.ui.ProfileEditScreen
import com.devxyz.login.ui.ProfileListScreen
import com.devxyz.login.ui.ProxiesScreen
import com.devxyz.login.ui.SettingsHubScreen
import com.devxyz.login.ui.SettingsSectionScreen
import com.devxyz.login.ui.SheetTitle
import com.devxyz.login.ui.SpecTextField
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

    // ---- v4: live server session + sync state ----
    val sessionStore = remember { SessionStore(context) }
    val api = remember { ServerApi() }
    var session by remember { mutableStateOf(sessionStore.load()) }
    var localChosen by remember { mutableStateOf(sessionStore.localModeChosen) }
    var forceLogin by remember { mutableStateOf(false) }
    var syncStatus by remember {
        mutableStateOf(if (session != null) "Synced" else "Local mode")
    }
    var syncRunning by remember { mutableStateOf(false) }
    var syncAgain by remember { mutableStateOf(false) }

    var accountEmail by remember {
        mutableStateOf(session?.email ?: (prefs.getString("email", "") ?: ""))
    }
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

    val scope = rememberCoroutineScope()
    var showAddWorkspace by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var newWorkspaceName by remember { mutableStateOf("") }

    // ---- v2.4.0 shell state: account sheet, add-profile sheet flow,
    // hoisted pin set (shared by the list rows and the editor header) ----
    var showAccountSheet by remember { mutableStateOf(false) }
    var pinnedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var addDraft by remember { mutableStateOf<Profile?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showAddProxy by remember { mutableStateOf(false) }


    /**
     * Runs one sync pass (SyncEngine owns the merge rules). Best-effort:
     * failures only flip the status line to "Sync pending" — dirty flags
     * survive and the next launch/login/mutation retries. Profiles the
     * user edited while a sync was in flight keep their newer local
     * version; profiles/folders created meanwhile are preserved.
     */
    // Forward-reference bridge: runSync (above updateStore) applies merged
    // results through updateStore; Kotlin local funs cannot mutually
    // reference, so runSync calls this holder, bound after updateStore.
    var updateStoreFn: (Store, Boolean) -> Unit = { _, _ -> }

    fun runSync() {
        val s = session ?: return
        if (syncRunning) {
            syncAgain = true
            return
        }
        syncRunning = true
        syncStatus = "Syncing…"
        val snapshot = store
        scope.launch {
            val report = try {
                SyncEngine.synchronize(api, s, snapshot)
            } catch (e: Exception) {
                syncRunning = false
                syncStatus = "Sync pending"
                return@launch
            }
            val current = store
            val resultById = report.store.profiles.associateBy { it.id }
            val snapshotById = snapshot.profiles.associateBy { it.id }
            val reconciled = current.profiles.map { cur ->
                val res = resultById[cur.id] ?: return@map cur
                val snap = snapshotById[cur.id]
                if (snap != null && cur.updatedAt != snap.updatedAt) {
                    // Edited mid-sync: keep the user's newer copy, adopt
                    // the server id the sync may have just assigned.
                    cur.copy(
                        serverId = cur.serverId ?: res.serverId,
                        dirty = if (res.serverId != null || cur.serverId != null) true else cur.dirty
                    )
                } else res
            }
            val extraFolders = current.folders.filter { cf ->
                report.store.folders.none { it.id == cf.id }
            }
            val finalStore = report.store.copy(
                profiles = reconciled,
                folders = report.store.folders + extraFolders,
                folderServerIds = report.store.folderServerIds +
                    extraFolders.mapNotNull { f ->
                        current.folderServerIds[f.id]?.let { f.id to it }
                    }.toMap()
            )
            updateStoreFn(finalStore, true)
            syncRunning = false
            when {
                report.authExpired -> {
                    sessionStore.clear()
                    session = null
                    syncStatus = "Local mode"
                    Toast.makeText(
                        context,
                        "Session expired — log in again to sync. Your profiles stay on this device.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                report.failures > 0 -> syncStatus = "Sync pending"
                else -> syncStatus = "Synced"
            }
            if (syncAgain) {
                syncAgain = false
                runSync()
            }
        }
    }

    /**
     * Single write path for the store. On local (non-sync) writes, any
     * profile that already lives on the server and whose content changed
     * (ignoring per-device run status / timestamps) is marked dirty so
     * the next sync PATCHes it — this one rule covers edits from the
     * editor, quick settings, delete and restore alike. After a local
     * write with sync work pending, a best-effort sync is scheduled.
     */
    fun updateStore(next: Store, fromSync: Boolean = false) {
        val previous = store
        var adjusted = next
        if (!fromSync) {
            val oldById = previous.profiles.associateBy { it.id }
            adjusted = next.copy(profiles = next.profiles.map { p ->
                val old = oldById[p.id]
                if (old != null && p.serverId != null && !p.dirty &&
                    old.copy(status = p.status, dirty = p.dirty, updatedAt = p.updatedAt) != p
                ) p.copy(dirty = true) else p
            })
        }
        store = adjusted
        profileStore.save(adjusted)
        if (!fromSync && session != null) {
            val hasWork = adjusted.profiles.any {
                it.dirty ||
                    (it.serverId == null && it.deletedAt == null) ||
                    (it.deletedAt != null && it.serverId != null)
            } || adjusted.folders != previous.folders
            if (hasWork) runSync()
        }
    }

    // Sync once on launch when a session exists.
    LaunchedEffect(session?.token) {
        if (session != null) runSync()
    }

    updateStoreFn = { next, fromSync -> updateStore(next, fromSync) }

    fun navigate(target: Screen) {
        screen = target
    }

    fun newProfile(): Profile {
        val now = ProfileStore.nowIso()
        // GoLogin editor parity (capture §4): the name auto-increments
        // "profile N" (the Default-profile-settings name template, with
        // {number} substituted), the OS default is Linux, the Proxy
        // segment default is Without proxy (v2.3.1: built-in proxies are
        // only ever assigned by the server at "Check Proxy" time — never
        // a baked-in host), and bookmarks start from the editor-variant
        // default list.
        val usedNumbers = store.profiles.mapNotNull { p ->
            Regex("""^profile (\d+)$""").find(p.name.trim())?.groupValues?.get(1)?.toIntOrNull()
        }
        val nextNumber = (usedNumbers.maxOrNull() ?: 0) + 1
        val template = defaults.profileNameTemplate
        val name = if (template.contains("{number}")) template.replace("{number}", nextNumber.toString())
        else "profile $nextNumber"
        return Profile(
            id = ProfileStore.newId(),
            name = name,
            os = "linux",
            fingerprint = FingerprintGenerator.generate("linux"),
            proxy = com.devxyz.login.model.ProxyConfig(mode = "none"),
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

    fun deleteProfile(p: Profile) {
        if (p.status == "running") stopProfile(p)
        updateStore(store.copy(profiles = store.profiles.map {
            if (it.id == p.id) it.copy(
                status = "ready",
                deletedAt = ProfileStore.nowIso(),
                updatedAt = ProfileStore.nowIso()
            ) else it
        }))
        Toast.makeText(context, "Moved to trash — restore it in Settings → Restore profiles", Toast.LENGTH_SHORT).show()
    }

    fun togglePin(p: Profile) {
        pinnedIds = if (p.id in pinnedIds) pinnedIds - p.id else pinnedIds + p.id
    }

    fun openAddSheet() {
        // Spec v2: the Add profile sheet pre-fills a generated
        // word-word name ("rapid-unit"); the draft lives here so the
        // Add proxy flow can write its result back into it.
        addDraft = newProfile().copy(name = ProfileNames.random())
        showAddSheet = true
    }

    DevxyzTheme(themeMode = themeMode) {
    if (session == null && (forceLogin || !localChosen)) {
        // v4: no session and local mode not chosen yet → the account gate
        // comes first (the GoLogin Android app also opens on login).
        LoginScreen(
            onLoggedIn = { s ->
                session = s
                sessionStore.save(s)
                forceLogin = false
                accountEmail = s.email
                syncStatus = "Syncing…"
                Toast.makeText(
                    context,
                    "Signed in as ${s.email} — syncing your profiles",
                    Toast.LENGTH_SHORT
                ).show()
                runSync()
            },
            onContinueLocal = {
                sessionStore.localModeChosen = true
                localChosen = true
                forceLogin = false
                syncStatus = "Local mode"
            }
        )
    } else {
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold { padding ->
            Column(modifier = Modifier.padding(padding)) {
                when (val s = screen) {
                    is Screen.List -> ProfileListScreen(
                        profiles = store.profiles,
                        folders = store.folders,
                        accountName = session?.workspaceName
                            ?: store.workspaces.firstOrNull()?.name ?: "My workspace",
                        connectionText = when {
                            session == null -> "Local mode"
                            syncStatus == "Synced" -> "Connected"
                            else -> syncStatus
                        },
                        plan = plan,
                        pinnedIds = pinnedIds,
                        onTogglePin = { togglePin(it) },
                        onOpenAccount = { showAccountSheet = true },
                        onChoosePlan = { navigate(Screen.SettingsSection("billing")) },
                        onRunMode = { p, apiMode -> runProfile(p, apiMode) },
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
                        onDelete = { deleteProfile(it) },
                        onNewProfile = { openAddSheet() },
                        apiToken = session?.token
                    )
                    is Screen.Edit -> ProfileEditScreen(
                        initial = s.profile,
                        isNew = s.isNew,
                        folders = store.folders,
                        savedProxies = store.proxies,
                        onSave = { saved ->
                            updateStore(
                                if (s.isNew) store.copy(profiles = store.profiles + saved)
                                else store.copy(profiles = store.profiles.map { if (it.id == saved.id) saved else it })
                            )
                            screen = Screen.List
                        },
                        onBack = { screen = Screen.List },
                        onDelete = { deleteProfile(it) },
                        pinned = s.profile.id in pinnedIds,
                        onTogglePin = { togglePin(s.profile) },
                        onCreateFolder = { name ->
                            updateStore(store.copy(
                                folders = store.folders + Folder(ProfileStore.newId(), name)
                            ))
                        },
                        onBuyProxy = { navigate(Screen.Proxies) },
                        apiToken = session?.token,
                        apiWorkspaceId = session?.workspaceId
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
                        session = session,
                        onRequireLogin = { forceLogin = true },
                        onBack = { screen = Screen.SettingsHub }
                    )
                }
            }
        }

        // v2.4.0: floating bottom bar on the three tab roots — a white
        // pill with Profiles / Proxies / Settings plus a separate
        // solid-green "+ Add profile" pill (spec v2).
        if (screen is Screen.List || screen is Screen.Proxies || screen is Screen.SettingsHub) {
            BottomNavBar(
                current = screen,
                onSelect = { navigate(it) },
                onAddProfile = { openAddSheet() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
    } // else: signed in or local mode

    if (showAccountSheet) {
        AccountSheet(
            workspaceName = store.workspaces.firstOrNull()?.name ?: "My workspace",
            email = session?.email,
            plan = plan,
            profileCount = store.profiles.count { it.deletedAt == null },
            syncText = if (session != null) "☁ $syncStatus"
            else "Local mode — profiles stay on this device",
            workspaces = store.workspaces,
            loggedIn = session != null,
            onNavigate = { target ->
                showAccountSheet = false
                navigate(target)
            },
            onNewProfile = {
                showAccountSheet = false
                navigate(Screen.Edit(newProfile(), isNew = true))
            },
            onAddWorkspace = {
                showAccountSheet = false
                showAddWorkspace = true
            },
            onWorkspaceClick = { ws ->
                Toast.makeText(context, "Workspace: ${ws.name} (active)", Toast.LENGTH_SHORT).show()
                showAccountSheet = false
            },
            onLogout = {
                showAccountSheet = false
                showLogoutConfirm = true
            },
            onLogin = {
                showAccountSheet = false
                forceLogin = true
            },
            onDismiss = { showAccountSheet = false }
        )
    }

    if (showAddSheet && addDraft != null) {
        AddProfileSheet(
            draft = addDraft!!,
            folders = store.folders,
            onDraftChange = { addDraft = it },
            onConnectProxy = {
                // The Add proxy flow is a full screen in this window —
                // the sheet steps aside and returns with the result.
                showAddSheet = false
                showAddProxy = true
            },
            onExtendedSettings = {
                val d = addDraft!!
                showAddSheet = false
                addDraft = null
                navigate(Screen.Edit(d, isNew = true))
            },
            onCreate = {
                val d = addDraft!!
                showAddSheet = false
                addDraft = null
                updateStore(store.copy(
                    profiles = store.profiles + d.copy(updatedAt = ProfileStore.nowIso())
                ))
                screen = Screen.List
            },
            onCreateFolder = { name ->
                updateStore(store.copy(
                    folders = store.folders + Folder(ProfileStore.newId(), name)
                ))
            },
            onDismiss = {
                showAddSheet = false
                addDraft = null
            }
        )
    }

    if (showAddProxy) {
        AddProxyScreen(
            savedProxies = store.proxies,
            apiToken = session?.token,
            apiWorkspaceId = session?.workspaceId,
            onBuyProxy = {
                showAddProxy = false
                addDraft = null
                navigate(Screen.Proxies)
            },
            onConfirm = { cfg, info ->
                showAddProxy = false
                addDraft = addDraft?.let { d ->
                    var next = d.copy(proxy = cfg)
                    if (info != null) {
                        next = next.copy(lastProxyCheck = info)
                        if (next.fingerprint.timezone.auto && info.timezone.isNotBlank()) {
                            next = next.copy(
                                fingerprint = next.fingerprint.copy(
                                    timezone = next.fingerprint.timezone.copy(zone = info.timezone)
                                )
                            )
                        }
                    }
                    next
                }
                if (addDraft != null) showAddSheet = true
            },
            onBack = {
                showAddProxy = false
                if (addDraft != null) showAddSheet = true
            }
        )
    }

    if (showAddWorkspace) {
        AppBottomSheet(onDismiss = { showAddWorkspace = false }) {
            Column(modifier = Modifier.padding(bottom = 10.dp)) {
                SheetTitle("Add workspace")
                Spacer(Modifier.height(8.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SpecTextField(
                        label = "Workspace name",
                        value = newWorkspaceName,
                        onValueChange = { newWorkspaceName = it },
                        clearable = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                GreenButton(
                    text = "Add",
                    enabled = newWorkspaceName.isNotBlank(),
                    onClick = {
                        updateStore(store.copy(
                            workspaces = store.workspaces + Workspace(ProfileStore.newId(), newWorkspaceName.trim())
                        ))
                        newWorkspaceName = ""
                        showAddWorkspace = false
                    }
                )
            }
        }
    }

    if (showLogoutConfirm) {
        ConfirmSheet(
            title = "Log out",
            message = "Log out of DevxyzLogin on this device? Your profiles stay stored locally.",
            confirmLabel = "Log out",
            onConfirm = {
                // v4: REAL logout — the server session token is
                // dropped; local profiles are never deleted.
                sessionStore.clear()
                session = null
                sessionStore.localModeChosen = true
                localChosen = true
                syncStatus = "Local mode"
                accountEmail = ""
                prefs.edit().remove("email").apply()
                showLogoutConfirm = false
                screen = Screen.List
                Toast.makeText(context, "Logged out — your profiles stay on this device", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showLogoutConfirm = false }
        )
    }
    }
}

/**
 * Floating bottom navigation (spec v2): a white pill (rounded 28dp,
 * shadow) with Profiles / Proxies / Settings tabs — active tab green
 * — and a separate solid-green "+ Add profile" pill beside it.
 */
@Composable
private fun BottomNavBar(
    current: Screen,
    onSelect: (Screen) -> Unit,
    onAddProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.weight(1f)
        ) {
            Row {
                BottomNavTab(
                    label = "Profiles",
                    icon = Icons.Filled.Person,
                    selected = current is Screen.List,
                    modifier = Modifier.weight(1f)
                ) { onSelect(Screen.List) }
                BottomNavTab(
                    label = "Proxies",
                    icon = Icons.Filled.Monitor,
                    selected = current is Screen.Proxies,
                    modifier = Modifier.weight(1f)
                ) { onSelect(Screen.Proxies) }
                BottomNavTab(
                    label = "Settings",
                    icon = Icons.Filled.Settings,
                    selected = current is Screen.SettingsHub,
                    modifier = Modifier.weight(1f)
                ) { onSelect(Screen.SettingsHub) }
            }
        }
        Spacer(Modifier.width(10.dp))
        Button(
            onClick = onAddProfile,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandGreen,
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
            modifier = Modifier.height(58.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Add profile", style = TextStyle(fontSize = 13.sp), maxLines = 1)
            }
        }
    }
}

@Composable
private fun BottomNavTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tint = if (selected) BrandGreen else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, style = TextStyle(fontSize = 12.sp), color = tint)
    }
}

/**
 * Account / workspace sheet (spec v2: the account menu is an avatar
 * bottom sheet, not a side drawer) — the same content and actions
 * the old drawer carried: plan, sync state, workspaces, support,
 * settings, log out / log in.
 */
@Composable
private fun AccountSheet(
    workspaceName: String,
    email: String?,
    plan: String,
    profileCount: Int,
    syncText: String,
    workspaces: List<Workspace>,
    loggedIn: Boolean,
    onNavigate: (Screen) -> Unit,
    onNewProfile: () -> Unit,
    onAddWorkspace: () -> Unit,
    onWorkspaceClick: (Workspace) -> Unit,
    onLogout: () -> Unit,
    onLogin: () -> Unit,
    onDismiss: () -> Unit
) {
    AppBottomSheet(onDismiss = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BrandGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        workspaceName.firstOrNull()?.uppercase() ?: "D",
                        color = Color.White,
                        style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        workspaceName,
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        email ?: "Local mode",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "$plan · $profileCount profiles",
                    style = MaterialTheme.typography.bodySmall
                )
                // v4: honest sync state — "Synced" only after a clean
                // pass, "Sync pending" while dirty work is waiting,
                // "Local mode" when there is no account session.
                Text(
                    syncText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            AccountRow(Icons.Filled.Upgrade, "Change plan") {
                onNavigate(Screen.SettingsSection("billing"))
            }
            AccountRow(Icons.Filled.ShoppingCart, "Buy proxy") {
                onNavigate(Screen.Proxies)
            }
            AccountRow(Icons.Filled.Add, "New profile") { onNewProfile() }
            AccountRow(Icons.Filled.Folder, "Folders manager") {
                onNavigate(Screen.Folders)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                "Workspaces",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 2.dp)
            )
            workspaces.forEach { ws ->
                AccountRow(Icons.Filled.Workspaces, ws.name) { onWorkspaceClick(ws) }
            }
            AccountRow(Icons.Filled.Add, "Add workspace") { onAddWorkspace() }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            AccountRow(Icons.Filled.Support, "Support") { onNavigate(Screen.Support) }
            AccountRow(Icons.Filled.Settings, "Settings") { onNavigate(Screen.SettingsHub) }
            if (loggedIn) {
                AccountRow(Icons.Filled.Logout, "Log out", danger = true) { onLogout() }
            } else {
                AccountRow(Icons.Filled.Login, "Log in") { onLogin() }
            }
        }
    }
}

@Composable
private fun AccountRow(
    icon: ImageVector,
    label: String,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val color = if (danger) DangerRed else MaterialTheme.colorScheme.onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, style = TextStyle(fontSize = 16.sp), color = color)
    }
}
