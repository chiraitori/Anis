package dev.chiraitori.anis.ui.screens

import android.Manifest
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import dev.chiraitori.anis.data.model.DefaultDnsProviders
import dev.chiraitori.anis.data.model.ProtectionMode
import dev.chiraitori.anis.ui.MainViewModel
import dev.chiraitori.anis.ui.theme.AmberWarning
import dev.chiraitori.anis.ui.theme.CoralRed
import dev.chiraitori.anis.ui.theme.EmeraldPrimary
import dev.chiraitori.anis.ui.theme.IndigoPrimary
import dev.chiraitori.anis.ui.theme.shapes.ShapeCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val protectionMode by viewModel.protectionMode.collectAsState()
    val isRootAvailable by viewModel.isRootAvailableFlow.collectAsState()
    val motionEnabled = remember { ValueAnimator.areAnimatorsEnabled() }

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
    var isNotificationGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )
    }
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isNotificationGranted = granted
        viewModel.setAutoUpdateNotification(granted)
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            viewModel.setAutoUpdateNotification(isNotificationGranted)
        }
    }

    val canContinue = when (currentStep) {
        SetupStep.VPN_PERMISSION -> if (protectionMode == ProtectionMode.ROOT_PROXY) isRootAvailable else isVpnGranted
        else -> true
    }

    BackHandler(enabled = currentStepIndex > 0) { currentStepIndex-- }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        bottomBar = {
            SetupBottomBar(
                currentStepIndex = currentStepIndex,
                totalSteps = steps.size,
                canContinue = canContinue,
                motionEnabled = motionEnabled,
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
                modifier = Modifier.fillMaxWidth()
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
                    if (!motionEnabled) {
                        fadeIn(snap()) togetherWith fadeOut(snap())
                    } else if (targetState.ordinal > initialState.ordinal) {
                        (slideInHorizontally(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) { width -> width / 3 } + fadeIn(tween(260)) + scaleIn(initialScale = 0.96f)) togetherWith
                            (slideOutHorizontally(tween(180)) { width -> -width / 5 } + fadeOut(tween(150)) + scaleOut(targetScale = 0.985f))
                    } else {
                        (slideInHorizontally(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) { width -> -width / 3 } + fadeIn(tween(260)) + scaleIn(initialScale = 0.96f)) togetherWith
                            (slideOutHorizontally(tween(180)) { width -> width / 5 } + fadeOut(tween(150)) + scaleOut(targetScale = 0.985f))
                    }.using(SizeTransform(clip = false))
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
                    SetupStep.FINISH -> FinishStepPage()
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
        modifier = modifier.semantics {
            progressBarRangeInfo = ProgressBarRangeInfo(
                current = (currentStepIndex + 1).toFloat(),
                range = 1f..totalSteps.toFloat(),
                steps = (totalSteps - 2).coerceAtLeast(0)
            )
        },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalSteps) {
            val isActive = i <= currentStepIndex
            val isCurrent = i == currentStepIndex
            val segmentWeight by animateFloatAsState(
                targetValue = if (isCurrent) 2.5f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "setup_progress_weight"
            )

            Box(
                modifier = Modifier
                    .weight(segmentWeight)
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

        SetupIconCollage(
            primaryIcon = Icons.Filled.Shield,
            supportingIcons = listOf(Icons.Filled.Dns, Icons.Filled.Security, Icons.Filled.LocalFireDepartment, Icons.Filled.Key),
            primaryColor = MaterialTheme.colorScheme.primary,
            primaryShape = ShapeCache.star8
        )

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

        SetupIconCollage(
            primaryIcon = if (protectionMode == ProtectionMode.ROOT_PROXY) Icons.Filled.Security else if (isGranted) Icons.Filled.CheckCircle else Icons.Filled.VpnLock,
            supportingIcons = listOf(Icons.Filled.Shield, Icons.Filled.Dns, Icons.Filled.Security, Icons.Filled.Check),
            primaryColor = if (protectionMode == ProtectionMode.ROOT_PROXY || isGranted) EmeraldPrimary else MaterialTheme.colorScheme.primary,
            primaryShape = ShapeCache.star8
        )

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
    val coroutineScope = rememberCoroutineScope()
    var isCertExported by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { viewModel.caManager.getOrCreateCaCertificatePem() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        SetupIconCollage(
            primaryIcon = Icons.Filled.Key,
            supportingIcons = listOf(Icons.Filled.Security, Icons.Filled.Download, Icons.Filled.Shield, Icons.Filled.CheckCircle),
            primaryColor = IndigoPrimary,
            primaryShape = ShapeCache.star6
        )

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
                    coroutineScope.launch {
                        isExporting = true
                        val res = withContext(Dispatchers.IO) { viewModel.caManager.exportCaToDownloads() }
                        isExporting = false
                        if (res.isSuccess) {
                            isCertExported = true
                            Toast.makeText(context, "Saved 'Anis-RootCA.crt' to Downloads!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Export error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = !isExporting,
                shape = ShapeCache.smooth14,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                if (isExporting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isExporting) "Saving…" else if (isCertExported) "CA Saved" else "1. Save CA", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

        SetupIconCollage(
            primaryIcon = if (isGranted) Icons.Filled.CheckCircle else Icons.Filled.NotificationsActive,
            supportingIcons = listOf(Icons.Filled.Shield, Icons.Filled.Security, Icons.Filled.Dns, Icons.Filled.Check),
            primaryColor = if (isGranted) EmeraldPrimary else MaterialTheme.colorScheme.secondary,
            primaryShape = ShapeCache.star8
        )

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
            enabled = !isGranted,
            shape = ShapeCache.smooth14,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(if (isGranted) Icons.Filled.Check else Icons.Filled.NotificationsActive, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isGranted) "Notifications enabled" else "Enable Notifications", fontWeight = FontWeight.Bold, maxLines = 1)
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

        SetupIconCollage(
            primaryIcon = Icons.Filled.Dns,
            supportingIcons = listOf(Icons.Filled.Shield, Icons.Filled.Security, Icons.Filled.VpnLock, Icons.Filled.CheckCircle),
            primaryColor = MaterialTheme.colorScheme.primary,
            primaryShape = ShapeCache.star6
        )

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
private fun FinishStepPage(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        SetupIconCollage(
            primaryIcon = Icons.Filled.Check,
            supportingIcons = listOf(Icons.Filled.Shield, Icons.Filled.RocketLaunch, Icons.Filled.Dns, Icons.Filled.Security),
            primaryColor = EmeraldPrimary,
            primaryShape = ShapeCache.star12
        )

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

        Spacer(modifier = Modifier.height(14.dp))
    }
}

// ──────────────── Reusable Components ────────────────

@Composable
private fun SetupIconCollage(
    primaryIcon: ImageVector,
    supportingIcons: List<ImageVector>,
    primaryColor: Color,
    primaryShape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier = Modifier
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val motionEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val centerScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.78f,
        animationSpec = if (motionEnabled) {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        } else {
            snap()
        },
        label = "setup_collage_scale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(174.dp),
        contentAlignment = Alignment.Center
    ) {
        SetupSatelliteIcon(
            icon = supportingIcons.getOrElse(0) { Icons.Filled.Shield },
            color = MaterialTheme.colorScheme.secondary,
            shape = ShapeCache.star4,
            alignment = Alignment.TopStart,
            offsetX = 28.dp,
            offsetY = 12.dp,
            rotation = -13f
        )
        SetupSatelliteIcon(
            icon = supportingIcons.getOrElse(1) { Icons.Filled.Security },
            color = MaterialTheme.colorScheme.tertiary,
            shape = ShapeCache.roundedHexagon,
            alignment = Alignment.TopEnd,
            offsetX = (-24).dp,
            offsetY = 20.dp,
            rotation = 16f,
            size = 62.dp
        )
        SetupSatelliteIcon(
            icon = supportingIcons.getOrElse(2) { Icons.Filled.Dns },
            color = MaterialTheme.colorScheme.primary,
            shape = ShapeCache.star6,
            alignment = Alignment.BottomStart,
            offsetX = 46.dp,
            offsetY = (-8).dp,
            rotation = 11f,
            size = 54.dp
        )
        SetupSatelliteIcon(
            icon = supportingIcons.getOrElse(3) { Icons.Filled.CheckCircle },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = ShapeCache.star8,
            alignment = Alignment.BottomEnd,
            offsetX = (-42).dp,
            offsetY = (-2).dp,
            rotation = -10f,
            size = 50.dp
        )

        Surface(
            shape = primaryShape,
            color = primaryColor.copy(alpha = 0.18f),
            contentColor = primaryColor,
            tonalElevation = 2.dp,
            modifier = Modifier
                .align(Alignment.Center)
                .size(112.dp)
                .graphicsLayer {
                    scaleX = centerScale
                    scaleY = centerScale
                    rotationZ = -5f + (centerScale * 5f)
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = primaryIcon,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp)
                )
            }
        }
    }
}

@Composable
private fun BoxScope.SetupSatelliteIcon(
    icon: ImageVector,
    color: Color,
    shape: androidx.compose.ui.graphics.Shape,
    alignment: Alignment,
    offsetX: androidx.compose.ui.unit.Dp,
    offsetY: androidx.compose.ui.unit.Dp,
    rotation: Float,
    size: androidx.compose.ui.unit.Dp = 58.dp
) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = color,
        tonalElevation = 1.dp,
        modifier = Modifier
            .align(alignment)
            .offset(offsetX, offsetY)
            .size(size)
            .rotate(rotation)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(size * 0.43f))
        }
    }
}

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
    canContinue: Boolean,
    motionEnabled: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cornerTargets = when (currentStepIndex % 3) {
        0 -> listOf(30.dp, 30.dp, 30.dp, 30.dp)
        1 -> listOf(18.dp, 30.dp, 18.dp, 30.dp)
        else -> listOf(30.dp, 16.dp, 30.dp, 16.dp)
    }
    val cornerSpec = if (motionEnabled) tween<androidx.compose.ui.unit.Dp>(520) else snap()
    val topStart by animateDpAsState(cornerTargets[0], cornerSpec, label = "setup_cta_top_start")
    val topEnd by animateDpAsState(cornerTargets[1], cornerSpec, label = "setup_cta_top_end")
    val bottomStart by animateDpAsState(cornerTargets[2], cornerSpec, label = "setup_cta_bottom_start")
    val bottomEnd by animateDpAsState(cornerTargets[3], cornerSpec, label = "setup_cta_bottom_end")
    val rotation by animateFloatAsState(
        targetValue = currentStepIndex * 360f,
        animationSpec = if (motionEnabled) tween(700) else snap(),
        label = "setup_cta_rotation"
    )
    val barShape = androidx.compose.foundation.shape.RoundedCornerShape(
        topStart = 34.dp,
        topEnd = 34.dp
    )

    Surface(
        modifier = modifier.shadow(8.dp, barShape, clip = true),
        shape = barShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            SetupProgressBar(
                currentStepIndex = currentStepIndex,
                totalSteps = totalSteps,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (currentStepIndex > 0) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous setup step")
                    }
                }

                AnimatedContent(
                    targetState = currentStepIndex,
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        if (!motionEnabled) {
                            fadeIn(snap()) togetherWith fadeOut(snap())
                        } else if (targetState > initialState) {
                            (slideInVertically { it / 2 } + fadeIn()) togetherWith
                                (slideOutVertically { -it / 2 } + fadeOut())
                        } else {
                            (slideInVertically { -it / 2 } + fadeIn()) togetherWith
                                (slideOutVertically { it / 2 } + fadeOut())
                        }.using(SizeTransform(clip = false))
                    },
                    label = "setup_step_label"
                ) { index ->
                    Column {
                        Text(
                            text = if (index == 0) "Let's get protected" else "Step ${index + 1} of $totalSteps",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = SetupStep.values()[index].title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!canContinue) {
                            Text(
                                text = "Complete this permission first",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Button(
                    onClick = onNext,
                    enabled = canContinue,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(
                        topStart = topStart,
                        topEnd = topEnd,
                        bottomStart = bottomStart,
                        bottomEnd = bottomEnd
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
                    modifier = Modifier
                        .height(58.dp)
                        .rotate(rotation)
                ) {
                    AnimatedContent(
                        targetState = currentStepIndex == totalSteps - 1,
                        modifier = Modifier.rotate(-rotation),
                        transitionSpec = {
                            (fadeIn(tween(220, delayMillis = 80)) + scaleIn(initialScale = 0.85f)) togetherWith
                                (fadeOut(tween(90)) + scaleOut(targetScale = 0.85f))
                        },
                        label = "setup_cta_content"
                    ) { isFinished ->
                        if (isFinished) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Continue setup")
                        }
                    }
                }
            }
        }
    }
}
