package dev.chiraitori.anis.ui

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chiraitori.anis.ui.components.AppDestination
import dev.chiraitori.anis.ui.components.ExpressiveNavBar
import dev.chiraitori.anis.ui.screens.BlockListsScreen
import dev.chiraitori.anis.ui.screens.DashboardScreen
import dev.chiraitori.anis.ui.screens.FirewallScreen
import dev.chiraitori.anis.ui.screens.QueryLogsScreen
import dev.chiraitori.anis.ui.screens.SettingsScreen
import dev.chiraitori.anis.ui.screens.SetupScreen
import dev.chiraitori.anis.data.model.ProtectionMode

@Composable
fun MainApp(
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val isOnboardingCompleted by viewModel.isOnboardingCompletedFlow.collectAsState()
    val currentLang by viewModel.appLanguage.collectAsState()

    androidx.compose.runtime.CompositionLocalProvider(
        dev.chiraitori.anis.ui.i18n.LocalAppLanguage provides currentLang
    ) {
        if (!isOnboardingCompleted) {
            SetupScreen(
                viewModel = viewModel,
                onComplete = {
                    viewModel.completeOnboarding()
                }
            )
            return@CompositionLocalProvider
        }

        var currentDestination by remember { mutableStateOf(AppDestination.DASHBOARD) }

    val stats by viewModel.stats.collectAsState()
    val firewallApps by viewModel.firewallApps.collectAsState()
    val blockedAppsCount = firewallApps.count { it.isBlocked }
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
    val protectionMode by viewModel.protectionMode.collectAsState()

    // VPN Consent Launcher
    val vpnConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.startVpn()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                val direction = if (forward) 1 else -1
                val spatialOffset = spring<IntOffset>(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
                val spatialScale = spring<Float>(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
                (fadeIn() +
                    slideInHorizontally(animationSpec = spatialOffset) { direction * it / 5 } +
                    scaleIn(initialScale = 0.97f, animationSpec = spatialScale)) togetherWith
                    (fadeOut() +
                        slideOutHorizontally(animationSpec = spatialOffset) { -direction * it / 7 } +
                        scaleOut(targetScale = 0.985f, animationSpec = spatialScale))
            },
            label = "screen_transition",
            modifier = Modifier.fillMaxSize()
        ) { destination ->
            when (destination) {
                AppDestination.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onToggleVpn = {
                        if (viewModel.isVpnRunning.value || viewModel.isStarting.value) {
                            viewModel.stopVpn()
                        } else if (protectionMode == ProtectionMode.LOCAL_VPN) {
                            val consentIntent = VpnService.prepare(context)
                            if (consentIntent != null) {
                                vpnConsentLauncher.launch(consentIntent)
                            } else {
                                viewModel.startVpn()
                            }
                        } else {
                            viewModel.startVpn()
                        }
                    },
                    onNavigate = { currentDestination = it }
                )
                AppDestination.BLOCKLISTS -> BlockListsScreen(
                    viewModel = viewModel
                )
                AppDestination.FIREWALL -> FirewallScreen(
                    viewModel = viewModel
                )
                AppDestination.LOGS -> QueryLogsScreen(
                    viewModel = viewModel
                )
                AppDestination.SETTINGS -> SettingsScreen(
                    viewModel = viewModel
                )
            }
        }

        // Soft Gradient Protection at Bottom (PixelPlayer style)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            androidx.compose.ui.graphics.Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        // Floating Expressive Navigation Bar
        ExpressiveNavBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            currentDestination = currentDestination,
            onNavigate = { currentDestination = it },
            blockedQueriesCount = stats.blockedQueries,
            blockedAppsCount = blockedAppsCount,
            hapticsEnabled = hapticsEnabled
        )
    }
}
}
