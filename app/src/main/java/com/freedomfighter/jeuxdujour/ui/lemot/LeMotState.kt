package com.freedomfighter.jeuxdujour.ui.lemot

data class LeMotState(
    val targetWord: String = "",
    val guesses: List<String> = emptyList(),
    val evaluations: List<List<LetterEvaluation>> = emptyList(),
    val currentInput: String = "",
    val gameStatus: GameStatus = GameStatus.LOADING,
    val message: String? = null,
    val revealingRow: Int = -1
) {
    val currentRow: Int get() = guesses.size
    val isGameOver: Boolean get() = gameStatus == GameStatus.WON || gameStatus == GameStatus.LOST
}

enum class GameStatus {
    LOADING, PLAYING, WON, LOST
}

enum class LetterEvaluation {
    CORRECT, PRESENT, ABSENT
}
