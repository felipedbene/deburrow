package dev.debene.gopher.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// debene.dev brand, derived from the app icon:
//   periwinkle/indigo squircle #4551D2, terminal green #59AA49, lime bullets #CAD564,
//   cream window #E2DED5, black screen.
private val Indigo = Color(0xFF4551D2)
private val IndigoLight = Color(0xFFBEC2FF)
private val Green = Color(0xFF59AA49)
private val GreenLight = Color(0xFFA6D89A)
private val Lime = Color(0xFF6E6F1E)
private val LimeLight = Color(0xFFD8D89E)

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E0FF),
    onPrimaryContainer = Color(0xFF00006E),
    secondary = Color(0xFF3F7A33),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC2F0B5),
    onSecondaryContainer = Color(0xFF07210B),
    tertiary = Lime,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF4F4B8),
    onTertiaryContainer = Color(0xFF212200),
    background = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFBF8FF),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE3E1EC),
    onSurfaceVariant = Color(0xFF46464F),
)

private val DarkColors = darkColorScheme(
    primary = IndigoLight,
    onPrimary = Color(0xFF10138A),
    primaryContainer = Color(0xFF2C36BA),
    onPrimaryContainer = Color(0xFFE0E0FF),
    secondary = GreenLight,
    onSecondary = Color(0xFF123918),
    secondaryContainer = Color(0xFF285021),
    onSecondaryContainer = Color(0xFFC2F0B5),
    tertiary = LimeLight,
    onTertiary = Color(0xFF393A0B),
    tertiaryContainer = Color(0xFF515220),
    onTertiaryContainer = Color(0xFFF4F4B8),
    background = Color(0xFF121318),
    onBackground = Color(0xFFE4E1E9),
    surface = Color(0xFF121318),
    onSurface = Color(0xFFE4E1E9),
    surfaceVariant = Color(0xFF46464F),
    onSurfaceVariant = Color(0xFFC7C5D0),
)

@Composable
fun GopherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Pinned to the debene.dev brand palette on every device; Material You dynamic color
    // is intentionally off so branding is consistent. Flip to true to allow wallpaper colors.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content,
    )
}
