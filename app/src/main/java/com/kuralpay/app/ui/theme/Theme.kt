package com.kuralpay.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private fun schemeFor(c: KpColors) = if (c.isDark) {
    darkColorScheme(
        primary = c.royal, onPrimary = c.onRoyal,
        primaryContainer = c.royalDeep, onPrimaryContainer = c.onRoyal,
        secondary = c.lime, onSecondary = c.onLime,
        tertiary = c.good,
        background = c.ink, onBackground = c.text,
        surface = c.deck, onSurface = c.text,
        surfaceVariant = c.deck2, onSurfaceVariant = c.muted,
        surfaceContainer = c.deck, surfaceContainerHigh = c.deck2,
        outline = c.line, outlineVariant = c.line,
        error = c.warn
    )
} else {
    lightColorScheme(
        primary = c.royal, onPrimary = c.onRoyal,
        primaryContainer = c.royalDeep, onPrimaryContainer = c.onRoyal,
        secondary = c.lime, onSecondary = c.onLime,
        tertiary = c.good,
        background = c.ink, onBackground = c.text,
        surface = c.deck, onSurface = c.text,
        surfaceVariant = c.deck2, onSurfaceVariant = c.muted,
        surfaceContainer = c.deck, surfaceContainerHigh = c.deck2,
        outline = c.line, outlineVariant = c.line,
        error = c.warn
    )
}

@Composable
fun KuralPayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkKpColors else LightKpColors
    CompositionLocalProvider(LocalKpColors provides colors) {
        MaterialTheme(
            colorScheme = schemeFor(colors),
            typography = Typography,
            content = content
        )
    }
}

object Kp {
    val colors: KpColors
        @Composable @ReadOnlyComposable get() = LocalKpColors.current
}
