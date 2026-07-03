package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    tertiary = DarkTertiary,
    background = DarkBg,
    surface = DarkSurface,
    onBackground = DarkOnBg,
    onSurface = DarkOnSurface,
    onPrimary = DarkBg,
    onSecondary = DarkOnBg,
    onTertiary = DarkBg
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    tertiary = LightTertiary,
    background = LightBg,
    surface = LightSurface,
    onBackground = LightOnBg,
    onSurface = LightOnSurface,
    onPrimary = LightSurface,
    onSecondary = LightSurface,
    onTertiary = LightSurface
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
