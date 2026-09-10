package com.dockeeper.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = IndigoContainer,
    onPrimaryContainer = IndigoDark,
    secondary = Teal,
    onSecondary = Color.White,
    secondaryContainer = TealContainer,
    onSecondaryContainer = TealDark,
    tertiary = AmberDark,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = Color(0xFF5A3E00),
    background = LightBackground,
    onBackground = Color(0xFF1A1B22),
    surface = Color.White,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF565A6B)
)

private val DarkColors = darkColorScheme(
    primary = IndigoLight,
    onPrimary = Color(0xFF001A6E),
    primaryContainer = IndigoDark,
    onPrimaryContainer = IndigoLight,
    secondary = TealLight,
    onSecondary = Color(0xFF00382E),
    secondaryContainer = TealDark,
    onSecondaryContainer = TealLight,
    tertiary = Amber,
    onTertiary = Color(0xFF3E2E00),
    tertiaryContainer = Color(0xFF5A4300),
    onTertiaryContainer = AmberContainer,
    background = DarkBackground,
    surface = Color(0xFF1C1D24),
    surfaceVariant = Color(0xFF44464F)
)

@Composable
fun DocKeeperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Usamos nuestra paleta de marca por defecto para un look más colorido y
    // consistente. Se puede activar Material You pasando dynamicColor = true.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DocKeeperTypography,
        content = content
    )
}
