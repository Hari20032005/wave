package com.stillwater.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.stillwater.app.MainActivity
import com.stillwater.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A plan reminder at the start of a high-risk window. Silent, badge-less,
 * hidden from the lock screen, and auto-dismissing — a quiet nudge to
 * rehearse, never a "come back" hook.
 */
@Singleton
class ReminderNotification @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val CHANNEL_ID = "plan_reminders"
        private const val NOTIFICATION_ID = 2
    }

    fun show(planSentence: String) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Plan reminders",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "A quiet reminder of your plan at your high-risk times."
                setShowBadge(false)
            },
        )

        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("High-tide time")
            .setContentText(planSentence)
            .setStyle(NotificationCompat.BigTextStyle().bigText(planSentence))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }
}
