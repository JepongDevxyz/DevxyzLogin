package com.devxyz.login.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devxyz.login.model.Folder
import com.devxyz.login.model.OsOptions
import com.devxyz.login.model.Profile

/**
 * Main screen (GoLogin Android parity): profile list with name, status
 * (ready/running), proxy label incl. host as "location text", Run/Stop,
 * search box, and folder filter chips.
 */
@Composable
fun ProfileListScreen(
    profiles: List<Profile>,
    folders: List<Folder>,
    onRun: (Profile) -> Unit,
    onStop: (Profile) -> Unit,
    onEdit: (Profile) -> Unit,
    onClone: (Profile) -> Unit,
    onDelete: (Profile) -> Unit,
    onNewProfile: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var folderFilter by remember { mutableStateOf<String?>(null) }

    val visible = profiles.filter { p ->
        p.deletedAt == null &&
            (folderFilter == null || folderFilter in p.folderIds) &&
            (query.isBlank() || p.name.contains(query, ignoreCase = true) || p.id.contains(query, ignoreCase = true))
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNewProfile) {
                Icon(Icons.Filled.Add, contentDescription = "New profile")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by name or ID") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = folderFilter == null,
                        onClick = { folderFilter = null },
                        label = { Text("All profiles") }
                    )
                }
                items(folders) { folder ->
                    FilterChip(
                        selected = folderFilter == folder.id,
                        onClick = { folderFilter = if (folderFilter == folder.id) null else folder.id },
                        label = { Text(folder.name) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (visible.isEmpty()) {
                Text(
                    "No profiles yet. Tap + to create one.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visible, key = { it.id }) { profile ->
                    ProfileRow(
                        profile = profile,
                        onRun = { onRun(profile) },
                        onStop = { onStop(profile) },
                        onEdit = { onEdit(profile) },
                        onClone = { onClone(profile) },
                        onDelete = { onDelete(profile) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(
    profile: Profile,
    onRun: () -> Unit,
    onStop: () -> Unit,
    onEdit: () -> Unit,
    onClone: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val running = profile.status == "running"
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(profile.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${OsOptions.label(profile.os)} • ${if (running) "Running" else "Ready"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    // Proxy flag/location text: type + endpoint (GoLogin shows flag + IP there)
                    Text(
                        "Proxy: ${profile.proxy.listLabel}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (profile.tags.isNotEmpty()) {
                        Text(
                            profile.tags.joinToString("  ") { "#$it" },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Profile menu")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = { menuOpen = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("Clone") },
                        leadingIcon = { Icon(Icons.Filled.FileCopy, contentDescription = null) },
                        onClick = { menuOpen = false; onClone() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                        onClick = { menuOpen = false; onDelete() }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row {
                if (running) {
                    OutlinedButton(onClick = onStop) {
                        Icon(Icons.Filled.Stop, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Stop")
                    }
                } else {
                    Button(onClick = onRun) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Run")
                    }
                }
            }
        }
    }
}
