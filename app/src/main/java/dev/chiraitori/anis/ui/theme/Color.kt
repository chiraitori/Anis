package dev.chiraitori.anis.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Expressive Brand Colors - Emerald Safe, Indigo Electric, Coral Red Alert, Amber
val EmeraldPrimary = Color(0xFF00C853)
val EmeraldLight = Color(0xFF69F0AE)
val EmeraldDark = Color(0xFF007E33)

val IndigoPrimary = Color(0xFF4C6EF5)
val IndigoSecondary = Color(0xFF3B5BDB)

val CoralRed = Color(0xFFFF5252)
val CoralRedDark = Color(0xFFD32F2F)
val AmberWarning = Color(0xFFFFB300)
val PurpleAccent = Color(0xFF9C27B0)
val CyanAccent = Color(0xFF00BCD4)
val VioletAccent = Color(0xFF7C4DFF)

val ShieldActiveGlow = Color(0x4400E676)
val ShieldInactiveGlow = Color(0x22888888)

// Dark Theme Palette with Material 3 Expressive Surface Containers
val MdDarkPrimary = Color(0xFF80CAFF)
val MdDarkOnPrimary = Color(0xFF003353)
val MdDarkPrimaryContainer = Color(0xFF004B76)
val MdDarkOnPrimaryContainer = Color(0xFFCBE6FF)

val MdDarkSecondary = Color(0xFF59DCB2)
val MdDarkOnSecondary = Color(0xFF003828)
val MdDarkSecondaryContainer = Color(0xFF00513B)
val MdDarkOnSecondaryContainer = Color(0xFF79F9CD)

val MdDarkTertiary = Color(0xFFFFB3AA)
val MdDarkOnTertiary = Color(0xFF561E17)
val MdDarkTertiaryContainer = Color(0xFF73342B)
val MdDarkOnTertiaryContainer = Color(0xFFFFDAD5)

val MdDarkError = Color(0xFFFFB4AB)
val MdDarkOnError = Color(0xFF690005)
val MdDarkErrorContainer = Color(0xFF93000A)
val MdDarkOnErrorContainer = Color(0xFFFFDAD6)

val MdDarkBackground = Color(0xFF0B0F14)
val MdDarkOnBackground = Color(0xFFE0E3EB)
val MdDarkSurface = Color(0xFF0B0F14)
val MdDarkOnSurface = Color(0xFFE0E3EB)
val MdDarkSurfaceVariant = Color(0xFF1E242E)
val MdDarkOnSurfaceVariant = Color(0xFFC2C7D0)

val MdDarkSurfaceContainerLowest = Color(0xFF060A0E)
val MdDarkSurfaceContainerLow = Color(0xFF111720)
val MdDarkSurfaceContainer = Color(0xFF161D27)
val MdDarkSurfaceContainerHigh = Color(0xFF1C2430)
val MdDarkSurfaceContainerHighest = Color(0xFF242E3C)

// Light Theme Palette with Material 3 Expressive Surface Containers
val MdLightPrimary = Color(0xFF006497)
val MdLightOnPrimary = Color(0xFFFFFFFF)
val MdLightPrimaryContainer = Color(0xFFCBE6FF)
val MdLightOnPrimaryContainer = Color(0xFF001E31)

val MdLightSecondary = Color(0xFF006C4F)
val MdLightOnSecondary = Color(0xFFFFFFFF)
val MdLightSecondaryContainer = Color(0xFF79F9CD)
val MdLightOnSecondaryContainer = Color(0xFF002116)

val MdLightTertiary = Color(0xFF904A41)
val MdLightOnTertiary = Color(0xFFFFFFFF)
val MdLightTertiaryContainer = Color(0xFFFFDAD5)
val MdLightOnTertiaryContainer = Color(0xFF3B0905)

val MdLightError = Color(0xFFBA1A1A)
val MdLightOnError = Color(0xFFFFFFFF)
val MdLightErrorContainer = Color(0xFFFFDAD6)
val MdLightOnErrorContainer = Color(0xFF410002)

val MdLightBackground = Color(0xFFF7F9FF)
val MdLightOnBackground = Color(0xFF171C22)
val MdLightSurface = Color(0xFFF7F9FF)
val MdLightOnSurface = Color(0xFF171C22)
val MdLightSurfaceVariant = Color(0xFFDFE3EB)
val MdLightOnSurfaceVariant = Color(0xFF42474E)

val MdLightSurfaceContainerLowest = Color(0xFFFFFFFF)
val MdLightSurfaceContainerLow = Color(0xFFF1F4FA)
val MdLightSurfaceContainer = Color(0xFFEBEFF5)
val MdLightSurfaceContainerHigh = Color(0xFFE5E9EF)
val MdLightSurfaceContainerHighest = Color(0xFFDFE3E9)

/**
 * PixelPlayer-inspired Gradient & Color Mixing Presets
 */
object ColorMixGradients {
    fun heroPrimaryGradient(primary: Color, tertiary: Color) = Brush.horizontalGradient(
        colors = listOf(primary, tertiary)
    )

    fun shieldRadialGradient(isActive: Boolean, primary: Color, surfaceHigh: Color) = Brush.radialGradient(
        if (isActive) listOf(EmeraldLight, EmeraldPrimary, EmeraldDark)
        else listOf(surfaceHigh, surfaceHigh.copy(alpha = 0.8f))
    )

    fun cardBorderGradient(primary: Color, isHighlighted: Boolean) = Brush.verticalGradient(
        if (isHighlighted) listOf(primary.copy(alpha = 0.6f), primary.copy(alpha = 0.1f))
        else listOf(Color.White.copy(alpha = 0.1f), Color.Transparent)
    )
}