package dev.chiraitori.anis.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

    var showCaInstallDialog by remember { mutableStateOf(false) }

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
                        text = "System-Wide Adblocker & Firewall",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = ShapeCache.star6,
                    color = if (isVpnRunning) EmeraldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = if (isVpnRunning) Icons.Filled.Security else Icons.Filled.Block,
                        contentDescription = null,
                        tint = if (isVpnRunning) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(11.dp)
                            .size(24.dp)
                    )
                }
            }
        }

        // Hero Status Shield Section
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeCache.smooth32,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                StatusShield(
                    isActive = isVpnRunning,
                    isStarting = isStarting,
                    activeRulesCount = stats.activeRulesCount,
                    onToggle = { viewModel.toggleVpn() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp)
                )
            }
        }

        // HTTPS Root CA Certificate Warning Box (Auto-hides if already installed or dismissed)
        if (!isCaInstalled && !isCaDismissed) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ShapeCache.smooth24)
                        .clickable { showCaInstallDialog = true },
                    shape = ShapeCache.smooth24,
                    color = AmberWarning.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.35f))
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
                                    color = AmberWarning.copy(alpha = 0.22f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = AmberWarning,
                                        modifier = Modifier
                                            .padding(9.dp)
                                            .size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Text(
                                    text = "Install CA Certificate",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            FilledTonalButton(
                                onClick = { showCaInstallDialog = true },
                                shape = ShapeCache.smooth14,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = AmberWarning.copy(alpha = 0.25f),
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Setup",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Install Anis CA certificate for deep in-app cosmetic HTTPS filtering and ad stripping.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
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
                adsBlockedCount = stats.blockedQueries,
                totalBlockedCount = stats.blockedQueries + stats.blockedFirewall
            )
        }

        // Metrics Grid (2x2)
        item {
            Text(
                text = "Live Filtering Statistics",
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
                    title = "Ads Blocked",
                    value = formatNumber(stats.blockedQueries),
                    subtitle = "Ads & trackers dropped",
                    icon = Icons.Filled.Block,
                    accentColor = CoralRed,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Total Queries",
                    value = formatNumber(stats.totalQueries),
                    subtitle = "DNS lookups handled",
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
                    title = "Block Rate",
                    value = "${String.format("%.1f", stats.blockRate)}%",
                    subtitle = "Traffic filtered locally",
                    icon = Icons.Filled.Speed,
                    accentColor = EmeraldPrimary,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Active Rules",
                    value = formatNumber(stats.activeRulesCount.toLong()),
                    subtitle = "Compiled block rules",
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
                                    text = "Upstream DNS",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Active: ${upstreamDns.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        TextButton(onClick = { onNavigate(AppDestination.SETTINGS) }) {
                            Text("Configure", fontWeight = FontWeight.Bold)
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
                    text = "Live DNS Stream",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                TextButton(onClick = { onNavigate(AppDestination.LOGS) }) {
                    Text("View All (${logs.size})", fontWeight = FontWeight.Bold)
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
        AlertDialog(
            onDismissRequest = { showCaInstallDialog = false },
            icon = {
                Surface(
                    shape = ShapeCache.star8,
                    color = AmberWarning.copy(alpha = 0.18f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        tint = AmberWarning,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Install HTTPS CA Certificate",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "To filter encrypted ads, banners, and trackers inside apps and browsers, Anis requires a Root CA certificate installed on your device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = ShapeCache.smooth16,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "1. Magisk / KernelSU (Root)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = "Installs automatically as a system-trusted certificate module to /data/adb/modules/anis_root_ca (Reboot required).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = ShapeCache.smooth16,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "2. User CA Certificate",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = IndigoPrimary
                            )
                            Text(
                                text = "Export the .CRT file and install via Android Settings → Security → Encryption & Credentials → Install CA Certificate.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val success = withContext(Dispatchers.IO) {
                                viewModel.installSystemCaCert()
                            }
                            if (success) {
                                Toast.makeText(context, "Magisk CA module installed! Please reboot your device.", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Root install failed or not rooted. You can export certificate manually.", Toast.LENGTH_SHORT).show()
                            }
                            showCaInstallDialog = false
                        }
                    },
                    shape = ShapeCache.smooth16
                ) {
                    Text("Install (Root Magisk)", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            viewModel.dismissCaWarning()
                            showCaInstallDialog = false
                        }
                    ) {
                        Text("Don't show again")
                    }
                    TextButton(
                        onClick = {
                            val certPem = viewModel.caManager.getOrCreateCaCertificatePem()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Anis Root CA Certificate", certPem)
                            clipboard.setPrimaryClip(clip)
                            viewModel.markCaInstalled()
                            Toast.makeText(context, "CA Certificate PEM copied to clipboard!", Toast.LENGTH_LONG).show()
                            showCaInstallDialog = false
                        }
                    ) {
                        Text("Copy PEM")
                    }
                }
            }
        )
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
