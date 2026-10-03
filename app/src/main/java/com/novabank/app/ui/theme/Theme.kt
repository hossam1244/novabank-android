package com.novabank.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val Navy = Color(0xFF1A2B4A)
private val Gold = Color(0xFFC9A24B)

private val LightColors = lightColorScheme(
    primary = Navy,
    secondary = Gold,
    surface = Color(0xFFF7F8FA),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FB4D8),
    secondary = Gold,
)

val AppShapes = Shapes(
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun NovaBankTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = AppShapes,
        content = content,
    )
}
