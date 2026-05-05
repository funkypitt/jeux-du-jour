package com.freedomfighter.jeuxdujour.ui.connexions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freedomfighter.jeuxdujour.core.datastore.PreferencesRepository
import com.freedomfighter.jeuxdujour.core.sound.SoundEffect
import com.freedomfighter.jeuxdujour.core.sound.SoundManager
import com.freedomfighter.jeuxdujour.core.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ConnexionsViewModel @Inject constructor(
    private val repository: ConnexionsRepository,
    private val prefsRepository: PreferencesRepository,
    private val soundManager: SoundManager
) : ViewModel() {

    private val _state = MutableStateFlow(ConnexionsState())
    val state: StateFlow<ConnexionsState> = _state.asStateFlow()

    private val date: LocalDate = DateUtils.today()
    private val dateKey = DateUtils.formatKey(date)

    init {
        viewModelScope.launch {
            val puzzle = repository.getPuzzle(date)

            // Try to restore saved state
            val saved = prefsRepository.getGameState("connexions", dateKey).first()
            val solvedCategories = if (saved != null && saved.isNotBlank()) {
                saved.split("|").filter { it.isNotBlank() }.toSet()
            } else {
                emptySet()
            }

            val solvedGroups = puzzle.groups.filter { it.category in solvedCategories }

            val status = when {
                solvedGroups.size == ConnexionsRepository.NUM_GROUPS -> ConnexionsStatus.WON
                else -> ConnexionsStatus.PLAYING
            }

            _state.value = ConnexionsState(
                groups = puzzle.groups,
                shuffledWords = puzzle.shuffledWords,
                solvedGroups = solvedGroups,
                gameStatus = status
            )
        }
    }

    fun onWordToggle(word: String) {
        val current = _state.value
        if (current.isGameOver) return

        soundManager.play(SoundEffect.TAP)

        val newSelected = if (word in current.selectedWords) {
            current.selectedWords - word
        } else {
            if (current.selectedWords.size >= ConnexionsRepository.WORDS_PER_GROUP) return
            current.selectedWords + word
        }
        _state.value = current.copy(selectedWords = newSelected, message = null)
    }

    fun onSubmit() {
        val current = _state.value
        if (current.selectedWords.size != ConnexionsRepository.WORDS_PER_GROUP) return
        if (current.isGameOver) return

        val matchedGroup = repository.checkGuess(current.selectedWords, current.groups)

        if (matchedGroup != null) {
            val newSolved = current.solvedGroups + matchedGroup
            val won = newSolved.size == ConnexionsRepository.NUM_GROUPS

            if (won) {
                soundManager.play(SoundEffect.WIN)
            } else {
                soundManager.play(SoundEffect.CORRECT)
            }

            _state.value = current.copy(
                solvedGroups = newSolved,
                selectedWords = emptySet(),
                gameStatus = if (won) ConnexionsStatus.WON else ConnexionsStatus.PLAYING,
                message = if (won) "Bravo !" else null,
                lastWrongGuess = null,
                showCelebration = won,
                showSuccessFlash = !won
            )

            viewModelScope.launch {
                val solvedData = newSolved.joinToString("|") { it.category }
                prefsRepository.saveGameState("connexions", dateKey, solvedData)
                if (won) prefsRepository.recordGameResult(true, dateKey)
            }
        } else {
            val newMistakes = current.mistakes + 1
            val isOneAway = repository.isOneAway(current.selectedWords, current.groups)
            val lost = newMistakes >= current.maxMistakes

            if (lost) {
                soundManager.play(SoundEffect.LOSE)
            } else {
                soundManager.play(SoundEffect.WRONG)
            }

            _state.value = current.copy(
                mistakes = newMistakes,
                selectedWords = if (lost) emptySet() else current.selectedWords,
                gameStatus = if (lost) ConnexionsStatus.LOST else ConnexionsStatus.PLAYING,
                message = when {
                    lost -> "Perdu !"
                    isOneAway -> "Presque !"
                    else -> "Incorrect"
                },
                lastWrongGuess = current.selectedWords
            )

            if (lost) {
                // Reveal all groups
                _state.value = _state.value.copy(solvedGroups = current.groups)
                viewModelScope.launch {
                    prefsRepository.recordGameResult(false, dateKey)
                }
            }
        }
    }

    fun onDeselectAll() {
        _state.value = _state.value.copy(selectedWords = emptySet(), message = null)
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }

    fun dismissCelebration() {
        _state.value = _state.value.copy(showCelebration = false)
    }

    fun dismissSuccessFlash() {
        _state.value = _state.value.copy(showSuccessFlash = false)
    }
}
