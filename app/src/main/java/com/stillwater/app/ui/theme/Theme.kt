package com.stillwater.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/*
 * Dynamic color is intentionally OFF: the low-arousal palette is the
 * product's therapeutic surface, and Material You could hand us a
 * high-saturation wallpaper-red scheme.
 */

private val DarkColors = darkColorScheme(
    primary = Seafoam,
    onPrimary = OnSeafoam,
    primaryContainer = SeafoamDim,
    onPrimaryContainer = Seafoam,
    secondary = SlateBlue,
    onSecondary = OnSlateBlue,
    secondaryContainer = SlateBlueDim,
    onSecondaryContainer = SlateBlue,
    tertiary = Sand,
    onTertiary = OnSand,
    tertiaryContainer = SandDim,
    onTertiaryContainer = Sand,
    background = DeepWater,
    onBackground = Mist,
    surface = DeepWater,
    onSurface = Mist,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = MistFaded,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurfaceRaised,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    error = Clay,
    onError = OnClay,
)

private val LightColors = lightColorScheme(
    primary = DeepSage,
    onPrimary = OnDeepSage,
    primaryContainer = DeepSageDim,
    onPrimaryContainer = DeepSage,
    secondary = LightSlate,
    onSecondary = OnLightSlate,
    secondaryContainer = LightSlateDim,
    onSecondaryContainer = LightSlate,
    tertiary = LightSand,
    onTertiary = OnLightSand,
    tertiaryContainer = LightSandDim,
    onTertiaryContainer = LightSand,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = LightSurface,
    onSurfaceVariant = InkFaded,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightSurfaceRaised,
    outline = LightOutline,
    outlineVariant = LightOutline,
    error = ClayLight,
    onError = OnClayLight,
)

@Composable
fun StillwaterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val toneColors = if (darkTheme) DarkToneColors else LightToneColors
    CompositionLocalProvider(LocalToneColors provides toneColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = StillwaterTypography,
            shapes = StillwaterShapes,
            content = content,
        )
    }
}

/** Accessor for the celebrate/lapse semantic tones: `Tones.current.lapseContainer`. */
object Tones {
    val current: ToneColors
        @Composable
        @ReadOnlyComposable
        get() = LocalToneColors.current
}
