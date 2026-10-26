package com.example.recordatorios.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ColoresClaros = lightColorScheme(
    primary = Color(0xFF3F51B5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE1FF),
    onPrimaryContainer = Color(0xFF001257),
    secondary = Color(0xFF5B5D72),
    background = Color(0xFFFBF8FF),
    surface = Color(0xFFFBF8FF),
    surfaceVariant = Color(0xFFE3E1EC)
)

private val ColoresOscuros = darkColorScheme(
    primary = Color(0xFFB9C3FF),
    onPrimary = Color(0xFF14247A),
    primaryContainer = Color(0xFF2E3D91),
    onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = Color(0xFFC4C5DD),
    background = Color(0xFF131318),
    surface = Color(0xFF131318),
    surfaceVariant = Color(0xFF46464F)
)

@Composable
fun RecordatoriosTheme(
    oscuro: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (oscuro) ColoresOscuros else ColoresClaros,
        content = content
    )
}
