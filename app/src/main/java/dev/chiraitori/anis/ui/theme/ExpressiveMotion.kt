package dev.chiraitori.anis.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role

object ExpressiveMotion {
    /**
     * Bouncy spring spec for Material 3 Expressive tactile feedback.
     */
    val BouncySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    val FastBouncySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val SmoothEasing = tween<Float>(
        durationMillis = 400,
        easing = FastOutSlowInEasing
    )
}

/**
 * Material 3 Expressive tactile scale spring modifier.
 */
fun Modifier.expressiveBounceClick(
    scaleDown: Float = 0.93f,
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) scaleDown else 1f,
        animationSpec = if (isPressed) ExpressiveMotion.FastBouncySpring else ExpressiveMotion.BouncySpring,
        label = "expressive_bounce"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            enabled = enabled,
            role = role,
            onClick = onClick
        )
}
