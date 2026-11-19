package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val FrameLoopDarkColorScheme = darkColorScheme(
    primary = OpticalAmber,
    onPrimary = ObsidianBlack,
    primaryContainer = OpticalAmberContainer,
    onPrimaryContainer = OpticalAmber,
    secondary = BadgeGifTint,
    onSecondary = ObsidianBlack,
    secondaryContainer = CardElevated,
    onSecondaryContainer = WarmWhite,
    tertiary = BadgeVideoTint,
    onTertiary = ObsidianBlack,
    background = DarkroomBackground,
    onBackground = WarmWhite,
    surface = CardSurface,
    onSurface = WarmWhite,
    surfaceVariant = CardElevated,
    onSurfaceVariant = MutedSlate,
    outline = HairlineBorder,
    outlineVariant = GlassBorder,
    error = DangerRed,
    onError = WarmWhite,
    errorContainer = DangerContainer,
    onErrorContainer = DangerRed
)

private val FrameLoopShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun FrameLoopTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FrameLoopDarkColorScheme,
        typography = AppTypography,
        shapes = FrameLoopShapes,
        content = content
    )
}
