package com.stillwater.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.stillwater.app.data.RiskWindowRepository
import com.stillwater.app.data.prefs.UserPreferencesRepository
import com.stillwater.app.notification.QuickAccessNotification
import com.stillwater.app.service.MonitorService
import com.stillwater.app.work.ReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Restores state after a reboot: protection service, the quick-access
 * notification, and the reminder chain. Note: starting a specialUse FGS from
 * BOOT_COMPLETED needs real-device verification on Android 15+ (documented
 * restriction when holding SYSTEM_ALERT_WINDOW) — hence the runCatching.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var preferencesRepository: UserPreferencesRepository
    @Inject lateinit var quickAccessNotification: QuickAccessNotification
    @Inject lateinit var riskWindowRepository: RiskWindowRepository
    @Inject lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val prefs = preferencesRepository.userPreferences.first()
                if (prefs.quickAccessEnabled) quickAccessNotification.show()
                if (prefs.interceptionEnabled) MonitorService.start(context)
                reminderScheduler.scheduleNext(riskWindowRepository.enabledWindows())
            } finally {
                pending.finish()
            }
        }
    }
}
