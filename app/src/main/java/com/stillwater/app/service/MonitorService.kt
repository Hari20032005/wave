package com.stillwater.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.stillwater.app.MainActivity
import com.stillwater.app.R
import com.stillwater.app.data.MonitoredAppRepository
import com.stillwater.app.data.RiskWindowRepository
import com.stillwater.app.data.UrgeRepository
import com.stillwater.app.data.db.RiskWindowEntity
import com.stillwater.app.data.prefs.UserPreferencesRepository
import com.stillwater.app.domain.model.Mode
import com.stillwater.app.domain.model.UrgeOutcome
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * The interception engine: a specialUse foreground service polling
 * UsageStatsManager (~1.5s cadence, paused while the screen is off) for
 * foreground moves into monitored apps during risk windows. Deliberately NOT
 * an AccessibilityService — only package names are read, never content.
 */
@AndroidEntryPoint
class MonitorService : Service() {

    companion object {
        private const val CHANNEL_ID = "protection"
        private const val NOTIFICATION_ID = 3
        private const val POLL_MS = 1500L
        private const val LOOKBACK_MS = 4000L

        /** After "continue anyway", leave that app alone for a while. */
        private const val COOLDOWN_MS = 10 * 60 * 1000L

        fun start(context: Context) {
            runCatching {
                context.startForegroundService(Intent(context, MonitorService::class.java))
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MonitorService::class.java))
        }
    }

    @Inject lateinit var monitoredAppRepository: MonitoredAppRepository
    @Inject lateinit var riskWindowRepository: RiskWindowRepository
    @Inject lateinit var urgeRepository: UrgeRepository
    @Inject lateinit var preferencesRepository: UserPreferencesRepository
    @Inject lateinit var riskRepository: com.stillwater.app.data.RiskRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollJob: Job? = null
    private lateinit var overlay: InterceptOverlay
    private val cooldownUntil = mutableMapOf<String, Long>()
    private var profileMode: Mode? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> startPolling()
                Intent.ACTION_SCREEN_OFF -> {
                    pollJob?.cancel()
                    withOverlayOnMain { dismiss() }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        overlay = InterceptOverlay(this)
        registerReceiver(
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        scope.launch {
            profileMode = preferencesRepository.userPreferences.first().mode
        }
        val powerManager = getSystemService(PowerManager::class.java)
        if (powerManager?.isInteractive != false) startPolling()
        return START_STICKY
    }

    private fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = scope.launch {
            while (true) {
                runCatching { pollOnce() }.onFailure {
                    android.util.Log.e("MonitorService", "poll failed", it)
                }
                delay(POLL_MS)
            }
        }
    }

    private suspend fun pollOnce() {
        if (overlay.isShowing) return

        val foreground = latestForegroundPackage()
        if (foreground == null || foreground == packageName) return

        val now = System.currentTimeMillis()
        if ((cooldownUntil[foreground] ?: 0) > now) return

        val monitored = monitoredAppRepository.enabledPackages()
        if (foreground !in monitored) return

        val windows = riskWindowRepository.enabledWindows()
        // No windows configured = protect around the clock.
        if (windows.isNotEmpty() && !windows.any { it.containsNow() }) return

        val label = runCatching {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(foreground, 0),
            ).toString()
        }.getOrDefault("that app")
        val memoryLine = runCatching { riskRepository.doorMemory(foreground) }.getOrNull()

        withContext(Dispatchers.Main) { showIntercept(foreground, label, memoryLine) }
    }

    private fun latestForegroundPackage(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - LOOKBACK_MS, now)
        var latest: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                latest = event.packageName
            }
        }
        return latest
    }

    private fun showIntercept(pkg: String, label: String, memoryLine: String?) {
        overlay.show(
            appLabel = label,
            memoryLine = memoryLine,
            onSurf = {
                overlay.dismiss()
                startActivity(
                    Intent(this, MainActivity::class.java)
                        .setAction(MainActivity.ACTION_OPEN_SOS)
                        .putExtra(MainActivity.EXTRA_ENTRY_POINT, "INTERCEPT")
                        .putExtra(MainActivity.EXTRA_INTERCEPTED_PACKAGE, pkg)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            },
            onSkip = {
                overlay.dismiss()
                scope.launch {
                    urgeRepository.logInterceptOutcome(pkg, profileMode, UrgeOutcome.SKIPPED_APP)
                }
                // Take them somewhere neutral instead of back into the app.
                startActivity(
                    Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_HOME)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            },
            onContinue = {
                overlay.dismiss()
                // Their call — user-in-control, no second nag for a while.
                cooldownUntil[pkg] = System.currentTimeMillis() + COOLDOWN_MS
                scope.launch {
                    urgeRepository.logInterceptOutcome(pkg, profileMode, UrgeOutcome.CONTINUED)
                }
            },
        )
    }

    private fun RiskWindowEntity.containsNow(): Boolean {
        val now = ZonedDateTime.now()
        val minute = now.hour * 60 + now.minute
        val todayBit = 1 shl (now.dayOfWeek.value - 1)
        val yesterdayBit = 1 shl (now.dayOfWeek.minus(1).value - 1)
        return if (startMinuteOfDay <= endMinuteOfDay) {
            (daysOfWeekMask and todayBit != 0) && minute in startMinuteOfDay..endMinuteOfDay
        } else {
            // Wraps midnight: tonight's tail belongs to yesterday's window.
            ((daysOfWeekMask and todayBit != 0) && minute >= startMinuteOfDay) ||
                ((daysOfWeekMask and yesterdayBit != 0) && minute <= endMinuteOfDay)
        }
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Protection",
                NotificationManager.IMPORTANCE_MIN,
            ).apply {
                description = "Shown while app protection is active."
                setShowBadge(false)
            },
        )
        val pendingIntent = PendingIntent.getActivity(
            this,
            2,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Protection is on")
            .setContentText("Watching only for the apps you chose. Nothing leaves this phone.")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()
    }

    private fun withOverlayOnMain(block: InterceptOverlay.() -> Unit) {
        scope.launch(Dispatchers.Main) { overlay.block() }
    }

    override fun onDestroy() {
        pollJob?.cancel()
        runCatching { unregisterReceiver(screenReceiver) }
        withOverlayOnMain { dismiss() }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
