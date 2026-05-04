package com.freedomfighter.jeuxdujour.ui.lemot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freedomfighter.jeuxdujour.core.datastore.PreferencesRepository
import com.freedomfighter.jeuxdujour.core.util.DateUtils
import com.freedomfighter.jeuxdujour.ui.components.TileState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class LeMotViewModel @Inject constructor(
    private val repository: LeMotRepository,
    private val prefsRepository: PreferencesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LeMotState())
    val state: StateFlow<LeMotState> = _state.asStateFlow()

    private val date: LocalDate = DateUtils.today()
    private val dateKey = DateUtils.formatKey(date)

    init {
        viewModelScope.launch {
            val target = repository.getTargetWord(date)
            // Try to restore saved state
            val saved = prefsRepository.getGameState("lemot", dateKey).first()
            if (saved != null) {
                restoreState(saved, target)
            } else {
                _state.value = LeMotState(
                    targetWord = target,
                    gameStatus = GameStatus.PLAYING
                )
            }
        }
    }

    fun onKeyPress(letter: Char) {
        val current = _state.value
        if (current.isGameOver || current.gameStatus == GameStatus.LOADING) return
        if (current.currentInput.length < LeMotRepository.WORD_LENGTH) {
            _state.value = current.copy(
                currentInput = current.currentInput + letter.uppercase(),
                message = null
            )
        }
    }

    fun onBackspace() {
        val current = _state.value
        if (current.isGameOver || current.gameStatus == GameStatus.LOADING) return
        if (current.currentInput.isNotEmpty()) {
            _state.value = current.copy(
                currentInput = current.currentInput.dropLast(1),
                message = null
            )
        }
    }

    fun onEnter() {
        val current = _state.value
        if (current.isGameOver || current.gameStatus == GameStatus.LOADING) return
        if (current.currentInput.length != LeMotRepository.WORD_LENGTH) {
            _state.value = current.copy(message = "Pas assez de lettres")
            return
        }

        viewModelScope.launch {
            val guess = current.currentInput.uppercase()
            if (!repository.isValidGuess(guess)) {
                _state.value = current.copy(message = "Mot inconnu")
                return@launch
            }

            val evaluation = repository.evaluateGuess(guess, current.targetWord)
            val newGuesses = current.guesses + guess
            val newEvaluations = current.evaluations + listOf(evaluation)
            val won = evaluation.all { it == LetterEvaluation.CORRECT }
            val lost = !won && newGuesses.size >= LeMotRepository.MAX_GUESSES

            val newStatus = when {
                won -> GameStatus.WON
                lost -> GameStatus.LOST
                else -> GameStatus.PLAYING
            }

            val message = when {
                won -> listOf("Génie !", "Magnifique !", "Superbe !", "Bien !", "De justesse !", "Ouf !")[newGuesses.size - 1]
                lost -> current.targetWord
                else -> null
            }

            _state.value = current.copy(
                guesses = newGuesses,
                evaluations = newEvaluations,
                currentInput = "",
                gameStatus = newStatus,
                message = message,
                revealingRow = newGuesses.size - 1
            )

            saveState()

            if (won || lost) {
                prefsRepository.recordGameResult(won, dateKey)
            }
        }
    }

    fun getLetterStates(): Map<Char, TileState> {
        val states = mutableMapOf<Char, TileState>()
        // Priority: CORRECT > PRESENT > ABSENT
        val priority = mapOf(
            TileState.CORRECT to 2,
            TileState.PRESENT to 1,
            TileState.ABSENT to 0
        )
        _state.value.guesses.forEachIndexed { rowIndex, guess ->
            val eval = _state.value.evaluations[rowIndex]
            guess.forEachIndexed { i, letter ->
                val newState = when (eval[i]) {
                    LetterEvaluation.CORRECT -> TileState.CORRECT
                    LetterEvaluation.PRESENT -> TileState.PRESENT
                    LetterEvaluation.ABSENT -> TileState.ABSENT
                }
                val existing = states[letter]
                val newPriority = priority[newState] ?: 0
                val existingPriority = priority[existing] ?: -1
                if (newPriority > existingPriority) {
                    states[letter] = newState
                }
            }
        }
        return states
    }

    fun getShareText(): String {
        val s = _state.value
        return repository.generateShareText(s.guesses, s.evaluations, s.gameStatus == GameStatus.WON, date)
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }

    fun clearRevealingRow() {
        _state.value = _state.value.copy(revealingRow = -1)
    }

    private suspend fun saveState() {
        val s = _state.value
        val data = s.guesses.joinToString("|")
        prefsRepository.saveGameState("lemot", dateKey, data)
    }

    private fun restoreState(saved: String, target: String) {
        if (saved.isBlank()) {
            _state.value = LeMotState(targetWord = target, gameStatus = GameStatus.PLAYING)
            return
        }
        val guesses = saved.split("|").filter { it.isNotBlank() }
        val evaluations = guesses.map { repository.evaluateGuess(it, target) }
        val won = evaluations.lastOrNull()?.all { it == LetterEvaluation.CORRECT } == true
        val lost = !won && guesses.size >= LeMotRepository.MAX_GUESSES

        _state.value = LeMotState(
            targetWord = target,
            guesses = guesses,
            evaluations = evaluations,
            gameStatus = when {
                won -> GameStatus.WON
                lost -> GameStatus.LOST
                else -> GameStatus.PLAYING
            },
            message = when {
                won -> "Bravo !"
                lost -> target
                else -> null
            }
        )
    }
}
