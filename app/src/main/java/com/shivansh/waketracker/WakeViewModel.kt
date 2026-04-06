package com.shivansh.waketracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shivansh.waketracker.data.WakeDatabase
import com.shivansh.waketracker.data.WakeLog // Added this missing import
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import java.time.format.DateTimeFormatter

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

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    fun previousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }
}