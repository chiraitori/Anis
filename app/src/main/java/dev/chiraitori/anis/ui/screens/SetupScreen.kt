package dev.chiraitori.anis.ui.screens

import android.app.Activity
import android.content.Context
import android.net.VpnService
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chiraitori.anis.data.model.DefaultDnsProviders
import dev.chiraitori.anis.data.model.ProtectionMode
import dev.chiraitori.anis.ui.MainViewModel
import dev.chiraitori.anis.ui.theme.AmberWarning
import dev.chiraitori.anis.ui.theme.CoralRed
import dev.chiraitori.anis.ui.theme.EmeraldPrimary
import dev.chiraitori.anis.ui.theme.IndigoPrimary
import dev.chiraitori.anis.ui.theme.shapes.ShapeCache

enum class SetupStep(val title: String) {
    WELCOME("Welcome"),
    VPN_PERMISSION("VPN Setup"),
    HTTPS_CA("HTTPS & CA"),
    NOTIFICATIONS("Alerts"),
    DNS_PROFILE("DNS Preset"),
    FINISH("All Set")
}

@Composable
fun SetupScreen(
    viewModel: MainViewModel,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentStepIndex by remember { mutableIntStateOf(0) }
    val steps = SetupStep.values()
    val currentStep = steps[currentStepIndex]

    // VPN launcher
    var isVpnGranted by remember { mutableStateOf(viewModel.isVpnPrepared()) }
    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            isVpnGranted = true
            Toast.makeText(context, "VPN Permission Granted!", Toast.LENGTH_SHORT).show()
        }
    }

    // Notification launcher (Android 13+)
    var isNotificationGranted by remember { mutableStateOf(true) }
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isNotificationGranted = granted
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            SetupProgressBar(
                currentStepIndex = currentStepIndex,
                totalSteps = steps.size,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            )
        },
        bottomBar = {
            SetupBottomBar(
                currentStepIndex = currentStepIndex,
                totalSteps = steps.size,
                onBack = { if (currentStepIndex > 0) currentStepIndex-- },
                onNext = {
                    if (currentStepIndex < steps.size - 1) {
                        currentStepIndex++
                    } else {
                        viewModel.completeOnboarding()
                        viewModel.startVpn()
                        onComplete()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }
                },
                label = "SetupStepTransition"
            ) { step ->
                when (step) {
                    SetupStep.WELCOME -> WelcomeStepPage()
                    SetupStep.VPN_PERMISSION -> VpnPermissionStepPage(
                        viewModel = viewModel,
                        isGranted = isVpnGranted,
                        onRequestPermission = {
                            val intent = VpnService.prepare(context)
                            if (intent != null) {
                                vpnLauncher.launch(intent)
                            } else {
                                isVpnGranted = true
                                Toast.makeText(context, "VPN Permission already granted!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    SetupStep.HTTPS_CA -> HttpsCaStepPage(viewModel = viewModel)
                    SetupStep.NOTIFICATIONS -> NotificationsStepPage(
                        isGranted = isNotificationGranted,
                        onRequestPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                isNotificationGranted = true
                            }
                        }
                    )
                    SetupStep.DNS_PROFILE -> DnsProfileStepPage(viewModel = viewModel)
                    SetupStep.FINISH -> FinishStepPage(
                        onStartApp = {
                            viewModel.completeOnboarding()
                            viewModel.startVpn()
                            onComplete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupProgressBar(
    currentStepIndex: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalSteps) {
            val isActive = i <= currentStepIndex
            val isCurrent = i == currentStepIndex

            Box(
                modifier = Modifier
                    .weight(if (isCurrent) 2.5f else 1f)
                    .height(5.dp)
                    .clip(ShapeCache.smooth10)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    )
            )
        }
    }
}

// ──────────────── Step 1: Welcome ────────────────

@Composable
private fun WelcomeStepPage(
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Hero Logo Shield with Material 3 Expressive Star8 Shape
        Surface(
            shape = ShapeCache.star8,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(86.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Welcome to Anis",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "System-Wide DNS & Firewall Shield with Material 3 Expressive Design",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Highlights using Material 3 Expressive shapes
        FeatureHighlightCard(
            icon = Icons.Filled.Security,
            iconColor = EmeraldPrimary,
            iconShape = ShapeCache.star6,
            title = "100% Local DNS Filtering",
            description = "Blocks intrusive ads, banners, and trackers directly on your device without remote proxy servers."
        )

        Spacer(modifier = Modifier.height(10.dp))

        FeatureHighlightCard(
            icon = Icons.Filled.LocalFireDepartment,
            iconColor = CoralRed,
            iconShape = ShapeCache.roundedHexagon,
            title = "App-Level Network Firewall",
            description = "Cut off background data access for specific applications with a single tap."
        )

        Spacer(modifier = Modifier.height(10.dp))

        FeatureHighlightCard(
            icon = Icons.Filled.Key,
            iconColor = IndigoPrimary,
            iconShape = ShapeCache.star4,
            title = "Encrypted DoH & HTTPS Inspection",
            description = "Auto-generates Root CA certificate for full tracking prevention and DNS over HTTPS security."
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ──────────────── Step 2: VPN Permission ────────────────

@Composable
private fun VpnPermissionStepPage(
    viewModel: MainViewModel,
    isGranted: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val isRootAvailable by viewModel.isRootAvailableFlow.collectAsState()
    val protectionMode by viewModel.protectionMode.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            shape = ShapeCache.star8,
            color = if (protectionMode == ProtectionMode.ROOT_PROXY || isGranted) EmeraldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(86.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (protectionMode == ProtectionMode.ROOT_PROXY) Icons.Filled.Security else if (isGranted) Icons.Filled.CheckCircle else Icons.Filled.VpnLock,
                    contentDescription = null,
                    tint = if (protectionMode == ProtectionMode.ROOT_PROXY || isGranted) EmeraldPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (protectionMode == ProtectionMode.ROOT_PROXY) "Root Mode (No VPN)" else "Local VPN Setup",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (protectionMode == ProtectionMode.ROOT_PROXY)
                "Root access allows Anis to transparently intercept DNS via iptables without occupying Android's VPN slot."
            else
                "Android requires a local VPN loopback interface to intercept and filter DNS requests on-device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Mode switch card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = ShapeCache.smooth22,
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Select Interception Method",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ProtectionMode.values().forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = protectionMode == mode,
                            onClick = { viewModel.setProtectionMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = ProtectionMode.values().size),
                            label = {
                                Text(
                                    text = if (mode == ProtectionMode.ROOT_PROXY) "Root (No VPN)" else "Local VPN",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (protectionMode == ProtectionMode.LOCAL_VPN) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeCache.smooth22,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "VPN Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = ShapeCache.smooth8,
                            color = if (isGranted) EmeraldPrimary.copy(alpha = 0.15f) else AmberWarning.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (isGranted) "READY" else "PERMISSION NEEDED",
                                color = if (isGranted) EmeraldPrimary else AmberWarning,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "No traffic is sent to remote VPN servers. Everything is processed locally in memory on your phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onRequestPermission,
                enabled = !isGranted,
                shape = ShapeCache.smooth14,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(if (isGranted) Icons.Filled.Check else Icons.Filled.VpnLock, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isGranted) "Permission Granted" else "Grant VPN Permission", fontWeight = FontWeight.Bold, maxLines = 1)
            }
        } else {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeCache.smooth22,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Root Privilege Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = ShapeCache.smooth8,
                            color = if (isRootAvailable) EmeraldPrimary.copy(alpha = 0.15f) else AmberWarning.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (isRootAvailable) "ROOT DETECTED" else "CHECKING ROOT",
                                color = if (isRootAvailable) EmeraldPrimary else AmberWarning,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Root mode redirects port 53 via iptables without creating a VPN interface. External VPNs can run simultaneously without conflicts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            FilledTonalButton(
                onClick = { viewModel.checkRootStatus() },
                shape = ShapeCache.smooth14,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Filled.Security, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Verify Root Access", fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}

// ──────────────── Step 3: HTTPS & Root CA ────────────────

@Composable
private fun HttpsCaStepPage(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var isCertExported by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.caManager.getOrCreateCaCertificatePem()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            shape = ShapeCache.star6,
            color = IndigoPrimary.copy(alpha = 0.15f),
            modifier = Modifier.size(86.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Key,
                    contentDescription = null,
                    tint = IndigoPrimary,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Root CA Certificate",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Anis auto-generates a local X.509 Root CA certificate to enable deep HTTPS adblocking.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(18.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = ShapeCache.smooth22,
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Installation Instructions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "1. Tap 'Save to Downloads' to export Anis-RootCA.crt.\n2. Tap 'Install in Settings' -> CA certificate -> select the downloaded file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                onClick = {
                    val res = viewModel.caManager.exportCaToDownloads()
                    if (res.isSuccess) {
                        isCertExported = true
                        Toast.makeText(context, "Saved 'Anis-RootCA.crt' to Downloads!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Export error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = ShapeCache.smooth14,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("1. Save CA", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Button(
                onClick = {
                    try {
                        context.startActivity(viewModel.caManager.createInstallCertIntent())
                    } catch (e: Exception) {
                        Toast.makeText(context, "Open Settings -> Security -> Install CA Cert", Toast.LENGTH_LONG).show()
                    }
                },
                shape = ShapeCache.smooth14,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("2. Install CA", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}

// ──────────────── Step 4: Notifications ────────────────

@Composable
private fun NotificationsStepPage(
    isGranted: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            shape = ShapeCache.star8,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(86.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Protection Alerts",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Required by Android so the DNS filter stays active reliably in the background without being killed.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onRequestPermission,
            shape = ShapeCache.smooth14,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Filled.NotificationsActive, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Enable Notifications", fontWeight = FontWeight.Bold, maxLines = 1)
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}

// ──────────────── Step 5: DNS Profile ────────────────

@Composable
private fun DnsProfileStepPage(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val upstreamDns by viewModel.upstreamDns.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            shape = ShapeCache.star6,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(86.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Dns,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Upstream DNS Resolver",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Select your default secure upstream resolver",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(16.dp))

        listOf(
            DefaultDnsProviders.CLOUDFLARE,
            DefaultDnsProviders.ADGUARD_DNS,
            DefaultDnsProviders.QUAD9,
            DefaultDnsProviders.GOOGLE
        ).forEach { provider ->
            val isSelected = upstreamDns.id == provider.id
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { viewModel.setUpstreamDns(provider) },
                shape = ShapeCache.smooth18,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { viewModel.setUpstreamDns(provider) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = provider.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = provider.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}

// ──────────────── Step 6: Finish ────────────────

@Composable
private fun FinishStepPage(
    onStartApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // 12-point scalloped star badge
        Surface(
            shape = ShapeCache.star12,
            color = EmeraldPrimary,
            modifier = Modifier.size(96.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(50.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "You're All Set!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Anis DNS & Firewall Shield is configured and ready to protect your device.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onStartApp,
            shape = ShapeCache.smooth18,
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(Icons.Filled.RocketLaunch, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Launch Protection", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}

// ──────────────── Reusable Components ────────────────

@Composable
private fun FeatureHighlightCard(
    icon: ImageVector,
    iconColor: Color,
    iconShape: androidx.compose.ui.graphics.Shape = ShapeCache.star6,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = ShapeCache.smooth20,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = iconShape,
                color = iconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SetupBottomBar(
    currentStepIndex: Int,
    totalSteps: Int,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (currentStepIndex > 0 && currentStepIndex < totalSteps - 1) {
            OutlinedButton(
                onClick = onBack,
                shape = ShapeCache.smooth12
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Back", fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        } else {
            Spacer(modifier = Modifier.width(1.dp))
        }

        if (currentStepIndex < totalSteps - 1) {
            Button(
                onClick = onNext,
                shape = ShapeCache.smooth12
            ) {
                Text("Continue", fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}
