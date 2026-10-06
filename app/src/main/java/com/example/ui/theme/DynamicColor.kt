package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val FallbackLight = lightColorScheme(
    primary = NaturalPrimary,
    onPrimary = NaturalOnPrimary,
    primaryContainer = NaturalPrimaryContainer,
    onPrimaryContainer = NaturalOnPrimaryContainer,
    surface = NaturalSurface,
    onSurface = NaturalOnSurface,
    background = NaturalBackground,
    onBackground = NaturalOnBackground
)

val FallbackDark = darkColorScheme(
    primary = Color(0xFF4FD1C5),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = Color(0xFFB2F5EA),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE0E0E0),
    background = Color(0xFF0E0E0E),
    onBackground = Color(0xFFE0E0E0)
)

@Composable
fun vietAiColorScheme(darkTheme: Boolean, dynamicColor: Boolean) = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val ctx = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
    }
    darkTheme -> FallbackDark
    else -> FallbackLight
}
