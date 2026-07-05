package com.stillwater.app.domain.model

/** How the user reached the SOS flow. */
enum class EntryPoint { WIDGET, NOTIFICATION, IN_APP, INTERCEPT, RETRO_LOG }

/** How an urge episode resolved. There is no "failed" — LAPSED is data, not judgment. */
enum class UrgeOutcome { SURFED, LAPSED, CONTINUED, SKIPPED_APP, ABANDONED }

/** How far up the escalation ladder the episode went. */
enum class SosStep { BREATH, SURF, PLAN, LOG }

enum class TriggerCategory { EMOTION, TIME, PLACE, ACTIVITY, SOCIAL }

data class Trigger(
    val id: Long,
    val name: String,
    val category: TriggerCategory,
    val isCustom: Boolean = false,
)
