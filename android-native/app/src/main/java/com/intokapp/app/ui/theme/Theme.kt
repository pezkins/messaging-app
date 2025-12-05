package com.intokapp.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple500,
    onPrimary = White,
    primaryContainer = Purple700,
    onPrimaryContainer = Purple100,
    secondary = Accent500,
    onSecondary = White,
    secondaryContainer = Accent400,
    onSecondaryContainer = White,
    tertiary = Purple300,
    onTertiary = Purple900,
    background = Surface950,
    onBackground = Surface50,
    surface = Surface900,
    onSurface = Surface50,
    surfaceVariant = Surface800,
    onSurfaceVariant = Surface300,
    error = Error,
    onError = White,
    outline = Surface600,
    outlineVariant = Surface700,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple600,
    onPrimary = White,
    primaryContainer = Purple100,
    onPrimaryContainer = Purple900,
    secondary = Accent500,
    onSecondary = White,
    secondaryContainer = Accent400,
    onSecondaryContainer = White,
    tertiary = Purple400,
    onTertiary = White,
    background = Surface50,
    onBackground = Surface900,
    surface = White,
    onSurface = Surface900,
    surfaceVariant = Surface100,
    onSurfaceVariant = Surface700,
    error = Error,
    onError = White,
    outline = Surface300,
    outlineVariant = Surface200,
)

@Composable
fun IntokTheme(
    darkTheme: Boolean = true, // Default to dark theme for Intok
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
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
        content = content
    )
}

