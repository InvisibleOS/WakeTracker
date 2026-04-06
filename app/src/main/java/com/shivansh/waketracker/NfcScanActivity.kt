package com.shivansh.waketracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.app.NotificationCompat
import com.shivansh.waketracker.data.WakeDatabase
import com.shivansh.waketracker.data.WakeLog
import com.shivansh.waketracker.data.WakeStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.util.Date
import java.util.Locale

class NfcScanActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Modern way to kill enter animations (Handles Android 14+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        super.onCreate(savedInstanceState)

        val sharedPrefs = getSharedPreferences("WakeTrackerPrefs", Context.MODE_PRIVATE)
        val intentData = intent?.data

        if (intentData?.scheme == "waketracker" && intentData.host == "scan") {
            CoroutineScope(Dispatchers.IO).launch {
                processNfcScan(sharedPrefs)

                withContext(Dispatchers.Main) {
                    finish()
                    closeTransition()
                }
            }
        } else {
            finish()
            closeTransition()
        }
    }

    // 2. Helper to kill exit animations cleanly
    private fun closeTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    private suspend fun processNfcScan(sharedPrefs: SharedPreferences) {
        val today = LocalDate.now()
        val scanTimeMs = System.currentTimeMillis()
        val targetHour = sharedPrefs.getInt("target_hour", 8)
        val targetMinute = sharedPrefs.getInt("target_minute", 0)

        val now = LocalTime.now()
        val targetTime = LocalTime.of(targetHour, targetMinute)

        val status = if (now.isBefore(targetTime) || now == targetTime) WakeStatus.ON_TIME else WakeStatus.LATE
        val dao = WakeDatabase.getDatabase(this).wakeDao()

        val existingLog = dao.getLogByDate(today.toString())
        if (existingLog == null) {
            dao.insertLog(WakeLog(dateStr = today.toString(), targetTimeMs = 0L, actualScanTimeMs = scanTimeMs, status = status))
        }

        val allLogs = dao.getAllLogsSnapshot()
        val currentMonth = YearMonth.of(today.year, today.monthValue)
        val logsForMonth = allLogs.filter { LocalDate.parse(it.dateStr).monthValue == currentMonth.monthValue }

        val onTimeCount = logsForMonth.count { it.status == WakeStatus.ON_TIME }
        val daysDivider = today.dayOfMonth

        val consistencyFraction = if (daysDivider > 0) onTimeCount.toFloat() / daysDivider else 0f
        val consistencyPercentage = (consistencyFraction * 100).toInt()

        postSuccessNotification(status, scanTimeMs, consistencyPercentage)
    }

    private fun postSuccessNotification(status: WakeStatus, scanTimeMs: Long, consistency: Int) {
        val channelId = "wake_tracker_updates"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "WakeTracker Live Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily wake status and consistency tracking"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeFormatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(scanTimeMs))

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Wake up logged")
            .setContentText("Woke up at $timeFormatted")
            .setProgress(100, consistency, false)
            // .setColor() is intentionally removed here so the system handles the tint!
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        notificationManager.notify(todayNotificationId(), builder.build())
    }
    private fun todayNotificationId(): Int {
        val today = LocalDate.now()
        return today.year * 1000 + today.dayOfYear
    }
}