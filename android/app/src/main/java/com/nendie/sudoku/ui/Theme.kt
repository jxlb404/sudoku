package com.nendie.sudoku.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Indigo = Color(0xFF5B5BEF)
val IndigoDark = Color(0xFF8B8DFA)

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E9FF),
    onPrimaryContainer = Color(0xFF23264A),
    background = Color(0xFFF1F2FF),
    onBackground = Color(0xFF23264A),
    surface = Color.White,
    onSurface = Color(0xFF23264A),
    surfaceVariant = Color(0xFFEFF1FF),
    onSurfaceVariant = Color(0xFF7B80A8),
    outline = Color(0xFFB9BFE8),
    outlineVariant = Color(0xFFD9DDF3),
    error = Color(0xFFD63A5B),
    errorContainer = Color(0xFFFFE0E6),
    onErrorContainer = Color(0xFF7A1F34)
)

private val DarkColors = darkColorScheme(
    primary = IndigoDark,
    onPrimary = Color(0xFF1B1E38),
    primaryContainer = Color(0xFF33386B),
    onPrimaryContainer = Color(0xFFEDEFFB),
    background = Color(0xFF14162B),
    onBackground = Color(0xFFEDEFFB),
    surface = Color(0xFF22264A),
    onSurface = Color(0xFFEDEFFB),
    surfaceVariant = Color(0xFF2A2E55),
    onSurfaceVariant = Color(0xFF9AA0C8),
    outline = Color(0xFF4B5288),
    outlineVariant = Color(0xFF343A63),
    error = Color(0xFFFF7D99),
    errorContainer = Color(0xFF5A2A3A),
    onErrorContainer = Color(0xFFFFDCE3)
)

@Composable
fun SudokuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
