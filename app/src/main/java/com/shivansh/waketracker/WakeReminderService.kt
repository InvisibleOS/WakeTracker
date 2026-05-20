package com.shivansh.waketracker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

class WakeReminderService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // API 36: Rich Ongoing Notification using ProgressStyle
        val builder = Notification.Builder(this, "WAKE_REMINDER_CHANNEL")
            .setSmallIcon(R.mipmap.alarm)
            .setContentTitle("Wake Target Approaching \uD83C\uDF05")
            .setContentText("Your target is in 30 mins. Be ready to scan your NFC tag!")
            .setContentIntent(pendingIntent)
            .setOngoing(true)

        if (Build.VERSION.SDK_INT >= 36) {
            // Android 16 Live Notifications use ProgressStyle
            try {
                // Using reflection or direct access if compiled with API 36
                val progressStyle = Notification.ProgressStyle()
                builder.setStyle(progressStyle)
            } catch (e: Exception) {
                // Fallback if ProgressStyle is not available
            }
        }

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                101, 
                builder.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(101, builder.build())
        }
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "WAKE_REMINDER_CHANNEL",
            "Wake Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Live notifications for your upcoming wake targets"
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}
