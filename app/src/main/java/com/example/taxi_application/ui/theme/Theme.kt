package com.example.taxi_application.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TaxiColorScheme = lightColorScheme(
    primary = TaxiYellow,
    onPrimary = TaxiBlack,
    primaryContainer = TaxiYellowDark,
    onPrimaryContainer = TaxiBlack,
    secondary = TaxiDarkGray,
    onSecondary = TaxiWhite,
    secondaryContainer = TaxiDarkGray,
    onSecondaryContainer = TaxiWhite,
    tertiary = TaxiGreen,
    onTertiary = TaxiWhite,
    background = TaxiLightGray,
    onBackground = TaxiBlack,
    surface = TaxiWhite,
    onSurface = TaxiBlack,
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = TaxiDarkGray,
    error = TaxiRed,
    onError = TaxiWhite,
    outline = TaxiGray
)

@Composable
fun Taxi_applicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TaxiColorScheme,
        typography = Typography,
        content = content
    )
}