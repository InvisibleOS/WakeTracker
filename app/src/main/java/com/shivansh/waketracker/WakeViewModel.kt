package com.shivansh.waketracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shivansh.waketracker.data.WakeDatabase
import com.shivansh.waketracker.data.WakeLog
import com.shivansh.waketracker.data.WakeStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

class WakeViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = WakeDatabase.getDatabase(application).wakeDao()

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth

    @OptIn(ExperimentalCoroutinesApi::class) // This clears the yellow warning
    val monthlyLogs: StateFlow<List<WakeLog>> = _currentMonth
        .flatMapLatest { month ->
            val yearMonthStr = month.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            dao.getLogsForMonth(yearMonthStr)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        generateMockData()
    }

    private fun generateMockData() {
        viewModelScope.launch {
            val now = YearMonth.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val zoneId = ZoneId.systemDefault()
            
            // Generate successful 8 AM scans for the first 15 days of the current month
            for (i in 1..15) {
                val date = now.atDay(i)
                if (date.isAfter(LocalDate.now())) break

                val dateStr = date.format(formatter)
                val scanTime = date.atTime(8, 0).atZone(zoneId).toInstant().toEpochMilli()
                val targetTime = date.atTime(8, 0).atZone(zoneId).toInstant().toEpochMilli()
                
                dao.insertLog(
                    WakeLog(
                        dateStr = dateStr,
                        targetTimeMs = targetTime,
                        actualScanTimeMs = scanTime,
                        status = WakeStatus.ON_TIME
                    )
                )
            }
        }
    }

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    fun previousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun clearAllData() {
        viewModelScope.launch {
            dao.deleteAllLogs()
        }
    }

    fun scheduleReminders(context: Context, targetHour: Int, targetMinute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            
            // If the time has already passed today, target is for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        
        val targetTimeMs = calendar.timeInMillis
        val liveUpdateTimeMs = targetTimeMs - (30 * 60 * 1000)

        // Intent for Live Update
        val liveUpdateIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_SHOW_LIVE_UPDATE
            putExtra(ReminderReceiver.EXTRA_TARGET_TIME_MS, targetTimeMs)
        }
        val liveUpdatePendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            liveUpdateIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Intent for Missed Scan
        val missedScanIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_SHOW_MISSED
        }
        val missedScanPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            missedScanIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Cancel previous alarms
        alarmManager.cancel(liveUpdatePendingIntent)
        alarmManager.cancel(missedScanPendingIntent)

        try {
            // Schedule new alarms
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                liveUpdateTimeMs,
                liveUpdatePendingIntent
            )
            
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                targetTimeMs,
                missedScanPendingIntent
            )
        } catch (e: SecurityException) {
            // Ignore if exact alarm permission is denied
        }
    }
}