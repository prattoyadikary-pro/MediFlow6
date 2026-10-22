package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.UnitSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mediflow_preferences")

class MediFlowPreferences(private val context: Context) {

    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode") // SYSTEM, LIGHT, DARK
        val KEY_UNIT_SYSTEM = stringPreferencesKey("unit_system") // METRIC, IMPERIAL
        val KEY_SNOOZE_MINUTES = intPreferencesKey("snooze_minutes") // 10
        val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val KEY_VIBRATE_ENABLED = booleanPreferencesKey("vibrate_enabled")
        val KEY_HIDE_NAMES_LOCKSCREEN = booleanPreferencesKey("hide_names_lockscreen")
        val KEY_QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        val KEY_QUIET_HOURS_START = stringPreferencesKey("quiet_hours_start") // "22:00"
        val KEY_QUIET_HOURS_END = stringPreferencesKey("quiet_hours_end") // "07:00"
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_THEME_MODE] ?: "SYSTEM"
    }

    val unitSystem: Flow<UnitSystem> = context.dataStore.data.map { preferences ->
        val name = preferences[KEY_UNIT_SYSTEM] ?: UnitSystem.METRIC.name
        try { UnitSystem.valueOf(name) } catch (e: Exception) { UnitSystem.METRIC }
    }

    val snoozeMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_SNOOZE_MINUTES] ?: 10
    }

    val soundEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_SOUND_ENABLED] ?: true
    }

    val vibrateEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_VIBRATE_ENABLED] ?: true
    }

    val hideNamesOnLockScreen: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_HIDE_NAMES_LOCKSCREEN] ?: false
    }

    val quietHoursEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_QUIET_HOURS_ENABLED] ?: false
    }

    val quietHoursStart: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_QUIET_HOURS_START] ?: "22:00"
    }

    val quietHoursEnd: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_QUIET_HOURS_END] ?: "07:00"
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    suspend fun setUnitSystem(unit: UnitSystem) {
        context.dataStore.edit { it[KEY_UNIT_SYSTEM] = unit.name }
    }

    suspend fun setSnoozeMinutes(minutes: Int) {
        context.dataStore.edit { it[KEY_SNOOZE_MINUTES] = minutes }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SOUND_ENABLED] = enabled }
    }

    suspend fun setVibrateEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_VIBRATE_ENABLED] = enabled }
    }

    suspend fun setHideNamesOnLockScreen(hide: Boolean) {
        context.dataStore.edit { it[KEY_HIDE_NAMES_LOCKSCREEN] = hide }
    }

    suspend fun setQuietHours(enabled: Boolean, start: String, end: String) {
        context.dataStore.edit {
            it[KEY_QUIET_HOURS_ENABLED] = enabled
            it[KEY_QUIET_HOURS_START] = start
            it[KEY_QUIET_HOURS_END] = end
        }
    }
}
