package com.freedomfighter.jeuxdujour.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = CorrectGreen,
    secondary = PresentYellow,
    tertiary = AccentWarm,
    background = Background,
    surface = Surface,
    onPrimary = Surface,
    onSecondary = TextPrimary,
    onTertiary = Surface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = Divider,
    error = AccentError,
    onError = Surface
)

@Composable
fun JeuxDuJourTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = JeuxTypography,
        content = content
    )
}
