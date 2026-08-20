package dev.chiraitori.anis.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import dev.chiraitori.anis.data.model.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = MdDarkPrimary,
    onPrimary = MdDarkOnPrimary,
    primaryContainer = MdDarkPrimaryContainer,
    onPrimaryContainer = MdDarkOnPrimaryContainer,
    secondary = MdDarkSecondary,
    onSecondary = MdDarkOnSecondary,
    secondaryContainer = MdDarkSecondaryContainer,
    onSecondaryContainer = MdDarkOnSecondaryContainer,
    tertiary = MdDarkTertiary,
    onTertiary = MdDarkOnTertiary,
    tertiaryContainer = MdDarkTertiaryContainer,
    onTertiaryContainer = MdDarkOnTertiaryContainer,
    error = MdDarkError,
    onError = MdDarkOnError,
    errorContainer = MdDarkErrorContainer,
    onErrorContainer = MdDarkOnErrorContainer,
    background = MdDarkBackground,
    onBackground = MdDarkOnBackground,
    surface = MdDarkSurface,
    onSurface = MdDarkOnSurface,
    surfaceVariant = MdDarkSurfaceVariant,
    onSurfaceVariant = MdDarkOnSurfaceVariant,
    surfaceContainerLowest = MdDarkSurfaceContainerLowest,
    surfaceContainerLow = MdDarkSurfaceContainerLow,
    surfaceContainer = MdDarkSurfaceContainer,
    surfaceContainerHigh = MdDarkSurfaceContainerHigh,
    surfaceContainerHighest = MdDarkSurfaceContainerHighest
)

private val LightColorScheme = lightColorScheme(
    primary = MdLightPrimary,
    onPrimary = MdLightOnPrimary,
    primaryContainer = MdLightPrimaryContainer,
    onPrimaryContainer = MdLightOnPrimaryContainer,
    secondary = MdLightSecondary,
    onSecondary = MdLightOnSecondary,
    secondaryContainer = MdLightSecondaryContainer,
    onSecondaryContainer = MdLightOnSecondaryContainer,
    tertiary = MdLightTertiary,
    onTertiary = MdLightOnTertiary,
    tertiaryContainer = MdLightTertiaryContainer,
    onTertiaryContainer = MdLightOnTertiaryContainer,
    error = MdLightError,
    onError = MdLightOnError,
    errorContainer = MdLightErrorContainer,
    onErrorContainer = MdLightOnErrorContainer,
    background = MdLightBackground,
    onBackground = MdLightOnBackground,
    surface = MdLightSurface,
    onSurface = MdLightOnSurface,
    surfaceVariant = MdLightSurfaceVariant,
    onSurfaceVariant = MdLightOnSurfaceVariant,
    surfaceContainerLowest = MdLightSurfaceContainerLowest,
    surfaceContainerLow = MdLightSurfaceContainerLow,
    surfaceContainer = MdLightSurfaceContainer,
    surfaceContainerHigh = MdLightSurfaceContainerHigh,
    surfaceContainerHighest = MdLightSurfaceContainerHighest
)

private val AmoledColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF0D2818),
    onPrimaryContainer = EmeraldPrimary,
    secondary = MdDarkSecondary,
    onSecondary = MdDarkOnSecondary,
    secondaryContainer = Color(0xFF161616),
    onSecondaryContainer = Color.White,
    tertiary = IndigoPrimary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF14182E),
    onTertiaryContainer = IndigoPrimary,
    error = CoralRed,
    onError = Color.Black,
    errorContainer = Color(0xFF2E1010),
    onErrorContainer = CoralRed,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF141414),
    onSurfaceVariant = Color(0xFFB8B8B8),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF080808),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF1C1C1C),
    surfaceContainerHighest = Color(0xFF262626)
)

// Material 3 Expressive Shapes Scale
val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun AnisTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemInDark = isSystemInDarkTheme()

    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemInDark
        ThemeMode.DARK, ThemeMode.AMOLED -> true
        ThemeMode.LIGHT -> false
    }

    val colorScheme = when (themeMode) {
        ThemeMode.AMOLED -> AmoledColorScheme
        ThemeMode.SYSTEM -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                } catch (_: Exception) {
                    if (isDark) DarkColorScheme else LightColorScheme
                }
            } else {
                if (isDark) DarkColorScheme else LightColorScheme
            }
        }
        ThemeMode.DARK -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    dynamicDarkColorScheme(context)
                } catch (_: Exception) {
                    DarkColorScheme
                }
            } else {
                DarkColorScheme
            }
        }
        ThemeMode.LIGHT -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    dynamicLightColorScheme(context)
                } catch (_: Exception) {
                    LightColorScheme
                }
            } else {
                LightColorScheme
            }
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = Typography,
        shapes = ExpressiveShapes,
        content = content
    )
}
