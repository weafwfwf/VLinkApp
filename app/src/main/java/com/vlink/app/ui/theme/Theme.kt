package com.vlink.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Purple = Color(0xFF6C5CE7)
private val PurpleDark = Color(0xFF4834D4)
private val Green = Color(0xFF00B894)
private val Bg = Color(0xFF0F1115)
private val Surface = Color(0xFF1A1D24)

private val DarkColors = darkColorScheme(
    primary = Purple,
    secondary = Green,
    background = Bg,
    surface = Surface,
)

private val LightColors = lightColorScheme(
    primary = PurpleDark,
    secondary = Green,
)

@Composable
fun VLinkTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
