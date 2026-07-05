package com.stillwater.app.domain.model

/** Which compulsion(s) the user chose to work on. */
enum class Mode { SOCIAL, PORN, BOTH }

/**
 * How the app frames the work, decided by the moral-incongruence screen.
 * Research: for many users distress is values-conflict, not dysregulation —
 * those users get values/acceptance framing, never abstinence-failure framing.
 */
enum class Framing {
    /** "Catch the urge, let it pass" — habit/skill framing. */
    HABIT_CHANGE,

    /** "Live closer to your values" — acceptance framing for high moral incongruence. */
    VALUES_FIRST,
}

/** Self-reported high-risk times, from onboarding. Seeds risk windows in M3. */
enum class TriggerTime { LATE_NIGHT, MORNING, AFTER_WORK, WEEKEND, WHEN_ALONE, UNPREDICTABLE }

/** Feelings that typically precede an urge. Seeds the trigger taxonomy in M2. */
enum class MoodTag { STRESSED, BORED, LONELY, SAD, ANXIOUS, TIRED }

data class UserPreferences(
    val mode: Mode? = null,
    val onboardingComplete: Boolean = false,
    val triggerTimes: Set<TriggerTime> = emptySet(),
    val commonMoods: Set<MoodTag> = emptySet(),
    /** Mean of the moral-incongruence items, 1..5. Null if screen not shown. */
    val moralIncongruenceScore: Float? = null,
    val framing: Framing = Framing.HABIT_CHANGE,
)

/** A value the user chose to protect. Stored in Room (`user_value`). */
data class UserValue(
    val id: Long = 0,
    val name: String,
    val rank: Int,
)
