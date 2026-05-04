package com.freedomfighter.jeuxdujour.ui.lemot

import com.freedomfighter.jeuxdujour.core.database.WordDao
import com.freedomfighter.jeuxdujour.core.seed.SeededRandom
import com.freedomfighter.jeuxdujour.core.seed.SeedGenerator
import com.freedomfighter.jeuxdujour.core.util.FrenchTextUtils
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeMotRepository @Inject constructor(
    private val wordDao: WordDao
) {
    companion object {
        const val WORD_LENGTH = 5
        const val MAX_GUESSES = 6
        private const val GAME_SALT = "lemot"
    }

    suspend fun getTargetWord(date: LocalDate): String {
        val seed = SeedGenerator.seedForGame(date, GAME_SALT)
        val rng = SeededRandom(seed)
        val count = wordDao.countCommonWordsByLength(WORD_LENGTH)
        if (count == 0) return "ESSAI" // fallback
        val offset = rng.nextInt(count)
        val word = wordDao.getWordAtOffset(WORD_LENGTH, offset)
        return word?.lettersAscii?.uppercase() ?: "ESSAI"
    }

    suspend fun isValidGuess(guess: String): Boolean {
        val ascii = FrenchTextUtils.toAsciiUpperCase(guess)
        return wordDao.isValidWord(ascii.lowercase(), WORD_LENGTH)
    }

    fun evaluateGuess(guess: String, target: String): List<LetterEvaluation> {
        val result = MutableList(WORD_LENGTH) { LetterEvaluation.ABSENT }
        val targetChars = target.toCharArray()
        val guessChars = guess.uppercase().toCharArray()
        val used = BooleanArray(WORD_LENGTH)

        // First pass: exact matches
        for (i in 0 until WORD_LENGTH) {
            if (guessChars[i] == targetChars[i]) {
                result[i] = LetterEvaluation.CORRECT
                used[i] = true
            }
        }

        // Second pass: present but wrong position
        for (i in 0 until WORD_LENGTH) {
            if (result[i] == LetterEvaluation.CORRECT) continue
            for (j in 0 until WORD_LENGTH) {
                if (!used[j] && guessChars[i] == targetChars[j]) {
                    result[i] = LetterEvaluation.PRESENT
                    used[j] = true
                    break
                }
            }
        }

        return result
    }

    fun generateShareText(guesses: List<String>, evaluations: List<List<LetterEvaluation>>, won: Boolean, date: LocalDate): String {
        val dayNumber = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.of(2024, 1, 1), date)
        val score = if (won) "${guesses.size}/$MAX_GUESSES" else "X/$MAX_GUESSES"
        val grid = evaluations.joinToString("\n") { row ->
            row.joinToString("") { eval ->
                when (eval) {
                    LetterEvaluation.CORRECT -> "\uD83D\uDFE9"  // green
                    LetterEvaluation.PRESENT -> "\uD83D\uDFE8"  // yellow
                    LetterEvaluation.ABSENT -> "\u2B1B"          // black
                }
            }
        }
        return "Le Mot #$dayNumber $score\n\n$grid"
    }
}
