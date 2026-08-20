package dev.chiraitori.anis.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.chiraitori.anis.data.model.AppLanguage
import dev.chiraitori.anis.data.model.AutoUpdateFrequency
import dev.chiraitori.anis.data.model.CustomDnsRule
import dev.chiraitori.anis.data.model.DefaultDnsProviders
import dev.chiraitori.anis.data.model.DnsProtocol
import dev.chiraitori.anis.data.model.DnsResponseType
import dev.chiraitori.anis.data.model.LogRetention
import dev.chiraitori.anis.data.model.ProtectionMode
import dev.chiraitori.anis.data.model.ThemeMode
import dev.chiraitori.anis.data.model.UpstreamDnsProvider
import dev.chiraitori.anis.ui.MainViewModel
import dev.chiraitori.anis.ui.theme.AmberWarning
import dev.chiraitori.anis.ui.theme.CoralRed
import dev.chiraitori.anis.ui.theme.EmeraldPrimary
import dev.chiraitori.anis.ui.theme.shapes.ShapeCache
import dev.chiraitori.anis.vpn.root.RootIptablesManager
import dev.chiraitori.anis.vpn.root.RootUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Advanced Settings Screen crafted after blockads-android and PixelPlayer.
 * Features categorized Material 3 cards, rich dialogs, and deep engine controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.checkIsCaInstalled()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Engine & Protection Flows
    val protectionMode by viewModel.protectionMode.collectAsState()
    val upstreamDns by viewModel.upstreamDns.collectAsState()
    val dnsProtocol by viewModel.dnsProtocol.collectAsState()
    val dnsResponseType by viewModel.dnsResponseType.collectAsState()
    val autoReconnect by viewModel.autoReconnect.collectAsState()
    val safeSearchEnabled by viewModel.safeSearchFlow.collectAsState()
    val youtubeRestricted by viewModel.youtubeRestrictedFlow.collectAsState()
    val isRootAvailable by viewModel.isRootAvailableFlow.collectAsState()
    val isCaInstalled by viewModel.isCaInstalledFlow.collectAsState()
    val httpsFilteringEnabled by viewModel.httpsFilteringEnabledFlow.collectAsState()

    // Application & Network Flows
    val whitelist by viewModel.whitelist.collectAsState()
    val blacklist by viewModel.blacklist.collectAsState()
    val whitelistedApps by viewModel.whitelistedApps.collectAsState()
    val firewallApps by viewModel.firewallApps.collectAsState()
    val customRules by viewModel.customRules.collectAsState()
    val trustedSsids by viewModel.trustedSsids.collectAsState()
    val pauseOnTrusted by viewModel.pauseOnTrusted.collectAsState()

    // Filter & Auto-Update Flows
    val autoUpdateFrequency by viewModel.autoUpdateFrequency.collectAsState()
    val autoUpdateWifiOnly by viewModel.autoUpdateWifiOnly.collectAsState()
    val autoUpdateNotification by viewModel.autoUpdateNotification.collectAsState()

    // Appearance & Data Flows
    val themeMode by viewModel.themeMode.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val logRetention by viewModel.logRetention.collectAsState()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
    val startOnBoot by viewModel.startOnBoot.collectAsState()

    // Dialog States
    var showDnsProviderDialog by remember { mutableStateOf(false) }
    var showCustomDnsDialog by remember { mutableStateOf(false) }
    var showResponseTypeDialog by remember { mutableStateOf(false) }
    var showAutoUpdateDialog by remember { mutableStateOf(false) }
    var showLogRetentionDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showWhitelistDialog by remember { mutableStateOf(false) }
    var showBlacklistDialog by remember { mutableStateOf(false) }
    var showCustomRulesDialog by remember { mutableStateOf(false) }
    var showTrustedWifiDialog by remember { mutableStateOf(false) }
    var showAppBypassDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showResetStatsConfirm by remember { mutableStateOf(false) }
    var showCaInstallConfirm by remember { mutableStateOf(false) }

    val trustedWifiPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (granted) {
            showTrustedWifiDialog = true
        } else {
            Toast.makeText(context, "Location permission is required to identify the current Wi-Fi network", Toast.LENGTH_LONG).show()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setAutoUpdateNotification(granted)
        if (!granted) {
            Toast.makeText(context, "Update notifications remain disabled", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 124.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // App Header Banner
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dev.chiraitori.anis.ui.i18n.tr("settings_title", "Settings & Engine"),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = dev.chiraitori.anis.ui.i18n.tr("settings_desc", "Fine-tune DNS routing, root proxy, filter schedules, and privacy rules"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ═══════════════════════════════════════════════════════════════════
        // SECTION 1: PROTECTION & DNS ENGINE
        // ═══════════════════════════════════════════════════════════════════
        item {
            SettingsSectionHeader(
                title = dev.chiraitori.anis.ui.i18n.tr("sec_engine", "Protection & DNS Engine"),
                icon = Icons.Filled.Shield,
                description = dev.chiraitori.anis.ui.i18n.tr("sec_engine_desc", "Configure packet interception, upstream resolvers, and safe browsing")
            )
        }

        item {
            SettingsCard {
                // Interception Mode Switch
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = ShapeCache.smooth16,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = if (protectionMode == ProtectionMode.ROOT_PROXY) Icons.Filled.PowerSettingsNew else Icons.Filled.VpnKey,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = dev.chiraitori.anis.ui.i18n.tr("interception_arch", "Interception Architecture"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (protectionMode == ProtectionMode.ROOT_PROXY) dev.chiraitori.anis.ui.i18n.tr("interception_root_desc", "Zero-overhead iptables transparent proxy") else dev.chiraitori.anis.ui.i18n.tr("interception_vpn_desc", "Rootless local TUN VPN interface"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = protectionMode == ProtectionMode.LOCAL_VPN,
                            onClick = { viewModel.setProtectionMode(ProtectionMode.LOCAL_VPN) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            icon = {}
                        ) {
                            Text(
                                text = dev.chiraitori.anis.ui.i18n.tr("mode_local_vpn", "Local VPN"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        SegmentedButton(
                            selected = protectionMode == ProtectionMode.ROOT_PROXY,
                            onClick = {
                                if (isRootAvailable) {
                                    viewModel.setProtectionMode(ProtectionMode.ROOT_PROXY)
                                } else {
                                    Toast.makeText(context, dev.chiraitori.anis.ui.i18n.I18n.get("root_not_detected", appLanguage, "Root access ('su') not detected on device"), Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            icon = {}
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = dev.chiraitori.anis.ui.i18n.tr("mode_root_iptables", "Root iptables"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!isRootAvailable) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        modifier = Modifier.size(8.dp)
                                    ) {}
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Upstream DNS Provider
                SettingsClickRow(
                    title = dev.chiraitori.anis.ui.i18n.tr("upstream_dns_resolver", "Upstream DNS Resolver"),
                    subtitle = "${upstreamDns.name} (${upstreamDns.primaryIp})",
                    icon = Icons.Filled.Dns,
                    badge = if (upstreamDns.isEncrypted) dev.chiraitori.anis.ui.i18n.tr("encrypted_badge", "Encrypted") else null,
                    onClick = { showDnsProviderDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // DNS Protocol (DoH vs Plain UDP)
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = dev.chiraitori.anis.ui.i18n.tr("resolver_protocol", "Resolver Transport Protocol"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (dnsProtocol == DnsProtocol.DOH) dev.chiraitori.anis.ui.i18n.tr("proto_doh_desc", "Encrypted DNS-over-HTTPS (TLS port 443)") else dev.chiraitori.anis.ui.i18n.tr("proto_udp_desc", "Standard Plain UDP Port 53"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = dnsProtocol == DnsProtocol.DOH,
                            onClick = { viewModel.setDnsProtocol(DnsProtocol.DOH) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            icon = {}
                        ) {
                            Text(
                                text = dev.chiraitori.anis.ui.i18n.tr("proto_doh", "DoH (Encrypted)"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        SegmentedButton(
                            selected = dnsProtocol == DnsProtocol.PLAIN_UDP,
                            onClick = { viewModel.setDnsProtocol(DnsProtocol.PLAIN_UDP) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            icon = {}
                        ) {
                            Text(
                                text = dev.chiraitori.anis.ui.i18n.tr("proto_udp", "Plain UDP"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // DNS Response Type
                SettingsClickRow(
                    title = dev.chiraitori.anis.ui.i18n.tr("dns_response_block", "DNS Response on Block"),
                    subtitle = "${dnsResponseType.title} — ${dnsResponseType.description}",
                    icon = Icons.Filled.Block,
                    onClick = { showResponseTypeDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // SafeSearch Enforcement
                SettingsToggleRow(
                    title = dev.chiraitori.anis.ui.i18n.tr("safesearch_title", "Strict SafeSearch Enforcement"),
                    subtitle = dev.chiraitori.anis.ui.i18n.tr("safesearch_desc", "Force family filtering on Google and Bing"),
                    icon = Icons.Filled.Security,
                    checked = safeSearchEnabled,
                    onCheckedChange = { viewModel.setSafeSearchEnabled(it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // YouTube Restricted Mode
                SettingsToggleRow(
                    title = dev.chiraitori.anis.ui.i18n.tr("youtube_restricted_title", "YouTube Restricted Mode"),
                    subtitle = dev.chiraitori.anis.ui.i18n.tr("youtube_restricted_desc", "Restrict potentially mature videos across all YouTube apps and browsers"),
                    icon = Icons.Filled.Shield,
                    checked = youtubeRestricted,
                    onCheckedChange = { viewModel.setYoutubeRestrictedMode(it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Auto-Reconnect
                SettingsToggleRow(
                    title = dev.chiraitori.anis.ui.i18n.tr("autoreconnect_title", "Auto-Reconnect & Roaming Recovery"),
                    subtitle = dev.chiraitori.anis.ui.i18n.tr("autoreconnect_desc", "Automatically restore DNS protection on network handover (Wi-Fi ↔ Cellular)"),
                    icon = Icons.Filled.NetworkCheck,
                    checked = autoReconnect,
                    onCheckedChange = { viewModel.setAutoReconnect(it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // HTTPS Deep Filtering (MITM)
                SettingsToggleRow(
                    title = dev.chiraitori.anis.ui.i18n.tr("https_deep_filtering", "HTTPS Deep Filtering (MITM)"),
                    subtitle = if (isCaInstalled) {
                        dev.chiraitori.anis.ui.i18n.tr("https_installed_desc", "Decrypt and filter in-app HTTPS traffic, block URL-level ads, and strip tracking queries")
                    } else {
                        dev.chiraitori.anis.ui.i18n.tr("https_uninstalled_desc", "Requires Root CA certificate installation. Tap to set up CA certificate.")
                    },
                    icon = Icons.Filled.Security,
                    checked = httpsFilteringEnabled && isCaInstalled,
                    onCheckedChange = { enabled ->
                        if (isCaInstalled) {
                            viewModel.setHttpsFilteringEnabled(enabled)
                        } else {
                            showCaInstallConfirm = true
                        }
                    }
                )

                if (isRootAvailable) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Install Magisk System Certificate
                    SettingsClickRow(
                        title = dev.chiraitori.anis.ui.i18n.tr("install_magisk_ca_title", "Install Magisk Root System CA"),
                        subtitle = dev.chiraitori.anis.ui.i18n.tr("install_magisk_ca_desc", "Write Anis CA to /data/adb/modules for HTTPS inspection across all apps"),
                        icon = Icons.Filled.VpnKey,
                        badge = if (isCaInstalled) dev.chiraitori.anis.ui.i18n.tr("badge_installed", "Installed") else dev.chiraitori.anis.ui.i18n.tr("badge_root", "Root"),
                        onClick = { showCaInstallConfirm = true }
                    )
                } else {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    SettingsClickRow(
                        title = "Install Rootless HTTPS CA",
                        subtitle = "Save the CA certificate and open Android certificate settings",
                        icon = Icons.Filled.VpnKey,
                        badge = if (isCaInstalled) "Installed" else "Rootless",
                        onClick = { showCaInstallConfirm = true }
                    )
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // SECTION 2: APPLICATIONS & ROUTING
        // ═══════════════════════════════════════════════════════════════════
        item {
            SettingsSectionHeader(
                title = dev.chiraitori.anis.ui.i18n.tr("sec_apps", "Applications & Routing"),
                icon = Icons.Filled.Apps,
                description = dev.chiraitori.anis.ui.i18n.tr("sec_apps_desc", "Manage bypassed applications and trusted home/office Wi-Fi networks")
            )
        }

        item {
            SettingsCard {
                // Whitelisted Bypassed Apps
                SettingsClickRow(
                    title = dev.chiraitori.anis.ui.i18n.tr("bypassed_apps", "Bypassed Applications"),
                    subtitle = "${whitelistedApps.size} ${dev.chiraitori.anis.ui.i18n.tr("bypassed_apps_desc", "apps bypass DNS inspection entirely")}",
                    icon = Icons.Filled.Apps,
                    badge = "${whitelistedApps.size}",
                    onClick = { showAppBypassDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Trusted Wi-Fi Networks
                SettingsClickRow(
                    title = dev.chiraitori.anis.ui.i18n.tr("trusted_wifi", "Trusted Wi-Fi SSIDs"),
                    subtitle = if (pauseOnTrusted) "${trustedSsids.size} ${dev.chiraitori.anis.ui.i18n.tr("trusted_wifi_desc_active", "trusted networks (Auto-pause active)")}" else dev.chiraitori.anis.ui.i18n.tr("trusted_wifi_desc_disabled", "Disabled (Protection active on all Wi-Fi)"),
                    icon = Icons.Filled.Wifi,
                    badge = if (pauseOnTrusted) dev.chiraitori.anis.ui.i18n.tr("badge_active", "Active") else dev.chiraitori.anis.ui.i18n.tr("badge_off", "Off"),
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            showTrustedWifiDialog = true
                        } else {
                            trustedWifiPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            )
                        }
                    }
                )
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // SECTION 3: FILTERS & CUSTOM RULES
        // ═══════════════════════════════════════════════════════════════════
        item {
            SettingsSectionHeader(
                title = dev.chiraitori.anis.ui.i18n.tr("sec_filters", "Filters & Rule Management"),
                icon = Icons.Filled.FilterList,
                description = dev.chiraitori.anis.ui.i18n.tr("sec_filters_desc", "Configure automated blocklist updates, domain allowlists, and DNS rewrites")
            )
        }

        item {
            SettingsCard {
                // Auto-Update Frequency
                SettingsClickRow(
                    title = dev.chiraitori.anis.ui.i18n.tr("update_freq", "Blocklist Update Frequency"),
                    subtitle = autoUpdateFrequency.title,
                    icon = Icons.Outlined.Timer,
                    onClick = { showAutoUpdateDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Wi-Fi Only Updates
                SettingsToggleRow(
                    title = dev.chiraitori.anis.ui.i18n.tr("wifi_only", "Update Over Wi-Fi Only"),
                    subtitle = dev.chiraitori.anis.ui.i18n.tr("wifi_only_desc", "Prevent downloading blocklist rule updates on cellular metered connections"),
                    icon = Icons.Filled.Wifi,
                    checked = autoUpdateWifiOnly,
                    onCheckedChange = { viewModel.setAutoUpdateWifiOnly(it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Update Notifications
                SettingsToggleRow(
                    title = dev.chiraitori.anis.ui.i18n.tr("update_notifications", "Rule Update Notifications"),
                    subtitle = dev.chiraitori.anis.ui.i18n.tr("update_notif_desc", "Show notification when new blocklist rules are compiled and loaded"),
                    icon = Icons.Filled.Notifications,
                    checked = autoUpdateNotification,
                    onCheckedChange = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.setAutoUpdateNotification(enabled)
                        }
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Domain Whitelist
                SettingsClickRow(
                    title = dev.chiraitori.anis.ui.i18n.tr("whitelist_domains", "Domain Whitelist (Always Allow)"),
                    subtitle = "${whitelist.size} ${dev.chiraitori.anis.ui.i18n.tr("whitelist_domains_desc", "custom allowed domains")}",
                    icon = Icons.Filled.CheckCircle,
                    badge = "${whitelist.size}",
                    onClick = { showWhitelistDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Domain Blacklist
                SettingsClickRow(
                    title = dev.chiraitori.anis.ui.i18n.tr("blacklist_domains", "Domain Blacklist (Always Block)"),
                    subtitle = "${blacklist.size} ${dev.chiraitori.anis.ui.i18n.tr("blacklist_domains_desc", "custom blocked domains")}",
                    icon = Icons.Filled.Block,
                    badge = "${blacklist.size}",
                    onClick = { showBlacklistDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Custom DNS Rewrites
                SettingsClickRow(
                    title = dev.chiraitori.anis.ui.i18n.tr("dns_rewrites", "Custom DNS Rewrites & Host Mappings"),
                    subtitle = "${customRules.size} ${dev.chiraitori.anis.ui.i18n.tr("dns_rewrites_desc", "custom IP redirects")}",
                    icon = Icons.Filled.Edit,
                    badge = "${customRules.size}",
                    onClick = { showCustomRulesDialog = true }
                )
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // SECTION 4: APPEARANCE & SYSTEM
        // ═══════════════════════════════════════════════════════════════════
        item {
            SettingsSectionHeader(
                title = dev.chiraitori.anis.ui.i18n.tr("sec_appearance", "Appearance & System"),
                icon = Icons.Filled.Palette,
                description = dev.chiraitori.anis.ui.i18n.tr("sec_appearance_desc", "Customize color themes, tactile haptic response, and boot actions")
            )
        }

        item {
            SettingsCard {
                // Theme Mode
                SettingsClickRow(
                    title = dev.chiraitori.anis.ui.i18n.tr("app_theme", "Application Theme"),
                    subtitle = when (themeMode) {
                        ThemeMode.SYSTEM -> dev.chiraitori.anis.ui.i18n.tr("theme_system", "System Default")
                        ThemeMode.DARK -> dev.chiraitori.anis.ui.i18n.tr("theme_dark", "Dark Theme")
                        ThemeMode.LIGHT -> dev.chiraitori.anis.ui.i18n.tr("theme_light", "Light Theme")
                        ThemeMode.AMOLED -> dev.chiraitori.anis.ui.i18n.tr("theme_amoled", "AMOLED Black (Pitch Dark)")
                    },
                    icon = Icons.Filled.Palette,
                    onClick = { showThemeDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // App Language
                SettingsClickRow(
                    title = dev.chiraitori.anis.ui.i18n.tr("app_language", "Application Language"),
                    subtitle = appLanguage.displayName,
                    icon = Icons.Filled.Language,
                    badge = if (appLanguage == AppLanguage.SYSTEM) "Auto" else appLanguage.languageCode.uppercase(),
                    onClick = { showLanguageDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Tactile Haptics
                SettingsToggleRow(
                    title = dev.chiraitori.anis.ui.i18n.tr("haptics_feedback", "Expressive Haptic Feedback"),
                    subtitle = dev.chiraitori.anis.ui.i18n.tr("haptics_desc", "Vibrate gently on tab switches and toggle clicks (PixelPlayer style)"),
                    icon = Icons.Filled.Vibration,
                    checked = hapticsEnabled,
                    onCheckedChange = { viewModel.setHapticsEnabled(it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Start on Boot
                SettingsToggleRow(
                    title = dev.chiraitori.anis.ui.i18n.tr("start_on_boot", "Start Protection on Boot"),
                    subtitle = dev.chiraitori.anis.ui.i18n.tr("start_on_boot_desc", "Automatically activate DNS adblocker when your phone turns on"),
                    icon = Icons.Filled.PowerSettingsNew,
                    checked = startOnBoot,
                    onCheckedChange = viewModel::setStartOnBoot
                )
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // SECTION 5: DATA, BACKUP & LOGS
        // ═══════════════════════════════════════════════════════════════════
        item {
            SettingsSectionHeader(
                title = dev.chiraitori.anis.ui.i18n.tr("sec_data", "Data, Backup & Logs"),
                icon = Icons.Filled.Storage,
                description = dev.chiraitori.anis.ui.i18n.tr("sec_data_desc", "Export/import settings configurations, manage log retention, and reset stats")
            )
        }

        item {
            SettingsCard {
                // Query Log Retention
                SettingsClickRow(
                    title = dev.chiraitori.anis.ui.i18n.tr("log_retention", "Query Log Retention Period"),
                    subtitle = logRetention.title,
                    icon = Icons.Outlined.HourglassEmpty,
                    onClick = { showLogRetentionDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Export Settings
                SettingsClickRow(
                    title = dev.chiraitori.anis.ui.i18n.tr("export_backup", "Export Settings & Rules Backup"),
                    subtitle = dev.chiraitori.anis.ui.i18n.tr("export_backup_desc", "Generate and copy a portable JSON backup of your configuration"),
                    icon = Icons.Filled.Upload,
                    onClick = { showBackupDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Import Settings
                SettingsClickRow(
                    title = dev.chiraitori.anis.ui.i18n.tr("import_backup", "Import & Restore Configuration"),
                    subtitle = dev.chiraitori.anis.ui.i18n.tr("import_backup_desc", "Restore blocklists, custom rules, whitelist, and settings from JSON"),
                    icon = Icons.Filled.Download,
                    onClick = { showRestoreDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Reset Analytics
                SettingsClickRow(
                    title = dev.chiraitori.anis.ui.i18n.tr("clear_logs", "Clear All Logs & Analytics"),
                    subtitle = dev.chiraitori.anis.ui.i18n.tr("clear_logs_desc", "Permanently purge query history and reset blocked counter statistics"),
                    icon = Icons.Filled.Delete,
                    onClick = { showResetStatsConfirm = true }
                )
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // SECTION 6: ABOUT & CREDITS
        // ═══════════════════════════════════════════════════════════════════
        item {
            SettingsSectionHeader(
                title = dev.chiraitori.anis.ui.i18n.tr("sec_about", "About & Credits"),
                icon = Icons.Filled.Info,
                description = dev.chiraitori.anis.ui.i18n.tr("sec_about_desc", "Author credits, open source repository, and engine diagnostics")
            )
        }

        item {
            SettingsCard {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Anis",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = dev.chiraitori.anis.ui.i18n.tr("version_label", "Version 0.1 (Material 3 Expressive)"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = ShapeCache.star8,
                            color = EmeraldPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.padding(11.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = dev.chiraitori.anis.ui.i18n.tr("app_tagline", "Next-generation DNS adblocker and kernel packet firewall for Android. Built with Material 3 Expressive UI, local VPN tunneling, and transparent root iptables routing."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Creator & GitHub Credit Card (chiraitori)
                    Surface(
                        shape = ShapeCache.smooth20,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Security,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(9.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = dev.chiraitori.anis.ui.i18n.tr("created_by", "Created by chiraitori"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "github.com/chiraitori",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/chiraitori"))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "https://github.com/chiraitori", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = ShapeCache.smooth14,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text("GitHub", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = ShapeCache.smooth12,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = dev.chiraitori.anis.ui.i18n.tr("root_status_label", "Root Status"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (isRootAvailable) dev.chiraitori.anis.ui.i18n.tr("root_available", "Available (su)") else dev.chiraitori.anis.ui.i18n.tr("rootless_mode", "Rootless Mode"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRootAvailable) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            shape = ShapeCache.smooth12,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = dev.chiraitori.anis.ui.i18n.tr("engine_state_label", "Engine State"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = dev.chiraitori.anis.ui.i18n.tr("engine_version", "Go/gVisor tunnel engine"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // DIALOGS & SHEET PICKERS
    // ═══════════════════════════════════════════════════════════════════

    // 1. Upstream DNS Provider Dialog
    if (showDnsProviderDialog) {
        AlertDialog(
            onDismissRequest = { showDnsProviderDialog = false },
            title = {
                Text(
                    text = dev.chiraitori.anis.ui.i18n.tr("select_upstream_dns", "Select Upstream DNS"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    DefaultDnsProviders.ALL.forEach { provider ->
                        val isSelected = upstreamDns.id == provider.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ShapeCache.smooth16)
                                .clickable {
                                    viewModel.setUpstreamDns(provider)
                                    showDnsProviderDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    viewModel.setUpstreamDns(provider)
                                    showDnsProviderDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = provider.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (provider.isEncrypted) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = ShapeCache.smooth8,
                                            color = EmeraldPrimary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "DoH",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = EmeraldPrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${provider.primaryIp} • ${provider.category.displayName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            showDnsProviderDialog = false
                            showCustomDnsDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = ShapeCache.smooth16
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(dev.chiraitori.anis.ui.i18n.tr("config_custom_dns", "Configure Custom DNS Endpoint"))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDnsProviderDialog = false }) {
                    Text(dev.chiraitori.anis.ui.i18n.tr("close", "Close"))
                }
            }
        )
    }

    // 2. Custom DNS Endpoint Editor Dialog
    if (showCustomDnsDialog) {
        var customName by remember { mutableStateOf(upstreamDns.name) }
        var primaryIp by remember { mutableStateOf(upstreamDns.primaryIp) }
        var secondaryIp by remember { mutableStateOf(upstreamDns.secondaryIp) }
        var dohUrl by remember { mutableStateOf(upstreamDns.dohUrl ?: "") }

        AlertDialog(
            onDismissRequest = { showCustomDnsDialog = false },
            title = {
                Text(dev.chiraitori.anis.ui.i18n.tr("custom_dns_server", "Custom DNS Server"), fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text(dev.chiraitori.anis.ui.i18n.tr("server_name", "Server Name")) },
                        singleLine = true,
                        shape = ShapeCache.smooth14,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = primaryIp,
                        onValueChange = { primaryIp = it },
                        label = { Text(dev.chiraitori.anis.ui.i18n.tr("primary_ip", "Primary IP (e.g. 1.1.1.1)")) },
                        singleLine = true,
                        shape = ShapeCache.smooth14,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = secondaryIp,
                        onValueChange = { secondaryIp = it },
                        label = { Text(dev.chiraitori.anis.ui.i18n.tr("secondary_ip", "Secondary IP (Optional)")) },
                        singleLine = true,
                        shape = ShapeCache.smooth14,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dohUrl,
                        onValueChange = { dohUrl = it },
                        label = { Text(dev.chiraitori.anis.ui.i18n.tr("doh_url", "DoH URL (e.g. https://cloudflare-dns.com/dns-query)")) },
                        singleLine = true,
                        shape = ShapeCache.smooth14,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (primaryIp.isNotBlank()) {
                            viewModel.setCustomUpstreamDns(
                                name = customName.ifBlank { "Custom Resolver" },
                                primaryIp = primaryIp.trim(),
                                secondaryIp = secondaryIp.trim(),
                                dohUrl = dohUrl.trim().ifBlank { null }
                            )
                            showCustomDnsDialog = false
                        }
                    },
                    shape = ShapeCache.smooth16
                ) {
                    Text(dev.chiraitori.anis.ui.i18n.tr("save_resolver", "Save Resolver"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDnsDialog = false }) {
                    Text(dev.chiraitori.anis.ui.i18n.tr("cancel", "Cancel"))
                }
            }
        )
    }

    // 3. DNS Response Type Picker Dialog
    if (showResponseTypeDialog) {
        AlertDialog(
            onDismissRequest = { showResponseTypeDialog = false },
            title = { Text(dev.chiraitori.anis.ui.i18n.tr("dns_response_block", "DNS Response on Block"), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    DnsResponseType.values().forEach { type ->
                        val isSelected = dnsResponseType == type
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ShapeCache.smooth16)
                                .clickable {
                                    viewModel.setDnsResponseType(type)
                                    showResponseTypeDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    viewModel.setDnsResponseType(type)
                                    showResponseTypeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(type.title, fontWeight = FontWeight.Bold)
                                Text(type.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showResponseTypeDialog = false }) { Text(dev.chiraitori.anis.ui.i18n.tr("close", "Close")) }
            }
        )
    }

    // 4. Auto-Update Frequency Picker Dialog
    if (showAutoUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showAutoUpdateDialog = false },
            title = { Text(dev.chiraitori.anis.ui.i18n.tr("update_freq", "Blocklist Update Schedule"), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    AutoUpdateFrequency.values().forEach { freq ->
                        val isSelected = autoUpdateFrequency == freq
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ShapeCache.smooth16)
                                .clickable {
                                    viewModel.setAutoUpdateFrequency(freq)
                                    showAutoUpdateDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    viewModel.setAutoUpdateFrequency(freq)
                                    showAutoUpdateDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(freq.title, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAutoUpdateDialog = false }) { Text(dev.chiraitori.anis.ui.i18n.tr("close", "Close")) }
            }
        )
    }

    // 5. Theme Mode Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(dev.chiraitori.anis.ui.i18n.tr("app_theme", "Application Theme"), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    ThemeMode.values().forEach { mode ->
                        val isSelected = themeMode == mode
                        val modeLabel = when (mode) {
                            ThemeMode.SYSTEM -> dev.chiraitori.anis.ui.i18n.tr("theme_system", "System Default")
                            ThemeMode.DARK -> dev.chiraitori.anis.ui.i18n.tr("theme_dark", "Dark Theme")
                            ThemeMode.LIGHT -> dev.chiraitori.anis.ui.i18n.tr("theme_light", "Light Theme")
                            ThemeMode.AMOLED -> dev.chiraitori.anis.ui.i18n.tr("theme_amoled", "AMOLED Black (Pitch Dark)")
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ShapeCache.smooth16)
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(modeLabel, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text(dev.chiraitori.anis.ui.i18n.tr("close", "Close")) }
            }
        )
    }

    // 5.1 Application Language Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(dev.chiraitori.anis.ui.i18n.tr("select_app_language", "Select App Language"), fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.height(280.dp)) {
                    items(listOf(AppLanguage.SYSTEM, AppLanguage.ENGLISH, AppLanguage.VIETNAMESE)) { lang ->
                        val isSelected = appLanguage == lang
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ShapeCache.smooth16)
                                .clickable {
                                    viewModel.setAppLanguage(lang)
                                    dev.chiraitori.anis.ui.i18n.I18n.applyLocale(context, lang)
                                    Toast.makeText(context, "Language: ${lang.displayName}", Toast.LENGTH_SHORT).show()
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 8.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    viewModel.setAppLanguage(lang)
                                    dev.chiraitori.anis.ui.i18n.I18n.applyLocale(context, lang)
                                    Toast.makeText(context, "Language: ${lang.displayName}", Toast.LENGTH_SHORT).show()
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(lang.displayName, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text(dev.chiraitori.anis.ui.i18n.tr("close", "Close")) }
            }
        )
    }

    // 6. Log Retention Dialog
    if (showLogRetentionDialog) {
        AlertDialog(
            onDismissRequest = { showLogRetentionDialog = false },
            title = { Text(dev.chiraitori.anis.ui.i18n.tr("query_log_retention", "Query Log Retention"), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    LogRetention.values().forEach { retention ->
                        val isSelected = logRetention == retention
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ShapeCache.smooth16)
                                .clickable {
                                    viewModel.setLogRetention(retention)
                                    showLogRetentionDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    viewModel.setLogRetention(retention)
                                    showLogRetentionDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(retention.title, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLogRetentionDialog = false }) { Text(dev.chiraitori.anis.ui.i18n.tr("close", "Close")) }
            }
        )
    }

    // 7. Whitelist Dialog
    if (showWhitelistDialog) {
        DomainListManagerDialog(
            title = dev.chiraitori.anis.ui.i18n.tr("domain_whitelist_title", "Domain Whitelist"),
            subtitle = dev.chiraitori.anis.ui.i18n.tr("domain_whitelist_subtitle", "Domains that will NEVER be blocked by any filter list"),
            domains = whitelist,
            onAddDomain = { viewModel.addWhitelistDomain(it) },
            onRemoveDomain = { viewModel.removeWhitelistDomain(it) },
            onDismiss = { showWhitelistDialog = false }
        )
    }

    // 8. Blacklist Dialog
    if (showBlacklistDialog) {
        DomainListManagerDialog(
            title = dev.chiraitori.anis.ui.i18n.tr("domain_blacklist_title", "Domain Blacklist"),
            subtitle = dev.chiraitori.anis.ui.i18n.tr("domain_blacklist_subtitle", "Custom domains that will ALWAYS be blocked immediately"),
            domains = blacklist,
            onAddDomain = { viewModel.addBlacklistDomain(it) },
            onRemoveDomain = { viewModel.removeBlacklistDomain(it) },
            onDismiss = { showBlacklistDialog = false }
        )
    }

    // 9. Custom DNS Rewrites Dialog
    if (showCustomRulesDialog) {
        CustomRulesManagerDialog(
            rules = customRules,
            onAddRule = { domain, ip -> viewModel.addCustomRule(domain, ip) },
            onRemoveRule = { viewModel.removeCustomRule(it) },
            onDismiss = { showCustomRulesDialog = false }
        )
    }

    // 10. Trusted Wi-Fi Dialog
    if (showTrustedWifiDialog) {
        TrustedWifiManagerDialog(
            trustedSsids = trustedSsids,
            isPauseEnabled = pauseOnTrusted,
            onTogglePause = { viewModel.setPauseOnTrustedEnabled(it) },
            onAddSsid = { viewModel.addTrustedSsid(it) },
            onRemoveSsid = { viewModel.removeTrustedSsid(it) },
            onDismiss = { showTrustedWifiDialog = false }
        )
    }

    // 11. Bypassed Apps Dialog
    if (showAppBypassDialog) {
        AppBypassManagerDialog(
            installedApps = firewallApps,
            whitelistedApps = whitelistedApps,
            onToggleBypass = { pkg, isBypassed ->
                if (isBypassed) viewModel.whitelistApp(pkg) else viewModel.unwhitelistApp(pkg)
            },
            onDismiss = { showAppBypassDialog = false }
        )
    }

    // 12. Backup Export Dialog
    if (showBackupDialog) {
        val backupJson = remember { viewModel.exportBackup() }
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text(dev.chiraitori.anis.ui.i18n.tr("backup_export_title", "Settings Backup (JSON)"), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = dev.chiraitori.anis.ui.i18n.tr("backup_export_help", "Copy this configuration string to save or transfer to another device:"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = backupJson,
                        onValueChange = {},
                        readOnly = true,
                        maxLines = 8,
                        shape = ShapeCache.smooth14,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Anis Backup", backupJson))
                        Toast.makeText(context, "Backup copied to clipboard!", Toast.LENGTH_SHORT).show()
                        showBackupDialog = false
                    },
                    shape = ShapeCache.smooth16
                ) {
                    Text(dev.chiraitori.anis.ui.i18n.tr("copy_to_clipboard", "Copy to Clipboard"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) { Text(dev.chiraitori.anis.ui.i18n.tr("close", "Close")) }
            }
        )
    }

    // 13. Restore Backup Dialog
    if (showRestoreDialog) {
        var restoreText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text(dev.chiraitori.anis.ui.i18n.tr("restore_config_title", "Restore Configuration"), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = dev.chiraitori.anis.ui.i18n.tr("restore_config_help", "Paste your exported JSON settings backup string below:"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = restoreText,
                        onValueChange = { restoreText = it },
                        placeholder = { Text("{\"version\":1,...}") },
                        maxLines = 8,
                        shape = ShapeCache.smooth14,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreText.isNotBlank()) {
                            val success = viewModel.importBackup(restoreText.trim())
                            if (success) {
                                Toast.makeText(context, "Configuration restored successfully!", Toast.LENGTH_SHORT).show()
                                showRestoreDialog = false
                            } else {
                                Toast.makeText(context, "Invalid backup format", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    shape = ShapeCache.smooth16
                ) {
                    Text(dev.chiraitori.anis.ui.i18n.tr("restore", "Restore"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text(dev.chiraitori.anis.ui.i18n.tr("cancel", "Cancel")) }
            }
        )
    }

    // 14. Reset Stats Confirm Dialog
    if (showResetStatsConfirm) {
        AlertDialog(
            onDismissRequest = { showResetStatsConfirm = false },
            title = { Text(dev.chiraitori.anis.ui.i18n.tr("clear_history_title", "Clear All Query History?"), fontWeight = FontWeight.Bold) },
            text = {
                Text(dev.chiraitori.anis.ui.i18n.tr("clear_history_help", "This will permanently delete all stored live query logs and reset your total blocked metrics. Filter lists and custom rules will be preserved."))
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllLogs()
                        Toast.makeText(context, "Query logs & statistics cleared", Toast.LENGTH_SHORT).show()
                        showResetStatsConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = ShapeCache.smooth16
                ) {
                    Text(dev.chiraitori.anis.ui.i18n.tr("clear_everything", "Clear Everything"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetStatsConfirm = false }) { Text(dev.chiraitori.anis.ui.i18n.tr("cancel", "Cancel")) }
            }
        )
    }

    // 15. Root or rootless CA install dialog
    if (showCaInstallConfirm) {
        AlertDialog(
            onDismissRequest = { showCaInstallConfirm = false },
            title = { Text(if (isRootAvailable) "Install System Root Certificate" else "Install HTTPS CA Certificate", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (isRootAvailable) {
                        "Anis will create a Magisk module and install its generated CA as a trusted system certificate. Reboot once after installation."
                    } else {
                        "Anis will save Anis-RootCA.crt to Downloads, then open Android Security settings. Choose Install a certificate → CA certificate and select that file."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (isRootAvailable) {
                                val success = withContext(Dispatchers.IO) {
                                    viewModel.installSystemCaCert()
                                }
                                if (success) {
                                    Toast.makeText(context, "Magisk module installed! Please reboot your device.", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Failed to install Magisk module. Check root permission.", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                val result = withContext(Dispatchers.IO) { viewModel.caManager.exportCaToDownloads() }
                                if (result.isSuccess) {
                                    Toast.makeText(context, result.getOrNull(), Toast.LENGTH_LONG).show()
                                    context.startActivity(viewModel.caManager.createInstallCertIntent())
                                } else {
                                    Toast.makeText(context, result.exceptionOrNull()?.message ?: "Failed to export certificate", Toast.LENGTH_LONG).show()
                                }
                            }
                            showCaInstallConfirm = false
                        }
                    },
                    shape = ShapeCache.smooth16
                ) {
                    Text(if (isRootAvailable) "Install Module" else "Save & Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCaInstallConfirm = false }) { Text(dev.chiraitori.anis.ui.i18n.tr("cancel", "Cancel")) }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// REUSABLE SETTINGS COMPONENTS
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun SettingsSectionHeader(
    title: String,
    icon: ImageVector,
    description: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (description != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = ShapeCache.smooth28,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        content = content
    )
}

@Composable
private fun SettingsClickRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    badge: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (icon != null) {
                Surface(
                    shape = ShapeCache.smooth14,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (badge != null) {
                Surface(
                    shape = ShapeCache.smooth10,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (icon != null) {
                Surface(
                    shape = ShapeCache.smooth14,
                    color = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// SUB-DIALOG COMPOSABLES
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun DomainListManagerDialog(
    title: String,
    subtitle: String,
    domains: Set<String>,
    onAddDomain: (String) -> Unit,
    onRemoveDomain: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newDomain by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newDomain,
                        onValueChange = { newDomain = it },
                        placeholder = { Text("example.com") },
                        singleLine = true,
                        shape = ShapeCache.smooth14,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newDomain.isNotBlank()) {
                                onAddDomain(newDomain.trim().lowercase())
                                newDomain = ""
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = dev.chiraitori.anis.ui.i18n.tr("add", "Add"))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (domains.isEmpty()) {
                    Text(dev.chiraitori.anis.ui.i18n.tr("no_domains_in_list", "No domains in this list"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(domains.toList()) { domain ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(domain, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                IconButton(onClick = { onRemoveDomain(domain) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(dev.chiraitori.anis.ui.i18n.tr("close", "Close")) }
        }
    )
}

@Composable
private fun CustomRulesManagerDialog(
    rules: List<CustomDnsRule>,
    onAddRule: (String, String) -> Unit,
    onRemoveRule: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var domain by remember { mutableStateOf("") }
    var targetIp by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dev.chiraitori.anis.ui.i18n.tr("dns_rewrites_title", "Custom DNS Rewrites"), fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(dev.chiraitori.anis.ui.i18n.tr("dns_rewrites_subtitle", "Map specific hostnames to custom IP addresses locally"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = domain,
                    onValueChange = { domain = it },
                    label = { Text(dev.chiraitori.anis.ui.i18n.tr("domain_hint", "Domain (e.g. router.local)")) },
                    singleLine = true,
                    shape = ShapeCache.smooth14,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = targetIp,
                    onValueChange = { targetIp = it },
                    label = { Text(dev.chiraitori.anis.ui.i18n.tr("target_ip_hint", "Target IP (e.g. 192.168.1.1)")) },
                    singleLine = true,
                    shape = ShapeCache.smooth14,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (domain.isNotBlank() && targetIp.isNotBlank()) {
                            onAddRule(domain.trim().lowercase(), targetIp.trim())
                            domain = ""
                            targetIp = ""
                        }
                    },
                    shape = ShapeCache.smooth14,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(dev.chiraitori.anis.ui.i18n.tr("add_rewrite_rule", "Add Rewrite Rule"))
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (rules.isEmpty()) {
                    Text(dev.chiraitori.anis.ui.i18n.tr("no_rewrites_configured", "No custom rewrites configured"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(modifier = Modifier.height(160.dp)) {
                        items(rules) { rule ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(rule.domain, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text("→ ${rule.targetIp}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { onRemoveRule(rule.id) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(dev.chiraitori.anis.ui.i18n.tr("close", "Close")) }
        }
    )
}

@Composable
private fun TrustedWifiManagerDialog(
    trustedSsids: Set<String>,
    isPauseEnabled: Boolean,
    onTogglePause: (Boolean) -> Unit,
    onAddSsid: (String) -> Unit,
    onRemoveSsid: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newSsid by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dev.chiraitori.anis.ui.i18n.tr("trusted_wifi_title", "Trusted Wi-Fi Networks"), fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(dev.chiraitori.anis.ui.i18n.tr("autopause_protection", "Auto-Pause Protection"), fontWeight = FontWeight.SemiBold)
                    Switch(checked = isPauseEnabled, onCheckedChange = onTogglePause)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(dev.chiraitori.anis.ui.i18n.tr("trusted_wifi_help", "When connected to a listed Wi-Fi network, Anis pauses filtering automatically."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newSsid,
                        onValueChange = { newSsid = it },
                        placeholder = { Text("Home_WiFi_5G") },
                        singleLine = true,
                        shape = ShapeCache.smooth14,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newSsid.isNotBlank()) {
                                onAddSsid(newSsid.trim())
                                newSsid = ""
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = dev.chiraitori.anis.ui.i18n.tr("add", "Add"))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (trustedSsids.isEmpty()) {
                    Text(dev.chiraitori.anis.ui.i18n.tr("no_trusted_wifi", "No trusted networks added"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(modifier = Modifier.height(160.dp)) {
                        items(trustedSsids.toList()) { ssid ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Wifi, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(ssid, style = MaterialTheme.typography.bodyMedium)
                                }
                                IconButton(onClick = { onRemoveSsid(ssid) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(dev.chiraitori.anis.ui.i18n.tr("close", "Close")) }
        }
    )
}

@Composable
private fun AppBypassManagerDialog(
    installedApps: List<dev.chiraitori.anis.data.model.AppFirewallItem>,
    whitelistedApps: Set<String>,
    onToggleBypass: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = installedApps.filter {
        searchQuery.isBlank() || it.appName.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dev.chiraitori.anis.ui.i18n.tr("bypassed_apps_title", "Bypassed Applications"), fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(dev.chiraitori.anis.ui.i18n.tr("bypassed_apps_help", "Selected applications will bypass DNS proxy and connect directly to the internet."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(dev.chiraitori.anis.ui.i18n.tr("search_apps_placeholder", "Search apps or packages...")) },
                    singleLine = true,
                    shape = ShapeCache.smooth14,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(modifier = Modifier.height(280.dp)) {
                    items(filteredApps) { app ->
                        val isBypassed = whitelistedApps.contains(app.packageName)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ShapeCache.smooth12)
                                .clickable { onToggleBypass(app.packageName, !isBypassed) }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(app.appName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Switch(
                                checked = isBypassed,
                                onCheckedChange = { onToggleBypass(app.packageName, it) },
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(dev.chiraitori.anis.ui.i18n.tr("done", "Done")) }
        }
    )
}
