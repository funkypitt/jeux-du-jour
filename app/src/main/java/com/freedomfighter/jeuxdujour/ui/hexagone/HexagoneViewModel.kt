package com.freedomfighter.jeuxdujour.ui.hexagone

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
class HexagoneViewModel @Inject constructor(
    private val repository: HexagoneRepository,
    private val prefsRepository: PreferencesRepository,
    private val soundManager: SoundManager
) : ViewModel() {

    private val _state = MutableStateFlow(HexagoneState())
    val state: StateFlow<HexagoneState> = _state.asStateFlow()

    private val date: LocalDate = DateUtils.today()
    private val dateKey = DateUtils.formatKey(date)

    init {
        viewModelScope.launch {
            val puzzle = repository.generatePuzzle(date)
            val outerLetters = puzzle.allLetters.filter { it != puzzle.centerLetter }.toList()

            // Try to restore saved state
            val saved = prefsRepository.getGameState("hexagone", dateKey).first()
            val foundWords = if (saved != null && saved.isNotBlank()) {
                saved.split("|").filter { it.isNotBlank() }.toList()
            } else {
                emptyList()
            }

            val score = foundWords.sumOf { repository.scoreWord(it, puzzle.pangrams) }

            _state.value = HexagoneState(
                centerLetter = puzzle.centerLetter,
                outerLetters = outerLetters,
                foundWords = foundWords,
                score = score,
                maxScore = puzzle.maxScore,
                rank = Rank.forScore(score, puzzle.maxScore),
                gameStatus = HexagoneStatus.PLAYING,
                validWords = puzzle.validWords,
                pangrams = puzzle.pangrams
            )
        }
    }

    fun onLetterPress(letter: Char) {
        val current = _state.value
        if (current.gameStatus != HexagoneStatus.PLAYING) return
        soundManager.play(SoundEffect.TAP)
        _state.value = current.copy(
            currentInput = current.currentInput + letter,
            message = null
        )
    }

    fun onBackspace() {
        val current = _state.value
        if (current.currentInput.isNotEmpty()) {
            _state.value = current.copy(
                currentInput = current.currentInput.dropLast(1),
                message = null
            )
        }
    }

    fun onClearInput() {
        _state.value = _state.value.copy(currentInput = "", message = null)
    }

    fun onShuffle() {
        val current = _state.value
        val shuffled = current.outerLetters.shuffled()
        _state.value = current.copy(outerLetters = shuffled)
    }

    fun onSubmit() {
        val current = _state.value
        val word = current.currentInput.lowercase()

        when {
            word.length < HexagoneRepository.MIN_WORD_LENGTH -> {
                soundManager.play(SoundEffect.WRONG)
                _state.value = current.copy(message = "Trop court", currentInput = "")
            }
            !word.contains(current.centerLetter.lowercase()) -> {
                soundManager.play(SoundEffect.WRONG)
                _state.value = current.copy(message = "Lettre centrale manquante", currentInput = "")
            }
            word in current.foundWords -> {
                soundManager.play(SoundEffect.WRONG)
                _state.value = current.copy(message = "Déjà trouvé", currentInput = "")
            }
            word !in current.validWords -> {
                soundManager.play(SoundEffect.WRONG)
                _state.value = current.copy(message = "Mot non reconnu", currentInput = "")
            }
            else -> {
                val isPangram = word in current.pangrams
                val points = repository.scoreWord(word, current.pangrams)
                val newScore = current.score + points
                val newFound = current.foundWords + word
                val oldRank = current.rank
                val newRank = Rank.forScore(newScore, current.maxScore)
                val isComplete = newScore >= current.maxScore
                val msg = when {
                    isPangram -> "Pangramme ! +${points} pts"
                    points == 1 -> "+1 pt"
                    else -> "+$points pts"
                }

                when {
                    isComplete -> soundManager.play(SoundEffect.WIN)
                    isPangram || newRank != oldRank -> soundManager.play(SoundEffect.RANK_UP)
                    else -> soundManager.play(SoundEffect.CORRECT)
                }

                _state.value = current.copy(
                    foundWords = newFound,
                    currentInput = "",
                    score = newScore,
                    rank = newRank,
                    message = msg,
                    gameStatus = if (isComplete) HexagoneStatus.COMPLETE else HexagoneStatus.PLAYING,
                    showCelebration = isComplete,
                    showSuccessFlash = !isComplete
                )

                viewModelScope.launch {
                    prefsRepository.saveGameState("hexagone", dateKey, newFound.joinToString("|"))
                }
            }
        }
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
