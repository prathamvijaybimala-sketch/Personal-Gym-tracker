package com.gympro.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Key-value settings — the direct counterpart of the web app's scalar
 * localStorage keys. Defaults match the web app (dark theme, 110g protein,
 * 8 glasses water, 90s rest timer).
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val ACTIVE_ROUTINE = stringPreferencesKey("activeRoutineId")
        val PROTEIN_GOAL = intPreferencesKey("proteinGoal")
        val WATER_TARGET = intPreferencesKey("waterTarget")
        val TIMER_DURATION = intPreferencesKey("timerDuration")
        val BF_HEIGHT = stringPreferencesKey("bfHeight")
        val UWT = stringPreferencesKey("uwt")
    }

    val theme: Flow<String> = context.dataStore.data.map { it[Keys.THEME] ?: "dark" }
    val activeRoutineId: Flow<String?> = context.dataStore.data.map { it[Keys.ACTIVE_ROUTINE] }
    val proteinGoal: Flow<Int> = context.dataStore.data.map { it[Keys.PROTEIN_GOAL] ?: 110 }
    val waterTarget: Flow<Int> = context.dataStore.data.map { it[Keys.WATER_TARGET] ?: 8 }
    val timerDuration: Flow<Int> = context.dataStore.data.map { it[Keys.TIMER_DURATION] ?: 90 }
    val bfHeight: Flow<String> = context.dataStore.data.map { it[Keys.BF_HEIGHT] ?: "" }
    val uwt: Flow<String> = context.dataStore.data.map { it[Keys.UWT] ?: "--" }

    suspend fun setTheme(value: String) = context.dataStore.edit { it[Keys.THEME] = value }
    suspend fun setActiveRoutineId(value: String) = context.dataStore.edit { it[Keys.ACTIVE_ROUTINE] = value }
    suspend fun setProteinGoal(value: Int) = context.dataStore.edit { it[Keys.PROTEIN_GOAL] = value }
    suspend fun setWaterTarget(value: Int) = context.dataStore.edit { it[Keys.WATER_TARGET] = value }
    suspend fun setTimerDuration(value: Int) = context.dataStore.edit { it[Keys.TIMER_DURATION] = value }
    suspend fun setBfHeight(value: String) = context.dataStore.edit { it[Keys.BF_HEIGHT] = value }
    suspend fun setUwt(value: String) = context.dataStore.edit { it[Keys.UWT] = value }

    suspend fun clear() = context.dataStore.edit { it.clear() }
}
