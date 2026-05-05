package com.freedomfighter.jeuxdujour.ui.hexagone

data class HexagoneState(
    val centerLetter: Char = ' ',
    val outerLetters: List<Char> = emptyList(),
    val foundWords: List<String> = emptyList(),
    val currentInput: String = "",
    val score: Int = 0,
    val maxScore: Int = 0,
    val rank: Rank = Rank.DEBUTANT,
    val gameStatus: HexagoneStatus = HexagoneStatus.LOADING,
    val message: String? = null,
    val validWords: Set<String> = emptySet(),
    val pangrams: Set<String> = emptySet(),
    val showCelebration: Boolean = false,
    val showSuccessFlash: Boolean = false
) {
    val allLetters: List<Char> get() = listOf(centerLetter) + outerLetters
}

enum class HexagoneStatus {
    LOADING, PLAYING, COMPLETE
}

enum class Rank(val label: String, val thresholdPercent: Float) {
    DEBUTANT("Débutant", 0f),
    BON_DEBUT("Bon début", 0.02f),
    AVANCE("Avancé", 0.05f),
    HABILE("Habile", 0.08f),
    COMPETENT("Compétent", 0.15f),
    TALENTUEUX("Talentueux", 0.25f),
    EXPERT("Expert", 0.40f),
    EXCEPTIONNEL("Exceptionnel", 0.50f),
    GENIE("Génie", 0.70f);

    companion object {
        fun forScore(score: Int, maxScore: Int): Rank {
            if (maxScore == 0) return DEBUTANT
            val ratio = score.toFloat() / maxScore
            return entries.lastOrNull { ratio >= it.thresholdPercent } ?: DEBUTANT
        }
    }
}
