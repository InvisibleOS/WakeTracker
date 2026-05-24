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
import android.content.Context
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
}