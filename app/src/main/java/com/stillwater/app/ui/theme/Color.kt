package com.stillwater.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/*
 * Stillwater palette — low-arousal by design (Design Psychology #1).
 *
 * Every hue is desaturated on purpose: saturation is arousal, and this app's
 * job is to lower arousal. 60% deep green-tinted neutrals, 30% muted
 * supporting tones, 10% seafoam accent. The ONLY warm color is Sand, reserved
 * for a single gentle focal point per screen — never for alarm.
 *
 * There is deliberately no bright red anywhere. Lapse styling is warm sand
 * (Design Psychology #3); the Material error slot gets a muted clay reserved
 * for genuine system errors (e.g. "export failed"), never for user behavior.
 */

// ---- Dark (primary theme — late-night is the peak-risk context) ----
val DeepWater = Color(0xFF0F1714)        // background: near-black spruce, low blue light
val DarkSurface = Color(0xFF161F1B)      // cards, sheets
val DarkSurfaceRaised = Color(0xFF1D2823) // elevated cards, dialogs
val Mist = Color(0xFFE3E8E4)             // primary text: soft off-white, no glare
val MistFaded = Color(0xFF9DACA2)        // secondary text (~70% emphasis)
val DarkOutline = Color(0xFF39463F)      // hairlines, dividers

val Seafoam = Color(0xFF8FB8A5)          // primary accent (the 10%)
val OnSeafoam = Color(0xFF10241B)
val SeafoamDim = Color(0xFF2A3B33)       // primary container
val SlateBlue = Color(0xFF90A7B6)        // secondary: muted blue for quiet chrome
val OnSlateBlue = Color(0xFF15222B)
val SlateBlueDim = Color(0xFF27333C)
val Sand = Color(0xFFD5C2A1)             // tertiary: the ONE warm accent
val OnSand = Color(0xFF2E271A)
val SandDim = Color(0xFF37311F)

// ---- Light (secondary theme: warm paper, not clinical white) ----
val Paper = Color(0xFFF4F1EA)
val LightSurface = Color(0xFFFBF9F4)
val LightSurfaceRaised = Color(0xFFFFFFFF)
val Ink = Color(0xFF252D28)
val InkFaded = Color(0xFF5B685F)
val LightOutline = Color(0xFFCBD2CA)

val DeepSage = Color(0xFF48705F)
val OnDeepSage = Color(0xFFF4F1EA)
val DeepSageDim = Color(0xFFD9E6DE)
val LightSlate = Color(0xFF5A7183)
val OnLightSlate = Color(0xFFF4F1EA)
val LightSlateDim = Color(0xFFDCE4E9)
val LightSand = Color(0xFF7E6C4F)
val OnLightSand = Color(0xFFF4F1EA)
val LightSandDim = Color(0xFFEDE4D1)

// ---- Muted clay: system errors ONLY, never user behavior ----
val Clay = Color(0xFFC29488)
val OnClay = Color(0xFF33201B)
val ClayLight = Color(0xFF8C5F53)
val OnClayLight = Color(0xFFF4F1EA)

/**
 * Semantic tones Material3 has no slot for. Locked in at M0 so red/confetti
 * styling can never leak in later (Design Psychology #2, #3, #8):
 *
 * - [celebrate*]: "urge surfed" — a soft green bloom. Quiet, predictable relief.
 * - [lapse*]: lapse debrief — warm sand. Matter-of-fact and kind, never an
 *   error state. If a screen ever styles a lapse with error colors, that is
 *   a bug against the product spec.
 */
@Immutable
data class ToneColors(
    val celebrateContainer: Color,
    val onCelebrateContainer: Color,
    val lapseContainer: Color,
    val onLapseContainer: Color,
)

val DarkToneColors = ToneColors(
    celebrateContainer = Color(0xFF22322B),
    onCelebrateContainer = Color(0xFFA9CDBA),
    lapseContainer = Color(0xFF322D22),
    onLapseContainer = Color(0xFFDFD2B6),
)

val LightToneColors = ToneColors(
    celebrateContainer = Color(0xFFDEEAE2),
    onCelebrateContainer = Color(0xFF33523F),
    lapseContainer = Color(0xFFEFE7D3),
    onLapseContainer = Color(0xFF5C5136),
)

val LocalToneColors = staticCompositionLocalOf { DarkToneColors }
