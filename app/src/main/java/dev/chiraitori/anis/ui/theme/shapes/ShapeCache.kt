package dev.chiraitori.anis.ui.theme.shapes

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Global Shape Cache using 100% official Material 3 Shapes (RoundedCornerShape & CircleShape).
 */
object ShapeCache {
    // Official Material 3 Expressive Primary Scale
    val extraSmall: CornerBasedShape = RoundedCornerShape(8.dp)
    val small: CornerBasedShape = RoundedCornerShape(12.dp)
    val medium: CornerBasedShape = RoundedCornerShape(16.dp)
    val large: CornerBasedShape = RoundedCornerShape(20.dp)
    val extraLarge: CornerBasedShape = RoundedCornerShape(28.dp)
    val extraLargeIncreased: CornerBasedShape = RoundedCornerShape(32.dp)
    val pill: CornerBasedShape = CircleShape
    val full: CornerBasedShape = CircleShape

    // Granular Corner Shapes
    val corner8: CornerBasedShape = RoundedCornerShape(8.dp)
    val corner10: CornerBasedShape = RoundedCornerShape(10.dp)
    val corner12: CornerBasedShape = RoundedCornerShape(12.dp)
    val corner14: CornerBasedShape = RoundedCornerShape(14.dp)
    val corner16: CornerBasedShape = RoundedCornerShape(16.dp)
    val corner18: CornerBasedShape = RoundedCornerShape(18.dp)
    val corner20: CornerBasedShape = RoundedCornerShape(20.dp)
    val corner22: CornerBasedShape = RoundedCornerShape(22.dp)
    val corner24: CornerBasedShape = RoundedCornerShape(24.dp)
    val corner26: CornerBasedShape = RoundedCornerShape(26.dp)
    val corner28: CornerBasedShape = RoundedCornerShape(28.dp)
    val corner32: CornerBasedShape = RoundedCornerShape(32.dp)
    val corner36: CornerBasedShape = RoundedCornerShape(36.dp)

    // Material 3 Expressive Asymmetrical Corner Shape
    val asymmetricCard: CornerBasedShape = Material3ExpressiveShapes.asymmetric(28.dp, 10.dp, 28.dp, 10.dp)

    // Legacy Aliases mapped directly to official M3 shapes:
    val smooth8 = corner8
    val smooth10 = corner10
    val smooth12 = corner12
    val smooth14 = corner14
    val smooth16 = corner16
    val smooth18 = corner18
    val smooth20 = corner20
    val smooth22 = corner22
    val smooth24 = corner24
    val smooth26 = corner26
    val smooth28 = corner28
    val smooth32 = corner32
    val smooth36 = corner36
    val smoothPill = pill

    val star4 = corner16
    val star5 = corner18
    val star6 = corner18
    val star8 = corner24
    val star12 = corner28

    val roundedHexagon = corner16
    val polygonHexagon = corner16
    val polygonOctagon = corner20
}
