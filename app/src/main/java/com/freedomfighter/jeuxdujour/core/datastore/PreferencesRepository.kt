package com.freedomfighter.jeuxdujour.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "jeux_du_jour_prefs")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val CURRENT_STREAK = intPreferencesKey("current_streak")
        val MAX_STREAK = intPreferencesKey("max_streak")
        val GAMES_PLAYED = intPreferencesKey("games_played")
        val GAMES_WON = intPreferencesKey("games_won")
        val LAST_PLAY_DATE = stringPreferencesKey("last_play_date")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            currentStreak = prefs[Keys.CURRENT_STREAK] ?: 0,
            maxStreak = prefs[Keys.MAX_STREAK] ?: 0,
            gamesPlayed = prefs[Keys.GAMES_PLAYED] ?: 0,
            gamesWon = prefs[Keys.GAMES_WON] ?: 0
        )
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun recordGameResult(won: Boolean, dateString: String) {
        context.dataStore.edit { prefs ->
            val played = (prefs[Keys.GAMES_PLAYED] ?: 0) + 1
            prefs[Keys.GAMES_PLAYED] = played

            if (won) {
                val wonCount = (prefs[Keys.GAMES_WON] ?: 0) + 1
                prefs[Keys.GAMES_WON] = wonCount

                val lastDate = prefs[Keys.LAST_PLAY_DATE] ?: ""
                val streak = if (lastDate.isNotEmpty()) {
                    (prefs[Keys.CURRENT_STREAK] ?: 0) + 1
                } else {
                    1
                }
                prefs[Keys.CURRENT_STREAK] = streak
                val maxStreak = prefs[Keys.MAX_STREAK] ?: 0
                if (streak > maxStreak) prefs[Keys.MAX_STREAK] = streak
            } else {
                prefs[Keys.CURRENT_STREAK] = 0
            }
            prefs[Keys.LAST_PLAY_DATE] = dateString
        }
    }

    // Game-specific state persistence (keyed by date)
    fun getGameState(gameKey: String, date: String): Flow<String?> {
        val key = stringPreferencesKey("${gameKey}_$date")
        return context.dataStore.data.map { it[key] }
    }

    suspend fun saveGameState(gameKey: String, date: String, state: String) {
        val key = stringPreferencesKey("${gameKey}_$date")
        context.dataStore.edit { it[key] = state }
    }
}
