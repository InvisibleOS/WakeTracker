package com.shivansh.waketracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Single DataStore instance scoped to the application process. */
private val Context.bufferTimeDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "buffer_settings")

object BufferTimePreferences {

    private val BUFFER_TIME_KEY = intPreferencesKey("buffer_time_minutes")

    /** Default buffer time in minutes when no value has been set. */
    const val DEFAULT_BUFFER_MINUTES = 60

    /** Preset options exposed to the Settings UI. */
    val PRESET_OPTIONS = listOf(15, 30, 60, 120)

    /**
     * Observe the stored buffer time as a [Flow].
     * Emits [DEFAULT_BUFFER_MINUTES] when no value has been persisted yet.
     */
    fun getBufferTimeMinutes(context: Context): Flow<Int> =
        context.bufferTimeDataStore.data.map { prefs ->
            prefs[BUFFER_TIME_KEY] ?: DEFAULT_BUFFER_MINUTES
        }

    /** Persist a new buffer time value. */
    suspend fun setBufferTimeMinutes(context: Context, minutes: Int) {
        context.bufferTimeDataStore.edit { prefs ->
            prefs[BUFFER_TIME_KEY] = minutes
        }
    }
}
