package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val MotionStudioColorScheme = darkColorScheme(
    primary = KineticCyan,
    onPrimary = StudioObsidian,
    primaryContainer = KineticCyanContainer,
    onPrimaryContainer = KineticCyan,
    secondary = DirectorAmber,
    onSecondary = StudioObsidian,
    secondaryContainer = DirectorAmberContainer,
    onSecondaryContainer = DirectorAmber,
    tertiary = ValidatedEmerald,
    onTertiary = StudioObsidian,
    tertiaryContainer = ValidatedEmeraldContainer,
    onTertiaryContainer = ValidatedEmerald,
    error = AlertCoral,
    onError = StudioObsidian,
    errorContainer = AlertCoralContainer,
    onErrorContainer = AlertCoral,
    background = StudioObsidian,
    onBackground = TextPrimary,
    surface = StudioSurface,
    onSurface = TextPrimary,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = StudioObsidian,
    surfaceContainerLow = StudioSurface,
    surfaceContainer = StudioSurfaceElevated,
    surfaceContainerHigh = StudioSurfaceHighest,
    surfaceContainerHighest = StudioSurfaceHighest,
    outline = StudioBorder,
    outlineVariant = StudioBorder.copy(alpha = 0.5f)
)

private val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MotionStudioTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MotionStudioColorScheme,
        typography = StudioTypography,
        shapes = StudioShapes,
        content = content
    )
}
