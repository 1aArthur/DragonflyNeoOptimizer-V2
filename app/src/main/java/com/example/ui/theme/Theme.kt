package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CyberPrimary,
    secondary = CyberSecondary,
    tertiary = CyberTertiary,
    background = CyberBackground,
    surface = CyberSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = BorderColor
)

private val LightColorScheme = DarkColorScheme // Always force premium dark mode for this app style

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark theme
    dynamicColor: Boolean = false, // Disable dynamic colors to preserve premium branding identity
    content: @Composable () -> Unit,
) {
    val colorScheme = DarkColorScheme // Maintain pure black brand identity

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
