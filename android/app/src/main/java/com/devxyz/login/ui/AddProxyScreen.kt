package com.devxyz.login.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devxyz.login.data.ApiResult
import com.devxyz.login.data.CatalogInfo
import com.devxyz.login.data.GeoData
import com.devxyz.login.data.ServerApi
import com.devxyz.login.model.Countries
import com.devxyz.login.model.Country
import com.devxyz.login.model.ProxyCheckInfo
import com.devxyz.login.model.ProxyConfig
import com.devxyz.login.model.SharedProxy
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import java.util.Locale

/**
 * Add proxy — spec v2 (ANDROID_UI_SPEC_V2.md): header "Add proxy" with
 * a green "⊕ Buy proxy" action; a Rotating | Custom segmented control.
 * Rotating: a "Proxy type" field opening the type sheet (GB figures
 * come from the REAL server catalog call — no provider configured
 * means "0.00 GB available" and the flow stays disabled, never a fake
 * balance), then the country list (France, Germany, United Kingdom,
 * United States first, then A–Z, star favorites persisted locally).
 * A country's chevron expands the curated states/cities dataset
 * (data/GeoData.kt); picking a region/city carries it to the server
 * as a location PREFERENCE on the assignment. Picking a country (or
 * region/city) opens the "Selected proxy" sheet: the server assigns
 * a real endpoint (POST /api/proxies/builtin) and REALLY checks it
 * (POST /api/proxies/check) while a spinner shows; only a passed
 * check enables the green "Add proxy" button. The city the sheet
 * displays is the check's measured exit — never the preference.
 * Custom: the existing custom-proxy form (host/port/type/auth +
 * saved proxies + the same real check) in the new field style.
 */

/** A country pick plus its optional region/city preference. */
private data class ProxySelection(
    val country: Country,
    val region: String?,
    val city: String?
)

private val ProxyTypeLabels = listOf("Residential", "Mobile", "Data Center")

private fun typeKeyOf(label: String): String = when (label) {
    "Mobile" -> "mobile"
    "Data Center" -> "datacenter"
    else -> "residential"
}

@Composable
fun AddProxyScreen(
    savedProxies: List<SharedProxy>,
    apiToken: String? = null,
    apiWorkspaceId: String? = null,
    onBuyProxy: () -> Unit = {},
    onConfirm: (ProxyConfig, ProxyCheckInfo?) -> Unit,
    onBack: () -> Unit
) {
    val api = remember { ServerApi() }
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("devxyz_prefs", Context.MODE_PRIVATE)
    }
    var tab by remember { mutableStateOf("rotating") }
    var proxyType by remember { mutableStateOf("Residential") }
    var showTypeSheet by remember { mutableStateOf(false) }
    var catalog by remember { mutableStateOf<CatalogInfo?>(null) }
    var selection by remember { mutableStateOf<ProxySelection?>(null) }
    var favorites by remember {
        mutableStateOf(
            (prefs.getString("fav_proxy_countries", "") ?: "")
                .split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
        )
    }

    LaunchedEffect(apiToken) {
        if (apiToken != null && catalog == null) {
            when (val r = api.catalog(apiToken)) {
                is ApiResult.Success -> catalog = r.data
                is ApiResult.Failure -> { /* type sheet falls back to the honest 0.00 GB state */ }
            }
        }
    }

    fun toggleFavorite(code: String) {
        favorites = if (code in favorites) favorites - code else favorites + code
        prefs.edit().putString("fav_proxy_countries", favorites.joinToString(",")).apply()
    }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            BackHeader(
                title = "Add proxy",
                onBack = onBack,
                actions = {
                    TextButton(onClick = onBuyProxy) {
                        Icon(
                            Icons.Filled.AddCircle, contentDescription = null,
                            tint = BrandGreen, modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Buy proxy", color = BrandGreen, style = TextStyle(fontSize = 15.sp))
                    }
                }
            )

            // Segmented Rotating | Custom (light track, white selected pill)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                SegmentedTab("Rotating", tab == "rotating", Modifier.weight(1f)) { tab = "rotating" }
                SegmentedTab("Custom", tab == "custom", Modifier.weight(1f)) { tab = "custom" }
            }

            if (tab == "rotating") {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height(8.dp))
                    SpecValueField(
                        label = "Proxy type",
                        value = proxyType,
                        trailing = {
                            Icon(
                                Icons.Filled.ExpandMore, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { showTypeSheet = true }
                    )
                }
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                // France, Germany, United Kingdom, United States first,
                // then A–Z (the existing Countries order already is the
                // four pinned, then alphabetical).
                val topCodes = listOf("FR", "DE", "GB", "US")
                val top = topCodes.map { Countries.byCode(it) }
                val rest = Countries.all
                    .filter { it.code !in topCodes }
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(top + rest, key = { it.code }) { country ->
                        CountryRow(
                            country = country,
                            favorite = country.code in favorites,
                            onToggleFavorite = { toggleFavorite(country.code) },
                            onSelect = { selection = ProxySelection(country, null, null) },
                            onSelectGeo = { region, city ->
                                selection = ProxySelection(country, region, city)
                            }
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            } else {
                CustomProxyForm(
                    savedProxies = savedProxies,
                    apiToken = apiToken,
                    onConfirm = onConfirm
                )
            }
        }
    }

    if (showTypeSheet) {
        ProxyTypeSheet(
            selected = proxyType,
            catalog = catalog,
            apiToken = apiToken,
            onSelect = {
                proxyType = it
                showTypeSheet = false
            },
            onDismiss = { showTypeSheet = false }
        )
    }

    selection?.let { sel ->
        SelectedProxySheet(
            country = sel.country,
            region = sel.region,
            city = sel.city,
            typeLabel = proxyType,
            apiToken = apiToken,
            apiWorkspaceId = apiWorkspaceId,
            onConfirm = { cfg, info ->
                selection = null
                onConfirm(cfg, info)
            },
            onDismiss = { selection = null }
        )
    }
}

@Composable
private fun SegmentedTab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = TextStyle(fontSize = 16.sp))
    }
}

@Composable
private fun CountryRow(
    country: Country,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    onSelect: () -> Unit,
    onSelectGeo: (region: String?, city: String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var expandedRegion by remember { mutableStateOf<String?>(null) }
    val regions = GeoData.regionsFor(country.code)
    // Spec v2: an opened/selected country row tints pale green.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (expanded) Modifier.background(runningTintColor()) else Modifier)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect() }
                .padding(horizontal = 16.dp, vertical = 13.dp)
        ) {
            Text(Countries.flag(country.code), style = TextStyle(fontSize = 20.sp))
            Spacer(Modifier.width(14.dp))
            Text(country.name, style = TextStyle(fontSize = 16.sp), modifier = Modifier.weight(1f))
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onToggleFavorite) {
                if (favorite) {
                    Icon(Icons.Filled.Star, contentDescription = "Favorite", tint = BrandGreen)
                } else {
                    Icon(
                        Icons.Outlined.StarBorder, contentDescription = "Add to favorites",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (expanded) {
            if (regions.isEmpty()) {
                // No curated states/cities for this country: selection
                // stays at country level and the exit city is whatever
                // the REAL server check measures after assignment —
                // stated, not faked.
                Text(
                    "States and cities are not listed for ${country.name} — the country is " +
                        "selected as a whole and the exit city is resolved by the server check.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 50.dp, end = 16.dp, bottom = 10.dp)
                )
            } else {
                // Region level: tap = prefer the whole region, chevron
                // expands its cities; a city tap prefers that city.
                // All three are PREFERENCES sent with the assignment
                // request — the exit is still measured by the check.
                regions.forEach { region ->
                    val regionOpen = expandedRegion == region.name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectGeo(region.name, null) }
                            .padding(start = 50.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                    ) {
                        Text(
                            region.name,
                            style = TextStyle(fontSize = 15.sp),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            expandedRegion = if (regionOpen) null else region.name
                        }) {
                            Icon(
                                if (regionOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = if (regionOpen) "Hide cities" else "Show cities",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (regionOpen) {
                        region.cities.forEach { cityName ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectGeo(region.name, cityName) }
                                    .padding(start = 66.dp, end = 16.dp, top = 9.dp, bottom = 9.dp)
                            ) {
                                Text(
                                    cityName,
                                    style = TextStyle(fontSize = 15.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

/**
 * GB line for the type sheet — the server catalog's `gbAvailable`
 * (provider-declared balance) only, never invented: a declared
 * number renders as-is; an explicit null (no provider / no declared
 * balance) renders "0.00 GB available".
 */
private fun gbLabel(catalog: CatalogInfo?, apiToken: String?, typeKey: String): String {
    val gbMap = catalog?.availableGb
    return when {
        gbMap != null && gbMap.containsKey(typeKey) -> {
            val gb = gbMap[typeKey]
            if (gb != null) String.format(Locale.US, "%.2f GB available", gb)
            else "0.00 GB available"
        }
        catalog != null && catalog.builtinAvailable -> "Available"
        else -> "0.00 GB available"
    }
}

@Composable
private fun ProxyTypeSheet(
    selected: String,
    catalog: CatalogInfo?,
    apiToken: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AppBottomSheet(onDismiss = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            SheetTitle("Proxy type")
            Spacer(Modifier.height(4.dp))
            ProxyTypeLabels.forEach { label ->
                val isSel = label == selected
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(label) }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            label,
                            style = TextStyle(fontSize = 16.sp),
                            color = if (isSel) BrandGreen else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            gbLabel(catalog, apiToken, typeKeyOf(label)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isSel) {
                        Icon(Icons.Filled.Check, contentDescription = "Selected", tint = BrandGreen)
                    }
                }
            }
            when {
                apiToken == null -> Text(
                    "Log in to use built-in proxies — assignment and checks run on the DevxyzLogin server.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
                catalog != null && !catalog.builtinAvailable -> Text(
                    "No built-in proxy provider is configured on the server yet — built-in proxies need a plugged-in supplier.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * "Selected proxy" sheet: assigns a REAL endpoint via the server and
 * REALLY checks it before the Add button unlocks. [region]/[city]
 * are the picker's location PREFERENCE — sent with the assignment
 * request and stored server-side on the assignment. The city in the
 * header line comes from the check result (measured exit), never
 * from the preference or a local list.
 */
@Composable
private fun SelectedProxySheet(
    country: Country,
    region: String?,
    city: String?,
    typeLabel: String,
    apiToken: String?,
    apiWorkspaceId: String?,
    onConfirm: (ProxyConfig, ProxyCheckInfo?) -> Unit,
    onDismiss: () -> Unit
) {
    val api = remember { ServerApi() }
    val scope = rememberCoroutineScope()
    var assigned by remember { mutableStateOf<ProxyConfig?>(null) }
    var protocol by remember { mutableStateOf("http") }
    var checking by remember { mutableStateOf(true) }
    var checkInfo by remember { mutableStateOf<ProxyCheckInfo?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun runCheck(cfg: ProxyConfig, proto: String) {
        val token = apiToken ?: return
        checking = true
        errorText = null
        checkInfo = null
        scope.launch {
            when (val r = api.checkProxy(token, proto, cfg.host, cfg.port, cfg.username, cfg.password)) {
                is ApiResult.Success -> {
                    checking = false
                    checkInfo = r.data
                }
                is ApiResult.Failure -> {
                    checking = false
                    errorText = "Proxy assigned (${cfg.host}:${cfg.port}) but the check failed: ${r.message}"
                }
            }
        }
    }

    LaunchedEffect(country.code, typeLabel, region, city) {
        val token = apiToken
        if (token == null) {
            checking = false
            errorText = "Log in to add a built-in proxy — assignment and checks run on the DevxyzLogin server."
            return@LaunchedEffect
        }
        checking = true
        errorText = null
        when (val r = api.builtinProxy(
            token, apiWorkspaceId, country.code, typeKeyOf(typeLabel),
            region ?: "", city ?: ""
        )) {
            is ApiResult.Success -> {
                val proxyObj = r.data["proxy"]
                val decoded = try {
                    proxyObj?.let {
                        Json { ignoreUnknownKeys = true }
                            .decodeFromJsonElement(ProxyConfig.serializer(), it)
                    }
                } catch (e: Exception) { null }
                if (decoded == null || decoded.host.isBlank() || decoded.port <= 0) {
                    checking = false
                    errorText = "The server returned an unusable proxy."
                } else {
                    // The endpoint is concrete now: keep the built-in
                    // country/kind for display, run as a custom endpoint.
                    val cfg = decoded.copy(
                        mode = "custom",
                        buyCountry = country.code,
                        buyType = typeLabel
                    )
                    assigned = cfg
                    protocol = cfg.type
                    runCheck(cfg, cfg.type)
                }
            }
            is ApiResult.Failure -> {
                checking = false
                errorText = if (r.httpCode == 501)
                    "No ${typeKeyOf(typeLabel)} provider is configured on the server yet — built-in proxies need a plugged-in supplier."
                else r.message
            }
        }
    }

    AppBottomSheet(onDismiss = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 10.dp)) {
            SheetTitle("Selected proxy")
            Spacer(Modifier.height(8.dp))
            // "Flag Country • City • Type" — city only once measured.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 13.dp)
            ) {
                Text(Countries.flag(country.code), style = TextStyle(fontSize = 20.sp))
                Spacer(Modifier.width(10.dp))
                val city = checkInfo?.city?.takeIf { it.isNotBlank() }
                Text(
                    country.name +
                        (if (city != null) " • $city" else "") +
                        " • $typeLabel",
                    style = TextStyle(fontSize = 16.sp)
                )
            }
            Spacer(Modifier.height(14.dp))
            // Protocol chips: HTTP / HTTPS / Socks 5 (selected = gray fill)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.weight(1f))
                listOf("http" to "HTTP", "https" to "HTTPS", "socks5" to "Socks 5").forEach { (v, label) ->
                    val isSel = protocol == v
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSel) MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                            .clickable(enabled = assigned != null && !checking) {
                                protocol = v
                                assigned?.let { runCheck(it, v) }
                            }
                            .padding(horizontal = 18.dp, vertical = 9.dp)
                    ) { Text(label, style = TextStyle(fontSize = 15.sp)) }
                    Spacer(Modifier.width(8.dp))
                }
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            when {
                checking -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = BrandGreen
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Checking proxy…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                checkInfo != null -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(proxyOkColor())
                        .padding(horizontal = 14.dp, vertical = 13.dp)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, tint = BrandGreen)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Proxy is ok!",
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                        color = BrandGreen
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${checkInfo!!.ip} • ${checkInfo!!.latencyMs} ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                errorText != null -> Text(
                    errorText!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 14.dp, vertical = 13.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            GreenButton(
                text = "Add proxy",
                enabled = checkInfo != null && assigned != null,
                onClick = {
                    val cfg = assigned ?: return@GreenButton
                    onConfirm(cfg.copy(type = protocol), checkInfo)
                }
            )
        }
    }
}

/** Custom tab: the existing custom-proxy form in the new field style. */
@Composable
private fun CustomProxyForm(
    savedProxies: List<SharedProxy>,
    apiToken: String?,
    onConfirm: (ProxyConfig, ProxyCheckInfo?) -> Unit
) {
    val api = remember { ServerApi() }
    val scope = rememberCoroutineScope()
    var type by remember { mutableStateOf("http") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var checking by remember { mutableStateOf(false) }
    var checkText by remember { mutableStateOf<String?>(null) }
    var checkFailed by remember { mutableStateOf(false) }
    var lastInfo by remember { mutableStateOf<ProxyCheckInfo?>(null) }

    val portNum = port.toIntOrNull() ?: 0
    val valid = host.isNotBlank() && portNum > 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        if (savedProxies.isNotEmpty()) {
            FieldLabel("Saved proxies")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                savedProxies.forEach { sp ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                            .clickable {
                                type = sp.proxy.type
                                host = sp.proxy.host
                                port = if (sp.proxy.port > 0) sp.proxy.port.toString() else ""
                                username = sp.proxy.username
                                password = sp.proxy.password
                                checkText = null
                                lastInfo = null
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) { Text(sp.name, style = TextStyle(fontSize = 14.sp)) }
                    Spacer(Modifier.width(8.dp))
                }
            }
        }
        FieldLabel("Protocol")
        RadioChips(
            options = listOf(
                "http" to "HTTP", "https" to "HTTPS",
                "socks4" to "SOCKS4", "socks5" to "SOCKS5"
            ),
            value = type,
            onSelect = { type = it }
        )
        Spacer(Modifier.height(10.dp))
        SpecTextField(
            label = "Host",
            value = host,
            onValueChange = { host = it; checkText = null; lastInfo = null },
            placeholder = "192.0.2.10",
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        SpecTextField(
            label = "Port",
            value = port,
            onValueChange = { port = it.filter { c -> c.isDigit() }; checkText = null; lastInfo = null },
            placeholder = "8080",
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        SpecTextField(
            label = "Username (optional)",
            value = username,
            onValueChange = { username = it },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        SpecTextField(
            label = "Password (optional)",
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Note: Android WebView applies HTTP/HTTPS proxies only; SOCKS and proxy authentication cannot be applied (you'll be told at launch).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            enabled = valid && !checking,
            onClick = {
                val token = apiToken
                if (token == null) {
                    checkFailed = true
                    checkText = "Log in to run a real proxy check — checks run on the DevxyzLogin server"
                } else {
                    checking = true
                    checkText = null
                    scope.launch {
                        when (val r = api.checkProxy(token, type, host.trim(), portNum, username, password)) {
                            is ApiResult.Success -> {
                                checking = false
                                checkFailed = false
                                lastInfo = r.data
                                checkText = "✓ ${r.data.ip} — ${r.data.place.ifBlank { "Unknown location" }} • " +
                                    "${r.data.timezone} • ${r.data.latencyMs} ms"
                            }
                            is ApiResult.Failure -> {
                                checking = false
                                checkFailed = true
                                checkText = "✗ ${r.message}"
                            }
                        }
                    }
                }
            }
        ) { Text(if (checking) "Checking…" else "Check proxy") }
        if (checkText != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                checkText!!,
                style = MaterialTheme.typography.bodySmall,
                color = if (checkFailed) MaterialTheme.colorScheme.error else BrandGreen
            )
        }
        Spacer(Modifier.height(10.dp))
        GreenButton(
            text = "Add proxy",
            enabled = valid,
            onClick = {
                onConfirm(
                    ProxyConfig(
                        mode = "custom", type = type, host = host.trim(),
                        port = portNum, username = username, password = password
                    ),
                    lastInfo
                )
            }
        )
    }
}
