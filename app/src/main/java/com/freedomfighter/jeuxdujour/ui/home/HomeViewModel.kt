package com.freedomfighter.jeuxdujour.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freedomfighter.jeuxdujour.core.datastore.PreferencesRepository
import com.freedomfighter.jeuxdujour.core.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HomeState(
    val dateDisplay: String = "",
    val leMotStatus: GameCardStatus = GameCardStatus.TODO,
    val hexagoneStatus: GameCardStatus = GameCardStatus.TODO,
    val connexionsStatus: GameCardStatus = GameCardStatus.TODO,
    val soundEnabled: Boolean = true,
    val streak: Int = 0,
    val motDuJour: MotDuJourEntry? = null
)

enum class GameCardStatus(val label: String) {
    TODO("À faire"),
    IN_PROGRESS("En cours"),
    DONE("Terminé")
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val prefsRepository: PreferencesRepository,
    private val motDuJourRepository: MotDuJourRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        loadState()
    }

    fun loadState() {
        viewModelScope.launch {
            val date = DateUtils.today()
            val dateKey = DateUtils.formatKey(date)

            val leMotState = prefsRepository.getGameState("lemot", dateKey).first()
            val hexagoneState = prefsRepository.getGameState("hexagone", dateKey).first()
            val connexionsState = prefsRepository.getGameState("connexions", dateKey).first()
            val prefs = prefsRepository.preferences.first()

            val motDuJour = motDuJourRepository.getWordForDate(date)

            _state.value = HomeState(
                dateDisplay = DateUtils.formatDisplay(date),
                leMotStatus = parseLeMotStatus(leMotState),
                hexagoneStatus = parseHexagoneStatus(hexagoneState),
                connexionsStatus = parseConnexionsStatus(connexionsState),
                soundEnabled = prefs.soundEnabled,
                streak = prefs.currentStreak,
                motDuJour = motDuJour
            )
        }
    }

    fun toggleSound() {
        viewModelScope.launch {
            val current = _state.value.soundEnabled
            prefsRepository.setSoundEnabled(!current)
            _state.value = _state.value.copy(soundEnabled = !current)
        }
    }

    private fun parseLeMotStatus(saved: String?): GameCardStatus {
        if (saved == null || saved.isBlank()) return GameCardStatus.TODO
        val guesses = saved.split("|").filter { it.isNotBlank() }
        return when {
            guesses.size >= 6 -> GameCardStatus.DONE
            guesses.isNotEmpty() -> GameCardStatus.IN_PROGRESS
            else -> GameCardStatus.TODO
        }
    }

    private fun parseHexagoneStatus(saved: String?): GameCardStatus {
        if (saved == null || saved.isBlank()) return GameCardStatus.TODO
        val words = saved.split("|").filter { it.isNotBlank() }
        return if (words.isNotEmpty()) GameCardStatus.IN_PROGRESS else GameCardStatus.TODO
    }

    private fun parseConnexionsStatus(saved: String?): GameCardStatus {
        if (saved == null || saved.isBlank()) return GameCardStatus.TODO
        val solved = saved.split("|").filter { it.isNotBlank() }
        return when {
            solved.size >= 4 -> GameCardStatus.DONE
            solved.isNotEmpty() -> GameCardStatus.IN_PROGRESS
            else -> GameCardStatus.TODO
        }
    }
}
