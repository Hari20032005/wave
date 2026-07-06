package com.stillwater.app.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.stillwater.app.data.db.RiskWindowEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules exactly one pending reminder: at the start of the next enabled
 * risk window. The worker re-schedules after firing, so the chain continues
 * without a periodic job ticking pointlessly all week.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val WORK_NAME = "plan_reminder"

        /** Next window-start after [now], or null if no windows are enabled. */
        fun nextWindowStart(now: ZonedDateTime, windows: List<RiskWindowEntity>): ZonedDateTime? =
            windows.flatMap { window ->
                (0..7).mapNotNull { dayOffset ->
                    val day = now.plusDays(dayOffset.toLong())
                    val dayBit = 1 shl (day.dayOfWeek.value - 1)
                    if (window.daysOfWeekMask and dayBit == 0) return@mapNotNull null
                    day.toLocalDate()
                        .atTime(window.startMinuteOfDay / 60, window.startMinuteOfDay % 60)
                        .atZone(now.zone)
                        .takeIf { it.isAfter(now) }
                }
            }.minOrNull()
    }

    fun scheduleNext(windows: List<RiskWindowEntity>) {
        val now = ZonedDateTime.now()
        val next = nextWindowStart(now, windows) ?: return
        val request = OneTimeWorkRequestBuilder<PlanReminderWorker>()
            .setInitialDelay(Duration.between(now, next))
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
