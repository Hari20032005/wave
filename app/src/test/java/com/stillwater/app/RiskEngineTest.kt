package com.stillwater.app

import com.google.common.truth.Truth.assertThat
import com.stillwater.app.domain.RiskEngine
import com.stillwater.app.domain.RiskEvent
import com.stillwater.app.domain.model.MoodTag
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class RiskEngineTest {

    private fun event(
        hour: Int,
        outcome: String = "SURFED",
        mood: String? = null,
        durationMin: Long? = 5,
    ) = RiskEvent(
        startedAtEpochMs = 1_000_000,
        endedAtEpochMs = durationMin?.let { 1_000_000 + it * 60_000 },
        localHourOfDay = hour,
        localDayOfWeek = 1,
        mood = mood,
        outcome = outcome,
        entryPoint = "IN_APP",
        interceptedPackage = null,
    )

    @Test
    fun `stays silent below the minimum event count`() {
        val snapshot = RiskEngine.snapshot(List(4) { event(hour = 23) })
        assertThat(snapshot.hasSignal).isFalse()
        assertThat(snapshot.peakHour).isNull()
        assertThat(RiskEngine.surfEvidence(snapshot)).isNull()
        assertThat(RiskEngine.tideLine(snapshot)).isNull()
    }

    @Test
    fun `finds the peak hour when urges cluster`() {
        val events = List(4) { event(hour = 23) } + event(hour = 9) + event(hour = 14)
        assertThat(RiskEngine.snapshot(events).peakHour).isEqualTo(23)
    }

    @Test
    fun `neighbouring hours reinforce the same peak`() {
        val events = listOf(22, 23, 23, 0, 22, 10).map { event(hour = it) }
        val peak = RiskEngine.snapshot(events).peakHour
        assertThat(peak).isAnyOf(22, 23)
    }

    @Test
    fun `no peak claimed for a uniform sprinkle`() {
        val events = listOf(1, 5, 9, 13, 17, 21).map { event(hour = it) }
        assertThat(RiskEngine.snapshot(events).peakHour).isNull()
    }

    @Test
    fun `typical wave minutes is the median of surfed durations`() {
        val events = listOf(3L, 5L, 40L).mapIndexed { i, d ->
            event(hour = 23, durationMin = d)
        } + List(2) { event(hour = 23) }
        assertThat(RiskEngine.snapshot(events).typicalWaveMinutes).isEqualTo(5)
    }

    @Test
    fun `abandoned flows never count as evidence`() {
        val events = List(6) { event(hour = 23, outcome = "ABANDONED") }
        val snapshot = RiskEngine.snapshot(events)
        assertThat(snapshot.totalEvents).isEqualTo(0)
        assertThat(snapshot.hasSignal).isFalse()
    }

    @Test
    fun `surf evidence reads like the app, not a dashboard`() {
        val events = List(5) { event(hour = 23, mood = "LONELY", durationMin = 6) }
        val line = RiskEngine.surfEvidence(RiskEngine.snapshot(events))
        assertThat(line).contains("about 6 minutes")
        assertThat(line).contains("surfed 4 of your last 4")
    }

    @Test
    fun `top mood needs at least three occurrences`() {
        val few = List(5) { event(hour = 23, mood = if (it < 2) "LONELY" else null) }
        assertThat(RiskEngine.snapshot(few).topMood).isNull()
        val many = List(5) { event(hour = 23, mood = "LONELY") }
        assertThat(RiskEngine.snapshot(many).topMood).isEqualTo(MoodTag.LONELY)
    }

    @Test
    fun `door memory speaks from the second visit and counts walkaways`() {
        assertThat(RiskEngine.doorMemory(visitsThisWeek = 1, walkedAway = 1)).isNull()
        assertThat(RiskEngine.doorMemory(visitsThisWeek = 3, walkedAway = 2))
            .isEqualTo("This is the third time at this door this week — you walked away 2 times.")
        assertThat(RiskEngine.doorMemory(visitsThisWeek = 2, walkedAway = 1))
            .contains("walked away once")
    }

    @Test
    fun `next high tide is today before the peak, tomorrow after it`() {
        val zone = ZoneId.of("Asia/Kolkata")
        val evening = ZonedDateTime.of(2026, 7, 7, 20, 0, 0, 0, zone)
        assertThat(RiskEngine.nextHighTide(evening, 23).dayOfMonth).isEqualTo(7)
        val lateNight = ZonedDateTime.of(2026, 7, 7, 23, 30, 0, 0, zone)
        assertThat(RiskEngine.nextHighTide(lateNight, 23).dayOfMonth).isEqualTo(8)
    }
}
