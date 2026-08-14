package com.smouldering_durtles.wk.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The Material 3 ColorScheme only has slots for the standard roles. The design leans on
 * several extra warm tones (chips, tiles, muted text, dividers, success), so those live in
 * [BdExtendedColors], exposed through [LocalBdColors] and reachable as `BdTheme.colors`.
 */
private val BdColorScheme = lightColorScheme(
    primary = Ember,
    onPrimary = OnEmber,
    primaryContainer = Container,
    onPrimaryContainer = EmberInk,
    secondary = EmberInk,
    onSecondary = OnEmber,
    secondaryContainer = SurfaceChipAlt,
    onSecondaryContainer = EmberInk,
    background = Surface,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    outline = OutlineWarm,
    outlineVariant = DividerWarm,
    error = Color(0xFFB3261E),
    onError = OnEmber,
)

private val BdDarkColorScheme = darkColorScheme(
    primary = EmberDark,
    onPrimary = OnEmberDark,
    primaryContainer = ContainerDark,
    onPrimaryContainer = EmberInkDark,
    secondary = EmberInkDark,
    onSecondary = OnEmberDark,
    secondaryContainer = TonalBtnDark,
    onSecondaryContainer = EmberInkDark,
    background = SurfaceDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceCardDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = DividerDark,
    outlineVariant = DividerDark,
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
)

data class BdExtendedColors(
    val surfaceCard: Color,
    val surfaceChip: Color,
    val container: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textMuted: Color,
    val textDisabled: Color,
    val divider: Color,
    val track: Color,
    val placeholderButton: Color,
    val success: Color,
    val emberInk: Color,
    val heroStart: Color,
    val heroEnd: Color,
    // data-viz (SRS stages)
    val apprentice: Color,
    val guru: Color,
    val master: Color,
    val enlightened: Color,
    val burned: Color,
)

val LightBdColors = BdExtendedColors(
    surfaceCard = SurfaceCard,
    surfaceChip = SurfaceChip,
    container = Container,
    textPrimary = TextPrimary,
    textSecondary = TextSecondary,
    textTertiary = TextTertiary,
    textMuted = TextMuted,
    textDisabled = TextDisabled,
    divider = DividerWarm,
    track = TrackWarm,
    placeholderButton = PlaceholderBtn,
    success = Success,
    emberInk = EmberInk,
    heroStart = EmberBright,
    heroEnd = EmberDeep,
    apprentice = Apprentice,
    guru = Guru,
    master = Master,
    enlightened = Enlightened,
    burned = Burned,
)

val DarkBdColors = BdExtendedColors(
    surfaceCard = SurfaceCardDark,
    surfaceChip = SurfaceChipDark,
    container = ContainerDark,
    textPrimary = TextPrimaryDark,
    textSecondary = TextSecondaryDark,
    textTertiary = TextTertiaryDark,
    textMuted = TextMutedDark,
    textDisabled = TextDisabledDark,
    divider = DividerDark,
    track = TrackDark,
    placeholderButton = PlaceholderBtnDark,
    success = SuccessDark,
    emberInk = EmberInkDark,
    heroStart = EmberDark,
    heroEnd = EmberBrightDeepDk,
    apprentice = ApprenticeDark,
    guru = GuruDark,
    master = MasterDark,
    enlightened = EnlightenedDark,
    burned = BurnedDark,
)

val LocalBdColors = staticCompositionLocalOf { LightBdColors }

/** Rounded shapes matching the design's generous radii. */
val BdShapes = Shapes(
    extraSmall = RoundedCornerShape(11.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun BlazingDurtlesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) BdDarkColorScheme else BdColorScheme
    val extended = if (darkTheme) DarkBdColors else LightBdColors
    CompositionLocalProvider(LocalBdColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = BdTypography,
            shapes = BdShapes,
            content = content
        )
    }
}

/** Convenience accessor: `BdTheme.colors.surfaceCard`. */
object BdTheme {
    val colors: BdExtendedColors
        @Composable get() = LocalBdColors.current
}
