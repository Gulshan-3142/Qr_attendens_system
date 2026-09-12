package com.college.attendance.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Indigo500,
    onPrimary = Slate100,
    primaryContainer = Slate800,
    secondary = Emerald500,
    background = Slate950,
    surface = Slate900,
    surfaceVariant = Slate800,
    onSurface = Slate100,
    onSurfaceVariant = Slate400,
    error = Rose500
)

private val LightColorScheme = lightColorScheme(
    primary = Indigo600,
    onPrimary = Slate100,
    primaryContainer = Indigo100,
    secondary = Emerald600,
    background = Slate100,
    surface = Slate100,
    surfaceVariant = Slate300,
    onSurface = Slate900,
    onSurfaceVariant = Slate700,
    error = Rose600
)

@Composable
fun SmartQRAttendanceTheme(
    darkTheme: Boolean = true, // Default to modern sleek dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
