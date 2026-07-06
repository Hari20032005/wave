package com.stillwater.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stillwater.app.data.PlanRepository
import com.stillwater.app.data.RiskWindowRepository
import com.stillwater.app.notification.QuickAccessNotification
import com.stillwater.app.notification.ReminderNotification
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class PlanReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val planRepository: PlanRepository,
    private val riskWindowRepository: RiskWindowRepository,
    private val reminderNotification: ReminderNotification,
    private val quickAccessNotification: QuickAccessNotification,
    private val reminderScheduler: ReminderScheduler,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val plan = planRepository.currentPlan()
        // Permission check piggybacks on the quick-access helper.
        if (plan != null && quickAccessNotification.hasPermission()) {
            reminderNotification.show(plan.sentence)
        }
        // Keep the chain alive either way.
        reminderScheduler.scheduleNext(riskWindowRepository.enabledWindows())
        return Result.success()
    }
}
