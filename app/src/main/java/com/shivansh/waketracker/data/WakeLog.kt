package com.shivansh.waketracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wake_logs")
data class WakeLog(
    @PrimaryKey
    val dateStr: String, // e.g., "2026-04-05" - This makes sure there is only one entry per day
    val targetTimeMs: Long,      // The epoch time you intended to wake up
    val actualScanTimeMs: Long?, // The epoch time of the NFC tap (null if missed)
    val status: WakeStatus       // Custom enum for your UI dots
)

enum class WakeStatus {
    ON_TIME, LATE, MISSED, FUTURE
}