package com.shivansh.waketracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WakeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WakeLog)

    @Query("SELECT * FROM wake_logs WHERE dateStr = :date LIMIT 1")
    suspend fun getLogByDate(date: String): WakeLog?

    @Query("SELECT * FROM wake_logs WHERE dateStr LIKE :yearMonth || '%' ORDER BY dateStr ASC")
    fun getLogsForMonth(yearMonth: String): Flow<List<WakeLog>>

    // --- NEW METHOD FOR BACKGROUND NOTIFICATIONS ---
    @Query("SELECT * FROM wake_logs")
    suspend fun getAllLogsSnapshot(): List<WakeLog>
}