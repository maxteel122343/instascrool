package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.example.data.ScrollSearchStateBus

private val DarkColorScheme = darkColorScheme(
    primary = AccentPrimary,
    onPrimary = Color.White,
    primaryContainer = IgPurple,
    onPrimaryContainer = Color.White,
    secondary = AccentSecondary,
    onSecondary = Color.White,
    secondaryContainer = DarkSurfaceElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = AccentCyan,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = DarkSurfaceBorder
)

private val LightColorScheme = lightColorScheme(
    primary = IgPink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEDF2),
    onPrimaryContainer = IgPink,
    secondary = IgPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E8FF),
    onSecondaryContainer = IgPurple,
    tertiary = IgOrange,
    onTertiary = Color.White,
    background = Color(0xFFF4F5F9),
    onBackground = Color(0xFF1A1A26),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A26),
    surfaceVariant = Color(0xFFE9EAF0),
    onSurfaceVariant = Color(0xFF4A4A60),
    outline = Color(0xFFD0D2E0)
)

@Composable
fun ScrollSearchTheme(
    darkTheme: Boolean = ScrollSearchStateBus.isDarkTheme.collectAsState().value,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
