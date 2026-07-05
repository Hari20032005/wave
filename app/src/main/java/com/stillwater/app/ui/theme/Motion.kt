package com.stillwater.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/*
 * Motion tokens — slow and regulating, never a bounce/pop hit (Design
 * Psychology #6). There are intentionally NO spring/overshoot tokens in this
 * file: overshoot is the visual grammar of the slot machine.
 */
object Motion {
    // Durations (ms). Deliberately slower than Material defaults.
    /** Small state changes: selection, toggle, fade of a hint. */
    const val GENTLE = 450
    /** Standard transitions: card expand, content swap. */
    const val CALM = 700
    /** Screen transitions inside the SOS flow — paced like a slow exhale. */
    const val DRIFT = 1200

    /** Default easing: eased both ends, no snap. */
    val CalmEase: Easing = CubicBezierEasing(0.35f, 0f, 0.25f, 1f)

    /** Sine-like easing for breathing loops — symmetric, organic. */
    val BreathEase: Easing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)
}

/**
 * Breathing cadences (ms per phase), matching real clinical pacing — the
 * animation IS the intervention, so these are design tokens, not magic
 * numbers in a screen.
 */
object BreathCadence {
    /** Box breathing: inhale – hold – exhale – hold. */
    val BOX = listOf(4000, 4000, 4000, 4000)

    /** 4-7-8: inhale – hold – long exhale (long exhale = parasympathetic). */
    val FOUR_SEVEN_EIGHT = listOf(4000, 7000, 8000)
}
