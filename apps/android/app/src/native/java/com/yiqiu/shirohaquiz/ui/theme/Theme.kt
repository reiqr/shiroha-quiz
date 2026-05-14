package com.yiqiu.shirohaquiz.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = ShirohaColors.BrandPrimary,
    onPrimary = ShirohaColors.TextOnBrand,
    secondary = ShirohaColors.BrandSecondary,
    background = ShirohaColors.BgApp,
    onBackground = ShirohaColors.TextPrimary,
    surface = ShirohaColors.BgElevated,
    onSurface = ShirohaColors.TextPrimary,
    onSurfaceVariant = ShirohaColors.TextSecondary,
    outline = ShirohaColors.LineSoft
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF89A7FF),
    onPrimary = Color(0xFF0F172A),
    secondary = Color(0xFFB7C8FF),
    background = ShirohaColors.BgDeepFocus,
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF13203A),
    onSurface = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFFB8C2D6),
    outline = Color(0x33D7DEEA)
)

@Composable
fun ShirohaQuizTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = ShirohaTypography,
        content = content
    )
}
