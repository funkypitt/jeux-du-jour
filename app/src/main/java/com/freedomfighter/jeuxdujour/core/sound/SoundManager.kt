package com.freedomfighter.jeuxdujour.core.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.freedomfighter.jeuxdujour.R
import com.freedomfighter.jeuxdujour.core.datastore.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class SoundEffect {
    TAP, POP, CORRECT, WRONG, RANK_UP, WIN, LOSE
}

@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefsRepository: PreferencesRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundIds = mutableMapOf<SoundEffect, Int>()

    init {
        soundIds[SoundEffect.TAP] = soundPool.load(context, R.raw.tap, 1)
        soundIds[SoundEffect.POP] = soundPool.load(context, R.raw.pop, 1)
        soundIds[SoundEffect.CORRECT] = soundPool.load(context, R.raw.correct, 1)
        soundIds[SoundEffect.WRONG] = soundPool.load(context, R.raw.wrong, 1)
        soundIds[SoundEffect.RANK_UP] = soundPool.load(context, R.raw.rank_up, 1)
        soundIds[SoundEffect.WIN] = soundPool.load(context, R.raw.win, 1)
        soundIds[SoundEffect.LOSE] = soundPool.load(context, R.raw.lose, 1)
    }

    fun play(effect: SoundEffect) {
        scope.launch {
            val enabled = prefsRepository.preferences.first().soundEnabled
            if (!enabled) return@launch
            val id = soundIds[effect] ?: return@launch
            soundPool.play(id, 1f, 1f, 1, 0, 1f)
        }
    }
}
