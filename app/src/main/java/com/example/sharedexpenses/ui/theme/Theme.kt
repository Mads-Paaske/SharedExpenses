package com.example.sharedexpenses.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = InkDark,
    onPrimary = PaperDark,
    secondary = BlueDark,
    secondaryContainer = BlueContainerDark,
    onSecondaryContainer = OnBlueContainerDark,
    background = PaperDark,
    surface = PaperDark,
    surfaceContainer = SurfaceDark,
    onSurfaceVariant = GreyDark,
    outline = GreyLineDark,
    error = ErrorRedDark,
    errorContainer = ErrorContainerDark,
)

private val LightColorScheme = lightColorScheme(
    primary = Ink,
    onPrimary = White,
    secondary = Blue,
    secondaryContainer = BlueContainer,
    onSecondaryContainer = OnBlueContainer,
    background = Paper,
    surface = Paper,
    surfaceContainer = White,
    onSurfaceVariant = Grey,
    outline = GreyLine,
    error = ErrorRed,
    errorContainer = ErrorContainer,
)

@Composable
fun SharedExpensesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Lecture 8 recommendation: dynamicColor = false, so our own theme is used on every device
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
