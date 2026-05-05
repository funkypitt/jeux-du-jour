package com.freedomfighter.jeuxdujour.ui.connexions

data class ConnexionsState(
    val groups: List<ConnexionGroup> = emptyList(),
    val shuffledWords: List<String> = emptyList(),
    val selectedWords: Set<String> = emptySet(),
    val solvedGroups: List<ConnexionGroup> = emptyList(),
    val mistakes: Int = 0,
    val maxMistakes: Int = 4,
    val gameStatus: ConnexionsStatus = ConnexionsStatus.LOADING,
    val message: String? = null,
    val lastWrongGuess: Set<String>? = null,
    val showCelebration: Boolean = false,
    val showSuccessFlash: Boolean = false
) {
    val remainingWords: List<String>
        get() {
            val solvedWords = solvedGroups.flatMap { it.words }.toSet()
            return shuffledWords.filter { it !in solvedWords }
        }
    val isGameOver: Boolean
        get() = gameStatus == ConnexionsStatus.WON || gameStatus == ConnexionsStatus.LOST
}

enum class ConnexionsStatus {
    LOADING, PLAYING, WON, LOST
}

data class ConnexionGroup(
    val category: String,
    val words: List<String>,
    val difficulty: Int // 0=yellow, 1=green, 2=blue, 3=purple
)
