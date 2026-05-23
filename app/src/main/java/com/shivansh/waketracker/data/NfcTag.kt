package com.shivansh.waketracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nfc_tags")
data class NfcTag(
    @PrimaryKey val id: String,
    val label: String,
    val createdAt: Long = System.currentTimeMillis()
)
