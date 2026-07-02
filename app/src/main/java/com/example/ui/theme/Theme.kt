package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PurpleDarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFF2B2930),
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF49454F),
    primaryContainer = Color(0xFF4A4458),
    onPrimaryContainer = Color(0xFFE8DEF8)
)

private val PurpleLightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFFBF8FF),
    surface = Color(0xFFF3EDFF)
)

private val OrangeDarkColorScheme = darkColorScheme(
    primary = OrangeDarkPrimary,
    secondary = OrangeSecondary,
    tertiary = OrangeTertiary,
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFF2B2930),
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF49454F),
    primaryContainer = Color(0xFF4A4458),
    onPrimaryContainer = Color(0xFFE8DEF8)
)

private val OrangeLightColorScheme = lightColorScheme(
    primary = OrangePrimary,
    secondary = OrangeSecondary,
    tertiary = OrangeTertiary,
    background = Color(0xFFFFFDFB),
    surface = Color(0xFFFFF3E0)
)

private val GreenDarkColorScheme = darkColorScheme(
    primary = GreenDarkPrimary,
    secondary = GreenSecondary,
    tertiary = GreenTertiary,
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFF2B2930),
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF49454F),
    primaryContainer = Color(0xFF4A4458),
    onPrimaryContainer = Color(0xFFE8DEF8)
)

private val GreenLightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    secondary = GreenSecondary,
    tertiary = GreenTertiary,
    background = Color(0xFFF2FBF9),
    surface = Color(0xFFE0F2F1)
)

private val BlueDarkColorScheme = darkColorScheme(
    primary = BlueDarkPrimary,
    secondary = BlueSecondary,
    tertiary = BlueTertiary,
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFF2B2930),
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF49454F),
    primaryContainer = Color(0xFF4A4458),
    onPrimaryContainer = Color(0xFFE8DEF8)
)

private val BlueLightColorScheme = lightColorScheme(
    primary = BluePrimary,
    secondary = BlueSecondary,
    tertiary = BlueTertiary,
    background = Color(0xFFF2F8FC),
    surface = Color(0xFFE3F2FD)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = AuraThemeSettings.isDarkTheme,
    accent: ThemeAccent = AuraThemeSettings.accent,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (accent) {
        ThemeAccent.PURPLE -> if (darkTheme) PurpleDarkColorScheme else PurpleLightColorScheme
        ThemeAccent.ORANGE -> if (darkTheme) OrangeDarkColorScheme else OrangeLightColorScheme
        ThemeAccent.GREEN -> if (darkTheme) GreenDarkColorScheme else GreenLightColorScheme
        ThemeAccent.BLUE -> if (darkTheme) BlueDarkColorScheme else BlueLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
