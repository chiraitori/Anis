package dev.chiraitori.anis.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chiraitori.anis.ui.theme.shapes.ShapeCache

enum class AppDestination(
    val title: String,
    val titleKey: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("Shield", "nav_shield", Icons.Filled.Security, Icons.Outlined.Security),
    BLOCKLISTS("Lists", "nav_lists", Icons.Filled.Checklist, Icons.Outlined.Checklist),
    FIREWALL("Firewall", "nav_firewall", Icons.Filled.LocalFireDepartment, Icons.Outlined.LocalFireDepartment),
    LOGS("Logs", "nav_logs", Icons.Filled.Dns, Icons.Outlined.Dns),
    SETTINGS("Settings", "nav_settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

private val PixelPlayerBarHeight = 90.dp
private val PixelPlayerIndicatorWidth = 64.dp
private val PixelPlayerIndicatorHeight = 32.dp
private val EaseInQuart = CubicBezierEasing(0.5f, 0f, 0.75f, 0f)

/** Floating bottom navigation tuned to PixelPlayer's proportions and interaction model. */
@Composable
fun ExpressiveNavBar(
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
    blockedQueriesCount: Long = 0L,
    blockedAppsCount: Int = 0,
    hapticsEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val systemBottomInset = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
        .coerceIn(0.dp, 96.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, bottom = systemBottomInset),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(PixelPlayerBarHeight),
            shape = ShapeCache.extraLargeIncreased,
            color = NavigationBarDefaults.containerColor,
            tonalElevation = 3.dp,
            shadowElevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppDestination.entries.forEach { destination ->
                    val selected = currentDestination == destination
                    PixelPlayerNavigationItem(
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                if (hapticsEnabled) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                onNavigate(destination)
                            }
                        },
                        icon = {
                            DestinationIcon(
                                destination = destination,
                                selected = selected,
                                blockedQueriesCount = blockedQueriesCount,
                                blockedAppsCount = blockedAppsCount
                            )
                        },
                        label = dev.chiraitori.anis.ui.i18n.tr(destination.titleKey, destination.title)
                    )
                }
            }
        }
    }
}

@Composable
private fun DestinationIcon(
    destination: AppDestination,
    selected: Boolean,
    blockedQueriesCount: Long,
    blockedAppsCount: Int
) {
    val count = when (destination) {
        AppDestination.LOGS -> blockedQueriesCount.coerceAtMost(99).toInt()
        AppDestination.FIREWALL -> blockedAppsCount.coerceAtMost(99)
        else -> 0
    }
    val badgeColor = if (destination == AppDestination.LOGS) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.tertiary
    }

    if (count > 0) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = badgeColor,
                    contentColor = if (destination == AppDestination.LOGS) {
                        MaterialTheme.colorScheme.onError
                    } else {
                        MaterialTheme.colorScheme.onTertiary
                    }
                ) {
                    Text(
                        text = if ((destination == AppDestination.LOGS && blockedQueriesCount > 99) ||
                            (destination == AppDestination.FIREWALL && blockedAppsCount > 99)
                        ) "99+" else count.toString(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        ) {
            DestinationIconGraphic(destination, selected)
        }
    } else {
        DestinationIconGraphic(destination, selected)
    }
}

@Composable
private fun DestinationIconGraphic(destination: AppDestination, selected: Boolean) {
    Icon(
        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
        contentDescription = null,
        modifier = Modifier.size(24.dp)
    )
}

@Composable
private fun RowScope.PixelPlayerNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    unselectedColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    indicatorColor: Color = MaterialTheme.colorScheme.secondaryContainer
) {
    val iconColor by animateColorAsState(
        targetValue = if (selected) selectedColor else unselectedColor,
        animationSpec = tween(150),
        label = "pixel_nav_icon_color"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) selectedColor else unselectedColor,
        animationSpec = tween(150),
        label = "pixel_nav_text_color"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pixel_nav_icon_scale"
    )
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                onClick = onClick,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null
            )
            .semantics(mergeDescendants = true) {
                this.contentDescription = label
                this.selected = selected
                this.role = Role.Tab
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(PixelPlayerIndicatorWidth, PixelPlayerIndicatorHeight),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = selected,
                enter = fadeIn(tween(100)) + scaleIn(
                    initialScale = 0.72f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
                exit = fadeOut(tween(100)) + scaleOut(
                    targetScale = 0.72f,
                    animationSpec = tween(100, easing = EaseInQuart)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp)
                        .background(indicatorColor, ShapeCache.pill)
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp, 24.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    },
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(LocalContentColor provides iconColor) {
                    icon()
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        ProvideTextStyle(
            MaterialTheme.typography.labelMedium.copy(
                color = textColor,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
            )
        ) {
            Text(text = label, maxLines = 1)
        }
    }
}
