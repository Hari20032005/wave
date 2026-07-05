package com.stillwater.app.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.stillwater.app.MainActivity
import com.stillwater.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A quiet, persistent shortcut in the shade — one tap to the SOS flow. LOW
 * importance: no sound, no vibration, no badge, never heads-up. This is a
 * hand rail, not engagement bait.
 */
@Singleton
class QuickAccessNotification @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val CHANNEL_ID = "quick_access"
        private const val NOTIFICATION_ID = 1
    }

    fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun show() {
        if (!hasPermission()) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Quick access",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "A quiet, always-there shortcut to the urge SOS flow."
                setShowBadge(false)
            },
        )

        val intent = Intent(context, MainActivity::class.java)
            .setAction(MainActivity.ACTION_OPEN_SOS)
            .putExtra(MainActivity.EXTRA_ENTRY_POINT, "NOTIFICATION")
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Stillwater is here")
            .setContentText("One tap when a wave rises.")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(Notification.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    fun hide() {
        context.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
    }
}
