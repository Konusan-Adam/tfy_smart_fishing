package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LuxuryDarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF0C2417),
    onPrimaryContainer = NeonGreenLight,
    secondary = MetallicSilver,
    onSecondary = Color.Black,
    background = MatteBlackBg,
    onBackground = TextWhite,
    surface = GraphiteSurface,
    onSurface = TextWhite,
    surfaceVariant = LuxuryCardBg,
    onSurfaceVariant = TextWhite,
    outline = LuxuryCardBorder,
    error = AlertRedMatte,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LuxuryDarkColorScheme,
        typography = Typography,
        content = content
    )
}
