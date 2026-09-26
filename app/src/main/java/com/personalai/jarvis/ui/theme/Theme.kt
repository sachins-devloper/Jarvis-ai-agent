package com.personalai.jarvis.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val JarvisColorScheme = darkColorScheme(
    primary = JarvisCyan,
    onPrimary = JarvisBackground,
    secondary = JarvisBlue,
    onSecondary = TextPrimary,
    background = JarvisBackground,
    onBackground = TextPrimary,
    surface = JarvisSurface,
    onSurface = TextPrimary,
    surfaceVariant = JarvisSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = JarvisBorder
)

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        content = content
    )
}
