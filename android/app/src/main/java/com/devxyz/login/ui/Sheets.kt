package com.devxyz.login.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devxyz.login.model.Countries
import com.devxyz.login.model.Folder
import com.devxyz.login.model.Profile

/**
 * Display label for a profile's proxy in the spec field boxes:
 * flag + country name (+ " · City" once a REAL check measured one),
 * the host for a custom proxy without a check, or "" when no proxy
 * is attached (callers then show the "Connect proxy" placeholder).
 */
fun proxyFieldLabel(profile: Profile): String {
    val p = profile.proxy
    val chk = profile.lastProxyCheck
    return when {
        chk != null && chk.countryCode.isNotBlank() ->
            "${Countries.flag(chk.countryCode)} ${Countries.nameOf(chk.countryCode)}" +
                (if (chk.city.isNotBlank()) " · ${chk.city}" else "")
        p.mode == "devxyz" ->
            "${Countries.flag(p.buyCountry)} ${Countries.nameOf(p.buyCountry)}"
        p.isSet -> p.listLabel
        else -> ""
    }
}

/**
 * Add profile bottom sheet (spec v2): title "Add profile", Name
 * (pre-filled by the caller with a generated word-word name, X
 * clear), Proxy ("Connect proxy" → the Add proxy flow, hosted by the
 * caller), Folder ("All" → Folders sheet), an outlined "Extended
 * settings ›" row-button opening the full editor, and the solid
 * green "Add profile" button.
 *
 * The draft profile lives with the caller (MainActivity) so the
 * hoisted Add proxy flow can write the chosen proxy back into it.
 */
@Composable
fun AddProfileSheet(
    draft: Profile,
    folders: List<Folder>,
    onDraftChange: (Profile) -> Unit,
    onConnectProxy: () -> Unit,
    onExtendedSettings: () -> Unit,
    onCreate: () -> Unit,
    onCreateFolder: ((String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var showFolders by remember { mutableStateOf(false) }

    AppBottomSheet(onDismiss = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 10.dp)) {
            SheetTitle("Add profile")
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
            ) {
                SpecTextField(
                    label = "Name",
                    value = draft.name,
                    onValueChange = { onDraftChange(draft.copy(name = it)) },
                    clearable = true,
                    modifier = Modifier.fillMaxWidth()
                )
                val proxyLabel = proxyFieldLabel(draft)
                SpecValueField(
                    label = "Proxy",
                    value = proxyLabel,
                    placeholder = "Connect proxy",
                    trailing = {
                        if (proxyLabel.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    onDraftChange(
                                        draft.copy(
                                            proxy = com.devxyz.login.model.ProxyConfig(mode = "none"),
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
                    value = folders.filter { it.id in draft.folderIds }
                        .joinToString(", ") { it.name }
                        .ifBlank { "All" },
                    trailing = {
                        Icon(
                            Icons.Filled.ExpandMore, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = { showFolders = true }
                )
                // Outlined "Extended settings ›" row-button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .clickable { onExtendedSettings() }
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Extended settings", style = TextStyle(fontSize = 16.sp))
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Filled.ChevronRight, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            GreenButton(
                text = "Add profile",
                enabled = draft.name.isNotBlank(),
                onClick = onCreate
            )
        }
    }

    if (showFolders) {
        FoldersSheet(
            folders = folders,
            selectedIds = draft.folderIds.toSet(),
            onToggle = { folder ->
                onDraftChange(
                    draft.copy(
                        folderIds = if (folder.id in draft.folderIds) draft.folderIds - folder.id
                        else draft.folderIds + folder.id
                    )
                )
            },
            onCreateFolder = onCreateFolder,
            onDismiss = { showFolders = false }
        )
    }
}

/**
 * Folders sheet (spec v2): title "Folders", a search field, folder
 * rows (tap toggles membership, check mark when selected), the
 * "No folders yet" empty state, and an outlined "⊕ Add folder"
 * button that expands into an inline create row. [onCreateFolder]
 * is null where the host cannot write the store (the button hides).
 */
@Composable
fun FoldersSheet(
    folders: List<Folder>,
    selectedIds: Set<String>,
    onToggle: (Folder) -> Unit,
    onCreateFolder: ((String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    val visible = folders.filter {
        query.isBlank() || it.name.contains(query, ignoreCase = true)
    }

    AppBottomSheet(onDismiss = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            SheetTitle("Folders")
            Spacer(Modifier.height(6.dp))
            // Search row ("Search..." with magnifier, hairline below)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    Icons.Filled.Search, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            "Search...",
                            style = TextStyle(fontSize = 16.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(BrandGreen),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear search",
                            modifier = Modifier.size(18.dp))
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (visible.isEmpty()) {
                Text(
                    if (folders.isEmpty()) "No folders yet" else "No folders match your search",
                    style = TextStyle(fontSize = 15.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            visible.forEach { folder ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(folder) }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(folder.name, style = TextStyle(fontSize = 16.sp), modifier = Modifier.weight(1f))
                    if (folder.id in selectedIds) {
                        Icon(Icons.Filled.Check, contentDescription = "Selected", tint = BrandGreen)
                    }
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            if (onCreateFolder != null) {
                Spacer(Modifier.height(14.dp))
                if (!adding) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .clickable { adding = true }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.AddCircleOutline, contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Add folder", style = TextStyle(fontSize = 16.sp))
                        }
                    }
                } else {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SpecTextField(
                            label = "Folder name",
                            value = newName,
                            onValueChange = { newName = it },
                            clearable = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { adding = false; newName = "" }) { Text("Cancel") }
                            TextButton(
                                enabled = newName.isNotBlank(),
                                onClick = {
                                    onCreateFolder(newName.trim())
                                    newName = ""
                                    adding = false
                                }
                            ) { Text("Add", color = BrandGreen, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }
}
