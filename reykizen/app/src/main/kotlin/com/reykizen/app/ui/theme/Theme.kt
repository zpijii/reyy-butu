package com.reykizen.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary           = OrangePrimary,
    onPrimary         = White,
    primaryContainer  = OrangeContainer,
    onPrimaryContainer = OnOrangeContainer,
    secondary         = OrangeDark,
    onSecondary       = White,
    background        = White,
    onBackground      = TextPrimary,
    surface           = OffWhite,
    onSurface         = TextPrimary,
    surfaceVariant    = Surface,
    onSurfaceVariant  = TextSecondary,
    outline           = DividerColor,
    error             = RedError,
    onError           = White
)

private val DarkColorScheme = darkColorScheme(
    primary           = OrangePrimaryDark,
    onPrimary         = BackgroundDark,
    primaryContainer  = OrangeDark,
    onPrimaryContainer = White,
    background        = BackgroundDark,
    onBackground      = White,
    surface           = SurfaceDark,
    onSurface         = White
)

@Composable
fun ReyKizenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography  = Typography(),
        content     = content
    )
}
