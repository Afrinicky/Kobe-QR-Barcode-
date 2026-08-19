package com.kobe.qrbarcode.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.kobe.qrbarcode.data.prefs.ThemeMode

private val LightColors = lightColorScheme(
    primary = KobeIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4E0FF),
    onPrimaryContainer = Color(0xFF190F6B),
    secondary = KobeTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8F5EC),
    onSecondaryContainer = Color(0xFF00382F),
    tertiary = KobeAmber,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE2C2),
    onTertiaryContainer = Color(0xFF3B1E00),
    background = KobeCloud,
    onBackground = KobeInk,
    surface = Color.White,
    onSurface = KobeInk,
    surfaceVariant = KobeMist,
    onSurfaceVariant = Color(0xFF49465B),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFCFBFF),
    surfaceContainer = Color(0xFFF3F1FB),
    surfaceContainerHigh = Color(0xFFEDEBF7),
    surfaceContainerHighest = Color(0xFFE7E4F3),
    outline = Color(0xFF7B7790),
    outlineVariant = Color(0xFFCBC7DC),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val DarkColors = darkColorScheme(
    primary = KobeIndigoLight,
    onPrimary = Color(0xFF23148F),
    primaryContainer = KobeIndigoDark,
    onPrimaryContainer = Color(0xFFE4E0FF),
    secondary = KobeTealLight,
    onSecondary = Color(0xFF003730),
    secondaryContainer = Color(0xFF005246),
    onSecondaryContainer = Color(0xFF8FF8E2),
    tertiary = Color(0xFFFFB870),
    onTertiary = Color(0xFF4A2800),
    tertiaryContainer = Color(0xFF693C00),
    onTertiaryContainer = Color(0xFFFFDCBE),
    background = Color(0xFF0F0D1B),
    onBackground = Color(0xFFE7E3F3),
    surface = Color(0xFF0F0D1B),
    onSurface = Color(0xFFE7E3F3),
    surfaceVariant = Color(0xFF2A2739),
    onSurfaceVariant = Color(0xFFC9C4DA),
    surfaceContainerLowest = Color(0xFF0A0812),
    surfaceContainerLow = Color(0xFF161423),
    surfaceContainer = KobeInkSoft,
    surfaceContainerHigh = Color(0xFF262336),
    surfaceContainerHighest = Color(0xFF302C41),
    outline = Color(0xFF938EA6),
    outlineVariant = Color(0xFF48445A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun KobeTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = KobeTypography,
        content = content
    )
}
