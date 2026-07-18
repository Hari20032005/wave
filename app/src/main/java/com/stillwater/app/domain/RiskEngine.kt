package com.stillwater.app.domain

import com.stillwater.app.domain.model.MoodTag
import com.stillwater.app.domain.model.UrgeOutcome
import java.time.Duration
import java.time.ZonedDateTime

/**
 * The on-device pattern engine — the app's differentiator made real.
 * Plain frequency statistics over the user's own logged history: no ML
 * dependency, no network, nothing leaves the phone. Every output is
 * confidence-gated so the app stays quiet until it actually knows something
 * (no fake insight from three data points).
 */

/** One historical episode, as the engine sees it. */
data class RiskEvent(
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long?,
    val localHourOfDay: Int,
    val localDayOfWeek: Int,
    val mood: String?,
    val outcome: String,
    val entryPoint: String,
    val interceptedPackage: String?,
)

data class TideSnapshot(
    /** 0–23 hour where this user's urges cluster; null until confident. */
    val peakHour: Int?,
    /** Surfed / (surfed + lapsed), overall. Null until enough resolutions. */
    val surfRate: Double?,
    /** Of the last [RECENT_WINDOW] resolved waves, how many were surfed. */
    val recentSurfed: Int,
    val recentResolved: Int,
    /** Median minutes a surfed wave took to pass. Null until confident. */
    val typicalWaveMinutes: Long?,
    /** Most common feeling before urges. Null until confident. */
    val topMood: MoodTag?,
    val totalEvents: Int,
) {
    val hasSignal: Boolean get() = totalEvents >= RiskEngine.MIN_EVENTS
}

object RiskEngine {

    /** Below this many episodes the app admits it's still learning. */
    const val MIN_EVENTS = 5
    const val RECENT_WINDOW = 4

    fun snapshot(events: List<RiskEvent>): TideSnapshot {
        val urges = events.filter { it.outcome != UrgeOutcome.ABANDONED.name }
        val resolved = urges.filter {
            it.outcome == UrgeOutcome.SURFED.name || it.outcome == UrgeOutcome.LAPSED.name
        }
        val surfed = resolved.filter { it.outcome == UrgeOutcome.SURFED.name }
        val recent = resolved.takeLast(RECENT_WINDOW)

        return TideSnapshot(
            peakHour = peakHour(urges),
            surfRate = if (resolved.size >= 3) surfed.size.toDouble() / resolved.size else null,
            recentSurfed = recent.count { it.outcome == UrgeOutcome.SURFED.name },
            recentResolved = recent.size,
            typicalWaveMinutes = typicalWaveMinutes(surfed),
            topMood = topMood(urges),
            totalEvents = urges.size,
        )
    }

    /**
     * Peak hour with ±1h gaussian-ish smoothing so 22:50 and 23:10 urges
     * reinforce each other instead of splitting a bucket.
     */
    private fun peakHour(urges: List<RiskEvent>): Int? {
        if (urges.size < MIN_EVENTS) return null
        val weights = DoubleArray(24)
        urges.forEach { event ->
            val h = event.localHourOfDay
            weights[h] += 1.0
            weights[(h + 23) % 24] += 0.5
            weights[(h + 1) % 24] += 0.5
        }
        val peak = weights.indices.maxBy { weights[it] }
        // Demand a real cluster, not a uniform sprinkle.
        return peak.takeIf { weights[it] >= 2.5 && weights[it] > urges.size * 0.25 }
    }

    private fun typicalWaveMinutes(surfed: List<RiskEvent>): Long? {
        val durations = surfed.mapNotNull { event ->
            event.endedAtEpochMs?.let { end ->
                Duration.ofMillis(end - event.startedAtEpochMs).toMinutes()
            }
        }.filter { it in 1..120 } // discard clock noise and left-open sessions
        if (durations.size < 2) return null
        return durations.sorted()[durations.size / 2]
    }

    private fun topMood(urges: List<RiskEvent>): MoodTag? {
        val counts = urges.mapNotNull { it.mood }
            .mapNotNull { runCatching { MoodTag.valueOf(it) }.getOrNull() }
            .groupingBy { it }.eachCount()
        val (mood, count) = counts.maxByOrNull { it.value } ?: return null
        return mood.takeIf { count >= 3 }
    }

    /** Next moment the user's personal peak hour comes around. */
    fun nextHighTide(now: ZonedDateTime, peakHour: Int): ZonedDateTime {
        val todayPeak = now.toLocalDate().atTime(peakHour, 0).atZone(now.zone)
        return if (todayPeak.isAfter(now)) todayPeak else todayPeak.plusDays(1)
    }

    // ---- Copy builders: the engine speaks in the app's calm voice ----

    /** One line of personal evidence for the surf screen. Null = stay silent. */
    fun surfEvidence(snapshot: TideSnapshot): String? {
        if (!snapshot.hasSignal) return null
        val parts = mutableListOf<String>()
        snapshot.typicalWaveMinutes?.let {
            parts += "Waves usually pass in about $it minutes for you."
        }
        if (snapshot.recentResolved >= 3 && snapshot.recentSurfed > 0) {
            parts += "You've surfed ${snapshot.recentSurfed} of your last ${snapshot.recentResolved}."
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" ")
    }

    /** The intercept overlay's door memory. Null = stay silent. */
    fun doorMemory(visitsThisWeek: Int, walkedAway: Int): String? {
        if (visitsThisWeek < 2) return null
        val ordinal = when (visitsThisWeek) {
            2 -> "second"; 3 -> "third"; 4 -> "fourth"; else -> "${visitsThisWeek}th"
        }
        return if (walkedAway > 0) {
            "This is the $ordinal time at this door this week — you walked away " +
                (if (walkedAway == 1) "once." else "$walkedAway times.")
        } else {
            "This is the $ordinal time at this door this week."
        }
    }

    /** Home-screen tide line. Null = still learning (say nothing loud). */
    fun tideLine(snapshot: TideSnapshot): String? {
        val peak = snapshot.peakHour ?: return null
        return "High tide for you is usually around ${formatHour(peak)}."
    }

    private fun formatHour(hour: Int): String = when {
        hour == 0 -> "midnight"
        hour < 12 -> "$hour am"
        hour == 12 -> "noon"
        else -> "${hour - 12} pm"
    }
}
