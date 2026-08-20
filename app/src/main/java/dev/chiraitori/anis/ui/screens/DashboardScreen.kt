package dev.chiraitori.anis.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chiraitori.anis.data.model.DefaultDnsProviders
import dev.chiraitori.anis.data.model.TopBlockedDomainStat
import dev.chiraitori.anis.ui.MainViewModel
import dev.chiraitori.anis.ui.components.AppDestination
import dev.chiraitori.anis.ui.components.CategoryChip
import dev.chiraitori.anis.ui.components.LogItemRow
import dev.chiraitori.anis.ui.components.ProfileSelectorCard
import dev.chiraitori.anis.ui.components.StatCard
import dev.chiraitori.anis.ui.components.StatusShield
import dev.chiraitori.anis.ui.components.ThreatBreakdownCard
import dev.chiraitori.anis.ui.theme.AmberWarning
import dev.chiraitori.anis.ui.theme.CoralRed
import dev.chiraitori.anis.ui.theme.EmeraldPrimary
import dev.chiraitori.anis.ui.theme.IndigoPrimary
import dev.chiraitori.anis.ui.theme.shapes.ShapeCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onToggleVpn: () -> Unit,
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isVpnRunning by viewModel.isVpnRunning.collectAsState()
    val isStarting by viewModel.isStarting.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val upstreamDns by viewModel.upstreamDns.collectAsState()
    val logs by viewModel.queryLogs.collectAsState()
    val topBlocked by viewModel.topBlockedDomains.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val isPausedByTrusted by viewModel.isPausedByTrusted.collectAsState()
    val isCaInstalled by viewModel.isCaInstalledFlow.collectAsState()
    val isCaDismissed by viewModel.isCaDismissedFlow.collectAsState()
    val httpsFilteringEnabled by viewModel.httpsFilteringEnabledFlow.collectAsState()

    var showCaInstallDialog by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.checkIsCaInstalled()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp, bottom = 124.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))

            // App Header Banner with M3 Expressive Star Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Anis",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = dev.chiraitori.anis.ui.i18n.tr("app_subtitle", "System-Wide Adblocker & Firewall"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = ShapeCache.star4,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Dns,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Hero Protection Status Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeCache.smooth32,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isVpnRunning) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                StatusShield(
                    isActive = isVpnRunning,
                    isStarting = isStarting,
                    activeRulesCount = stats.activeRulesCount,
                    onToggle = onToggleVpn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp)
                )
            }
        }

        // Permanent HTTPS Deep Filtering Card (Interactive)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ShapeCache.smooth24)
                    .clickable {
                        if (!isCaInstalled) {
                            showCaInstallDialog = true
                        }
                    },
                shape = ShapeCache.smooth24,
                color = if (!isCaInstalled) AmberWarning.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, if (!isCaInstalled) AmberWarning.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = ShapeCache.star8,
                                color = if (!isCaInstalled) AmberWarning.copy(alpha = 0.22f) else EmeraldPrimary.copy(alpha = 0.2f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = if (!isCaInstalled) Icons.Filled.Warning else Icons.Filled.Security,
                                    contentDescription = null,
                                    tint = if (!isCaInstalled) AmberWarning else EmeraldPrimary,
                                    modifier = Modifier
                                        .padding(9.dp)
                                        .size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = dev.chiraitori.anis.ui.i18n.tr("install_ca_title", "Install HTTPS CA Certificate"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isCaInstalled) dev.chiraitori.anis.ui.i18n.tr("ca_installed_badge", "CA Installed • HTTPS Ready") else dev.chiraitori.anis.ui.i18n.tr("ca_uninstalled_badge", "CA Certificate Not Installed"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isCaInstalled) EmeraldPrimary else AmberWarning,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (!isCaInstalled) {
                            FilledTonalButton(
                                onClick = { showCaInstallDialog = true },
                                shapes = ButtonDefaults.shapes(
                                    shape = ShapeCache.smooth14,
                                    pressedShape = ShapeCache.star4
                                ),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = AmberWarning.copy(alpha = 0.25f),
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = dev.chiraitori.anis.ui.i18n.tr("setup", "Setup"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        } else {
                            androidx.compose.material3.Switch(
                                checked = httpsFilteringEnabled,
                                onCheckedChange = { viewModel.setHttpsFilteringEnabled(it) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = dev.chiraitori.anis.ui.i18n.tr("install_ca_desc", "CA certificate is required for deep HTTPS interception, in-app ad element blocking, and tracker sanitization. Tap here to install (supports Rootless & Root)."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Protection Profiles Carousel Card
        item {
            ProfileSelectorCard(
                profiles = profiles,
                activeProfile = activeProfile,
                onSelectProfile = { viewModel.switchToProfile(it) }
            )
        }

        // Threat Analytics Breakdown Bar
        item {
            ThreatBreakdownCard(
                stats = stats
            )
        }

        // Metrics Grid (2x2)
        item {
            Text(
                text = dev.chiraitori.anis.ui.i18n.tr("stats_title", "Live Filtering Statistics"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = dev.chiraitori.anis.ui.i18n.tr("stats_blocked", "Ads Blocked"),
                    value = formatNumber(stats.blockedQueries),
                    subtitle = dev.chiraitori.anis.ui.i18n.tr("stats_blocked_desc", "Ads & trackers dropped"),
                    icon = Icons.Filled.Block,
                    accentColor = CoralRed,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = dev.chiraitori.anis.ui.i18n.tr("stats_total", "Total Queries"),
                    value = formatNumber(stats.totalQueries),
                    subtitle = dev.chiraitori.anis.ui.i18n.tr("stats_total_desc", "DNS lookups handled"),
                    icon = Icons.Filled.QueryStats,
                    accentColor = IndigoPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = dev.chiraitori.anis.ui.i18n.tr("stats_block_rate", "Block Rate"),
                    value = "${String.format("%.1f", stats.blockRate)}%",
                    subtitle = dev.chiraitori.anis.ui.i18n.tr("stats_rate_desc", "Traffic filtered locally"),
                    icon = Icons.Filled.Speed,
                    accentColor = EmeraldPrimary,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = dev.chiraitori.anis.ui.i18n.tr("stats_active_rules", "Active Rules"),
                    value = formatNumber(stats.activeRulesCount.toLong()),
                    subtitle = dev.chiraitori.anis.ui.i18n.tr("stats_rules_desc", "Compiled block rules"),
                    icon = Icons.Filled.Checklist,
                    accentColor = AmberWarning,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Top Blocked Domains Card (from BlockAds logic)
        if (topBlocked.isNotEmpty()) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ShapeCache.smooth26,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Top Blocked Domains",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            TextButton(onClick = { onNavigate(AppDestination.LOGS) }) {
                                Text("View Analytics", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        topBlocked.take(4).forEach { stat ->
                            TopBlockedRow(stat = stat, onWhitelist = { viewModel.addWhitelistDomain(stat.domain) })
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }

        // Upstream DNS Selector Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeCache.smooth26,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = ShapeCache.star6,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Dns,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dev.chiraitori.anis.ui.i18n.tr("upstream_dns", "Upstream DNS"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${dev.chiraitori.anis.ui.i18n.tr("active", "Active")}: ${upstreamDns.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        TextButton(onClick = { onNavigate(AppDestination.SETTINGS) }) {
                            Text(dev.chiraitori.anis.ui.i18n.tr("configure", "Configure"), fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(DefaultDnsProviders.ALL) { provider ->
                            val isSelected = upstreamDns.id == provider.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setUpstreamDns(provider) },
                                label = { Text(provider.name.replace(" DNS", "").replace(" Public", "")) },
                                shape = ShapeCache.smoothPill,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }
        }

        // Recent Activity Feed Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dev.chiraitori.anis.ui.i18n.tr("live_stream", "Live DNS Stream"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                TextButton(onClick = { onNavigate(AppDestination.LOGS) }) {
                    Text("${dev.chiraitori.anis.ui.i18n.tr("view_all", "View All")} (${logs.size})", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Recent mini-feed
        if (logs.isEmpty()) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ShapeCache.smooth20,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isVpnRunning) "Listening for network requests..." else "Protection paused. Tap shield above to begin filtering.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(logs.take(4)) { log ->
                LogItemRow(
                    log = log,
                    onClick = { onNavigate(AppDestination.LOGS) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showCaInstallDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showCaInstallDialog = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = ShapeCache.star8,
                    color = AmberWarning.copy(alpha = 0.18f),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = dev.chiraitori.anis.ui.i18n.tr("ca_dialog_title", "Install HTTPS CA Certificate"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = dev.chiraitori.anis.ui.i18n.tr("ca_dialog_desc", "To filter encrypted ads, banners, and trackers inside apps and browsers, Anis installs a local CA certificate (works on rootless and rooted devices)."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 1. Rootless (AdGuard method)
                Surface(
                    shape = ShapeCache.smooth20,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = dev.chiraitori.anis.ui.i18n.tr("ca_rootless_title", "1. Rootless Installation (AdGuard style)"),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = EmeraldPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dev.chiraitori.anis.ui.i18n.tr("ca_rootless_desc", "Saves 'Anis-RootCA.crt' to Downloads and opens Android Security Settings. Tap 'Install a certificate' → 'CA certificate'."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        viewModel.caManager.exportCaToDownloads()
                                    }
                                    result.onSuccess { msg ->
                                        Toast.makeText(context, "$msg. Opening Certificate Settings...", Toast.LENGTH_LONG).show()
                                        try {
                                            context.startActivity(viewModel.caManager.createInstallCertIntent())
                                        } catch (_: Exception) {
                                            try {
                                                context.startActivity(Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS))
                                            } catch (_: Exception) {}
                                        }
                                        showCaInstallDialog = false
                                    }.onFailure { e ->
                                        Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = ShapeCache.smooth16,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(dev.chiraitori.anis.ui.i18n.tr("ca_btn_save_install", "Save & Install (Rootless)"), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Magisk (Root method)
                Surface(
                    shape = ShapeCache.smooth20,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = dev.chiraitori.anis.ui.i18n.tr("ca_root_title", "2. Magisk / KernelSU (Root)"),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = IndigoPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dev.chiraitori.anis.ui.i18n.tr("ca_root_desc", "Installs automatically as a system-trusted certificate module to /data/adb/modules/anis_root_ca (Reboot required)."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    val success = withContext(Dispatchers.IO) {
                                        viewModel.installSystemCaCert()
                                    }
                                    if (success) {
                                        Toast.makeText(context, "Magisk CA module installed! Please reboot your device.", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Root install failed or not rooted. Use Rootless option above.", Toast.LENGTH_SHORT).show()
                                    }
                                    showCaInstallDialog = false
                                }
                            },
                            shape = ShapeCache.smooth16,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(dev.chiraitori.anis.ui.i18n.tr("ca_btn_root", "Install (Root Magisk)"), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            val certPem = viewModel.caManager.getOrCreateCaCertificatePem()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Anis Root CA Certificate", certPem)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "CA Certificate PEM copied to clipboard!", Toast.LENGTH_LONG).show()
                            showCaInstallDialog = false
                        }
                    ) {
                        Text(dev.chiraitori.anis.ui.i18n.tr("ca_copy_pem", "Copy PEM"))
                    }

                    TextButton(
                        onClick = { showCaInstallDialog = false }
                    ) {
                        Text(dev.chiraitori.anis.ui.i18n.tr("close", "Close"))
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBlockedRow(
    stat: TopBlockedDomainStat,
    onWhitelist: () -> Unit
) {
    Surface(
        shape = ShapeCache.smooth14,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stat.domain,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${stat.count} blocks (${String.format("%.1f", stat.percentage)}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            CategoryChip(category = stat.category)
        }
    }
}

private fun formatNumber(count: Long): String {
    return java.text.NumberFormat.getIntegerInstance().format(count)
}
