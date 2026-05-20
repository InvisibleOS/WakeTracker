package com.shivansh.waketracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_SHOW_LIVE_UPDATE = "com.shivansh.waketracker.SHOW_LIVE_UPDATE"
        const val ACTION_SHOW_MISSED = "com.shivansh.waketracker.SHOW_MISSED"
        const val EXTRA_TARGET_TIME_MS = "extra_target_time_ms"
        
        private const val CHANNEL_ID = "wake_tracker_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)

        val title: String
        val text: String

        // Remove the "Scan now" button. Just a simple tap to open the app.
        val contentIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        when (intent.action) {
            ACTION_SHOW_LIVE_UPDATE -> {
                title = "Upcoming Wake Scan"
                text = "Remember to scan your NFC tag by 8:00 AM."
            }
            ACTION_SHOW_MISSED -> {
                title = "Missed Scan"
                text = "You missed your 8:00 AM scan."
            }
            else -> return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(false) // Not ongoing so user can swipe it away cleanly.
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Wake Tracker Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for your wake scans"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
