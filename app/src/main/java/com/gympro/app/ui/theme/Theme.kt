package com.gympro.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------
// Design tokens — ported from the web app's CSS variables.
// ---------------------------------------------------------------------------

object GymColors {
    // Dark theme
    val Accent = Color(0xFFC8F500)
    val AccentDim = Color(0xFF8FB300)
    val Surface0 = Color(0xFF070708)
    val Surface1 = Color(0xFF0D0D10)
    val Surface2 = Color(0xFF131318)
    val Surface3 = Color(0xFF1A1A20)
    val Surface4 = Color(0xFF22222A)
    val Text = Color(0xFFEEEEF2)
    val TextMid = Color(0xFF9999A3)
    val TextSub = Color(0xFF5E5E6A)
    val Border = Color(0xFF1E1E28)
    val Border2 = Color(0xFF2A2A36)

    // Semantic
    val Success = Color(0xFF22C55E)
    val Partial = Color(0xFFF59E0B)
    val Fail = Color(0xFFEF4444)
    val Pr = Color(0xFFA855F7)
    val Water = Color(0xFF3B82F6)
    val Cardio = Color(0xFF6366F1)

    // Light theme overrides
    val LightBackground = Color(0xFFF4F4F0)
    val LightSurface = Color(0xFFFEFEFE)
    val LightAccent = Color(0xFF6A9800)
    val LightText = Color(0xFF111118)
}

private val DarkColors = darkColorScheme(
    primary = GymColors.Accent,
    onPrimary = Color.Black,
    secondary = GymColors.AccentDim,
    onSecondary = Color.Black,
    tertiary = GymColors.Pr,
    onTertiary = Color.White,
    background = GymColors.Surface0,
    onBackground = GymColors.Text,
    surface = GymColors.Surface1,
    onSurface = GymColors.Text,
    surfaceVariant = GymColors.Surface2,
    onSurfaceVariant = GymColors.TextMid,
    outline = GymColors.Border,
    outlineVariant = GymColors.Border2,
    error = GymColors.Fail,
    onError = Color.White,
    primaryContainer = Color(0xFF1A2000),
    onPrimaryContainer = GymColors.Accent,
    surfaceContainerLowest = GymColors.Surface0,
    surfaceContainerLow = GymColors.Surface1,
    surfaceContainer = GymColors.Surface2,
    surfaceContainerHigh = GymColors.Surface3,
    surfaceContainerHighest = GymColors.Surface4,
)

private val LightColors = lightColorScheme(
    primary = GymColors.LightAccent,
    onPrimary = Color.White,
    secondary = GymColors.AccentDim,
    onSecondary = Color.Black,
    tertiary = GymColors.Pr,
    onTertiary = Color.White,
    background = GymColors.LightBackground,
    onBackground = GymColors.LightText,
    surface = GymColors.LightSurface,
    onSurface = GymColors.LightText,
    surfaceVariant = Color(0xFFF0F0EC),
    onSurfaceVariant = Color(0xFF555560),
    outline = Color(0xFFD8D8D4),
    outlineVariant = Color(0xFFC4C4BE),
    error = GymColors.Fail,
    onError = Color.White,
    primaryContainer = Color(0xFFF2FFCC),
    onPrimaryContainer = Color(0xFF4A7000),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9F9F5),
    surfaceContainer = Color(0xFFF0F0EC),
    surfaceContainerHigh = Color(0xFFE8E8E2),
    surfaceContainerHighest = Color(0xFFDCDCD6),
)

private val GymShapes = Shapes(
    extraSmall = RoundedCornerShape(5.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun GymProTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        shapes = GymShapes,
        content = content,
    )
}
