package com.aldef.system.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AldefColorScheme = darkColorScheme(
    primary = NeonOrange,
    onPrimary = InkDeep,
    primaryContainer = Surface3,
    onPrimaryContainer = NeonAmber,
    secondary = NeonCyan,
    onSecondary = InkDeep,
    secondaryContainer = Surface3,
    onSecondaryContainer = NeonCyan,
    tertiary = NeonViolet,
    onTertiary = TextPrimary,
    background = Ink,
    onBackground = TextPrimary,
    surface = Surface1,
    onSurface = TextPrimary,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextSecondary,
    outline = Hairline,
    outlineVariant = Hairline,
    error = NeonRed,
    onError = InkDeep
)

@Composable
fun AldefSystemTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }
    MaterialTheme(colorScheme = AldefColorScheme, typography = Typography, content = content)
}
