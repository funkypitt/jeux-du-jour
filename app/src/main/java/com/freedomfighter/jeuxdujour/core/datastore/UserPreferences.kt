package com.freedomfighter.jeuxdujour.core.datastore

data class UserPreferences(
    val soundEnabled: Boolean = true,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0
)
