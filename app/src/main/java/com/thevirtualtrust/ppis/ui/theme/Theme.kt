package com.thevirtualtrust.ppis.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6FC4FF), onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF004B78), onPrimaryContainer = Color(0xFFD1EAFF),
    secondary = Color(0xFF62D7BE), onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF005145), onSecondaryContainer = Color(0xFF8DF8DB),
    tertiary = Color(0xFF8EE6A5), background = PpisDarkBackground,
    onBackground = PpisDarkInk, surface = PpisDarkSurface, onSurface = PpisDarkInk,
    surfaceVariant = PpisDarkSurfaceVariant, onSurfaceVariant = PpisDarkMuted,
    outline = PpisDarkOutline, error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = PpisNavy, onPrimary = Color.White,
    primaryContainer = Color(0xFFD4E7FF), onPrimaryContainer = Color(0xFF001D35),
    secondary = PpisTeal, onSecondary = Color.White,
    secondaryContainer = Color(0xFFC1F1E8), onSecondaryContainer = Color(0xFF00201C),
    tertiary = Color(0xFF277A43), background = PpisBackground, onBackground = PpisInk,
    surface = PpisSurface, onSurface = PpisInk, surfaceVariant = PpisSurfaceVariant,
    onSurfaceVariant = PpisMuted, outline = PpisOutline, error = PpisError,
    onError = Color.White, errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002)
)

@Composable
fun PPISTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
