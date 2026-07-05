package com.stillwater.app.ui.sos

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Gentle, regulating haptics — short, low-amplitude marks of rhythm, never a
 * buzz of urgency. All calls are best-effort (no-ops on devices without a
 * vibrator or without amplitude control).
 */
class SosHaptics(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
            ?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    /** Marks a breath turning point (in → out). */
    fun breathTurn() = safeVibrate(VibrationEffect.createOneShot(30, 50))

    /** Soft periodic pulse during urge-surfing. */
    fun surfPulse() = safeVibrate(VibrationEffect.createOneShot(20, 35))

    /** The calm celebration: two soft, spaced touches. */
    fun bloom() = safeVibrate(
        VibrationEffect.createWaveform(
            longArrayOf(0, 40, 160, 60),
            intArrayOf(0, 45, 0, 70),
            -1,
        ),
    )

    private fun safeVibrate(effect: VibrationEffect) {
        runCatching { vibrator?.vibrate(effect) }
    }
}
