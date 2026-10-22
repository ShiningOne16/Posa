package com.posa.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object PosaColors {
    val Accent = Color(0xFFFFC857)      // ámbar: silueta guía
    val Match = Color(0xFF4ADE80)       // verde: pose correcta
    val Close = Color(0xFFFFB020)       // naranja: casi
    val Off = Color(0xFFFF5A5F)         // rojo: corregir
    val Scrim = Color(0x99000000)
    val Panel = Color(0xCC111014)
}

private val scheme = darkColorScheme(
    primary = PosaColors.Accent,
    onPrimary = Color(0xFF221A00),
    secondary = PosaColors.Match,
    background = Color.Black,
    surface = Color(0xFF16141A),
    onSurface = Color.White,
)

@Composable
fun PosaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
