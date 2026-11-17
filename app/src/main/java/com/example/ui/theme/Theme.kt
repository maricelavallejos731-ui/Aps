package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ApexDarkColorScheme = darkColorScheme(
    primary = ApexCrimson,
    onPrimary = TextPrimary,
    primaryContainer = ApexCrimsonDark,
    onPrimaryContainer = TextPrimary,
    secondary = TelemetryCyan,
    onSecondary = ObsidianBg,
    secondaryContainer = TelemetryCardElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = SectorPurple,
    onTertiary = TextPrimary,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = CarbonSurface,
    onSurface = TextPrimary,
    surfaceVariant = TelemetryCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = CarbonSurface,
    surfaceContainer = TelemetryCard,
    surfaceContainerHigh = TelemetryCardElevated,
    outline = GridBorder,
    outlineVariant = GridBorder,
    error = OutRed,
    onError = TextPrimary
)

private val ApexShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun ApexRacingTimingTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ApexDarkColorScheme,
        typography = ApexTypography,
        shapes = ApexShapes,
        content = content
    )
}
