package dev.chiraitori.anis.ui.theme.shapes

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Official Material Design 3 Expressive Shapes System.
 * Uses 100% official Compose Material 3 CornerBasedShapes (RoundedCornerShape & CircleShape)
 * with no non-standard custom canvas shapes.
 */
object Material3ExpressiveShapes {
    val None: CornerBasedShape = RoundedCornerShape(0.dp)
    val ExtraSmall: CornerBasedShape = RoundedCornerShape(8.dp)
    val Small: CornerBasedShape = RoundedCornerShape(12.dp)
    val Medium: CornerBasedShape = RoundedCornerShape(16.dp)
    val Large: CornerBasedShape = RoundedCornerShape(20.dp)
    val ExtraLarge: CornerBasedShape = RoundedCornerShape(28.dp)
    val ExtraLargeIncreased: CornerBasedShape = RoundedCornerShape(32.dp)
    val Full: CornerBasedShape = CircleShape

    // Material 3 Expressive Asymmetrical Corner Shapes
    fun asymmetric(
        topStart: Dp = 28.dp,
        topEnd: Dp = 10.dp,
        bottomEnd: Dp = 28.dp,
        bottomStart: Dp = 10.dp
    ): CornerBasedShape = RoundedCornerShape(
        topStart = topStart,
        topEnd = topEnd,
        bottomEnd = bottomEnd,
        bottomStart = bottomStart
    )
}
