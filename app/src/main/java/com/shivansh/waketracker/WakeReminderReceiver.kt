package com.shivansh.waketracker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class WakeReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        createNotificationChannel(context)

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = Notification.Builder(context, "WAKE_REMINDER_CHANNEL")
            .setSmallIcon(R.mipmap.alarm)
            .setContentTitle("Wake Target Approaching \uD83C\uDF05")
            .setContentText("Your target is in 30 mins. Be ready to scan your NFC tag!")
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Makes it act like a live/ongoing notification

        if (Build.VERSION.SDK_INT >= 36) {
            try {
                val progressStyle = Notification.ProgressStyle()
                builder.setStyle(progressStyle)
            } catch (e: Exception) {
                // Fallback
            }
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(101, builder.build())
    }

    private fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            "WAKE_REMINDER_CHANNEL",
            "Wake Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Live notifications for your upcoming wake targets"
        }
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}
