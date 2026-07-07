package com.stillwater.app

import com.google.common.truth.Truth.assertThat
import com.stillwater.app.data.db.RiskWindowEntity
import com.stillwater.app.work.ReminderScheduler
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderSchedulerTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    // Monday 2026-07-06 14:00 IST
    private val mondayAfternoon = ZonedDateTime.of(2026, 7, 6, 14, 0, 0, 0, zone)

    private val lateNight = RiskWindowEntity(
        id = 1, label = "Late night", daysOfWeekMask = 0b1111111,
        startMinuteOfDay = 22 * 60, endMinuteOfDay = 2 * 60,
    )
    private val weekends = RiskWindowEntity(
        id = 2, label = "Weekends", daysOfWeekMask = 0b1100000,
        startMinuteOfDay = 19 * 60, endMinuteOfDay = 23 * 60,
    )

    @Test
    fun `next start is tonight when the daily window is later today`() {
        val next = ReminderScheduler.nextWindowStart(mondayAfternoon, listOf(lateNight))
        assertThat(next).isEqualTo(mondayAfternoon.toLocalDate().atTime(22, 0).atZone(zone))
    }

    @Test
    fun `weekend-only window skips ahead to saturday`() {
        val next = ReminderScheduler.nextWindowStart(mondayAfternoon, listOf(weekends))
        assertThat(next!!.dayOfWeek.value).isEqualTo(6) // Saturday
        assertThat(next.hour).isEqualTo(19)
    }

    @Test
    fun `soonest window wins across several`() {
        val next = ReminderScheduler.nextWindowStart(mondayAfternoon, listOf(weekends, lateNight))
        assertThat(next!!.hour).isEqualTo(22) // tonight beats saturday
    }

    @Test
    fun `already inside a window schedules the next occurrence, not now`() {
        val elevenPm = mondayAfternoon.withHour(23)
        val next = ReminderScheduler.nextWindowStart(elevenPm, listOf(lateNight))
        assertThat(next!!.isAfter(elevenPm)).isTrue()
        assertThat(next.dayOfWeek.value).isEqualTo(2) // Tuesday 22:00
    }

    @Test
    fun `no enabled windows means nothing to schedule`() {
        assertThat(ReminderScheduler.nextWindowStart(mondayAfternoon, emptyList())).isNull()
    }
}
