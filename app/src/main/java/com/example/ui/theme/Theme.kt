package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BasketOrange,
    onPrimary = Color.White,
    primaryContainer = BasketOrange.copy(alpha = 0.22f),
    onPrimaryContainer = TextWhite,
    secondary = ElectricCyan,
    onSecondary = CourtMidnight,
    secondaryContainer = ElectricCyan.copy(alpha = 0.18f),
    onSecondaryContainer = TextWhite,
    tertiary = ChampionshipGold,
    onTertiary = CourtMidnight,
    tertiaryContainer = ChampionshipGold.copy(alpha = 0.18f),
    onTertiaryContainer = TextWhite,
    background = CourtMidnight,
    surface = CourtDeepSlate,
    surfaceVariant = CourtLightSlate,
    onBackground = TextWhite,
    onSurface = TextWhite,
    onSurfaceVariant = TextGray,
    outline = CourtBorder,
    error = ErrorRed
)

private val LightColorScheme = lightColorScheme(
    primary = BasketDarkOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE5DC),
    onPrimaryContainer = Color(0xFF3B0B00),
    secondary = Color(0xFF006978),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC2F5FF),
    onSecondaryContainer = Color(0xFF001F25),
    tertiary = Color(0xFF7A5A00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDEA0),
    onTertiaryContainer = Color(0xFF271900),
    background = Color(0xFFF5F7FB),
    surface = Color.White,
    surfaceVariant = Color(0xFFE9EEF4),
    onBackground = Color(0xFF151A21),
    onSurface = Color(0xFF151A21),
    onSurfaceVariant = Color(0xFF526171),
    outline = Color(0xFF7A8796),
    error = Color(0xFFBA1A1A)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
