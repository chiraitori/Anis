package dev.chiraitori.anis.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chiraitori.anis.ui.theme.EmeraldDark
import dev.chiraitori.anis.ui.theme.EmeraldLight
import dev.chiraitori.anis.ui.theme.EmeraldPrimary
import dev.chiraitori.anis.ui.theme.expressiveBounceClick
import dev.chiraitori.anis.ui.theme.shapes.ShapeCache

@Composable
fun StatusShield(
    isActive: Boolean,
    isStarting: Boolean,
    activeRulesCount: Int,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "expressive_motion")

    // Material 3 Expressive Loading Rotation
    val spinningAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "spin_angle"
    )

    val heroShape = when {
        isStarting -> ShapeCache.heroLoading
        isActive -> ShapeCache.heroActive
        else -> ShapeCache.heroInactive
    }

    val buttonScale by animateFloatAsState(
        targetValue = when {
            isStarting -> 1.06f
            isActive -> 1.02f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "button_scale"
    )

    val heroRotation by animateFloatAsState(
        targetValue = when {
            isStarting -> 18f
            isActive -> 0f
            else -> -8f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "hero_rotation"
    )

    val shieldBgGradient = when {
        isStarting -> listOf(EmeraldLight, EmeraldPrimary)
        isActive -> listOf(EmeraldLight, EmeraldPrimary, EmeraldDark)
        else -> listOf(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.surfaceContainerHighest
        )
    }

    val statusPillColor by animateColorAsState(
        targetValue = when {
            isStarting -> MaterialTheme.colorScheme.primaryContainer
            isActive -> EmeraldPrimary.copy(alpha = 0.15f)
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(400),
        label = "status_pill_bg"
    )

    val statusTextColor by animateColorAsState(
        targetValue = when {
            isStarting -> MaterialTheme.colorScheme.onPrimaryContainer
            isActive -> EmeraldPrimary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(400),
        label = "status_text_color"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(160.dp)
        ) {
            // Official Material 3 Expressive Morphing Hero Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(140.dp)
                    .graphicsLayer {
                        scaleX = buttonScale
                        scaleY = buttonScale
                        rotationZ = heroRotation
                    }
                    .clip(heroShape)
                    .background(Brush.radialGradient(shieldBgGradient))
                    .border(
                        width = 2.5.dp,
                        brush = Brush.verticalGradient(
                            if (isActive || isStarting) {
                                listOf(Color.White.copy(alpha = 0.70f), EmeraldPrimary.copy(alpha = 0.20f))
                            } else {
                                listOf(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), Color.Transparent)
                            }
                        ),
                        shape = heroShape
                    )
                    .expressiveBounceClick(scaleDown = 0.92f, onClick = onToggle)
            ) {
                AnimatedContent(
                    targetState = when {
                        isStarting -> 1
                        isActive -> 2
                        else -> 0
                    },
                    transitionSpec = {
                        (scaleIn(animationSpec = spring(Spring.DampingRatioMediumBouncy)) + fadeIn()).togetherWith(
                            scaleOut(animationSpec = spring(Spring.DampingRatioNoBouncy)) + fadeOut()
                        )
                    },
                    label = "hero_icon_state"
                ) { state ->
                    when (state) {
                        1 -> {
                            // Official Material 3 Expressive Connecting State
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 3.5.dp,
                                    modifier = Modifier.size(52.dp)
                                )
                                Icon(
                                    imageVector = Icons.Filled.Sync,
                                    contentDescription = "Connecting",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .rotate(spinningAngle)
                                )
                            }
                        }
                        2 -> {
                            // Active Protection Shield
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = "Active Protection",
                                tint = Color.White,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                        else -> {
                            // Paused Power Icon
                            Icon(
                                imageVector = Icons.Filled.PowerSettingsNew,
                                contentDescription = "Protection Paused",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(54.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Official Material 3 Status Pill
        Surface(
            shape = ShapeCache.pill,
            color = statusPillColor,
            border = if (isActive) androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f)) else null,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            val startingStr = dev.chiraitori.anis.ui.i18n.tr("shield_starting", "STARTING PROTECTION...")
            val activeStr = dev.chiraitori.anis.ui.i18n.tr("shield_active", "SYSTEM-WIDE PROTECTION ACTIVE")
            val pausedStr = dev.chiraitori.anis.ui.i18n.tr("shield_paused", "PROTECTION PAUSED")

            AnimatedContent(
                targetState = when {
                    isStarting -> startingStr
                    isActive -> activeStr
                    else -> pausedStr
                },
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "pill_text"
            ) { statusText ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = when {
                            isStarting -> Icons.Filled.Sync
                            isActive -> Icons.Filled.CheckCircle
                            else -> Icons.Filled.PauseCircle
                        },
                        contentDescription = null,
                        tint = statusTextColor,
                        modifier = Modifier
                            .size(16.dp)
                            .then(if (isStarting) Modifier.rotate(spinningAngle) else Modifier)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusText,
                        color = statusTextColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.6.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val protectingStr = dev.chiraitori.anis.ui.i18n.tr("rules_protecting", "active rules protecting device")
        val tapToStartStr = dev.chiraitori.anis.ui.i18n.tr("tap_to_start", "Tap shield to activate local DNS adblocker")

        Text(
            text = if (isActive) "$activeRulesCount $protectingStr" else tapToStartStr,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
