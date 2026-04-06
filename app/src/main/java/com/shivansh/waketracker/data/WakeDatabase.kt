package com.shivansh.waketracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WakeLog::class], version = 1, exportSchema = false)
abstract class WakeDatabase : RoomDatabase() {

    abstract fun wakeDao(): WakeDao

    companion object {
        @Volatile
        private var INSTANCE: WakeDatabase? = null

        fun getDatabase(context: Context): WakeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WakeDatabase::class.java,
                    "wake_tracker_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}