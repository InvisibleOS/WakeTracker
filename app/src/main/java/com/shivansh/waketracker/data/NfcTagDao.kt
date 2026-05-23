package com.shivansh.waketracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NfcTagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: NfcTag)

    @Delete
    suspend fun deleteTag(tag: NfcTag)

    @Query("SELECT * FROM nfc_tags ORDER BY createdAt DESC")
    fun getAllTags(): Flow<List<NfcTag>>
}
