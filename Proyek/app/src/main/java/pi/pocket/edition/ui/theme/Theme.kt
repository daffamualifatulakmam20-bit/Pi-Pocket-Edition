package pi.pocket.edition.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryVariant,
    secondary = Secondary,
    onSecondary = Color.White,
    secondaryContainer = SecondaryVariant,
    background = Black_Background,
    onBackground = Dark_TextPrimary,
    surface = Black_Surface,
    onSurface = Dark_TextPrimary,
    surfaceVariant = Black_Card,
    onSurfaceVariant = Dark_TextSecondary,
    outline = Black_Border,
    outlineVariant = Black_Border,
    surfaceContainerLowest = Black_Background,
    surfaceContainerLow = Black_Surface,
    surfaceContainer = Black_Card,
    surfaceContainerHigh = Black_CardElevated,
    surfaceContainerHighest = Color(0xFF222222),
    error = StatusRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDADA),
    secondary = PrimaryVariant,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE8E8),
    background = White_Background,
    onBackground = Light_TextPrimary,
    surface = White_Surface,
    onSurface = Light_TextPrimary,
    surfaceVariant = White_Card,
    onSurfaceVariant = Light_TextSecondary,
    outline = White_Border,
    outlineVariant = White_Border,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8F8F8),
    surfaceContainer = White_Card,
    surfaceContainerHigh = White_CardElevated,
    surfaceContainerHighest = Color(0xFFEEEEEE),
    error = StatusRed,
    onError = Color.White
)

@Composable
fun PiPocketTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PiPocketTypography,
        content = content
    )
}
