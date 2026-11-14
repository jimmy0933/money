package com.moneytracker.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Emerald80,
    onPrimary = Color(0xFF00382A),
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = Color(0xFFC4F5DF),
    secondary = Color(0xFFA9CDBD),
    onSecondary = Color(0xFF14352A),
    secondaryContainer = Color(0xFF2B4A3E),
    onSecondaryContainer = Color(0xFFC4E9D8),
    tertiary = Color(0xFFE3C46A),
    onTertiary = Color(0xFF3C2F00),
    background = DarkBackground,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFB4C0BA),
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = ExpenseRedDark,
    onError = Color(0xFF4A0A0A),
    errorContainer = Color(0xFF5E1D1D),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = Emerald40,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainerLight,
    onPrimaryContainer = Color(0xFF00382A),
    secondary = Color(0xFF4C6A5C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFE9DC),
    onSecondaryContainer = Color(0xFF0A2A1E),
    tertiary = Color(0xFF8A6D00),
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightInk,
    surface = LightSurface,
    onSurface = LightInk,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF49554F),
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = ExpenseRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFE3E0),
    onErrorContainer = Color(0xFF5E1010)
)

@Composable
fun MoneyTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
