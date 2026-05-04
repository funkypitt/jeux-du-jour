package com.freedomfighter.jeuxdujour.ui.hexagone

import androidx.sqlite.db.SimpleSQLiteQuery
import com.freedomfighter.jeuxdujour.core.database.WordDao
import com.freedomfighter.jeuxdujour.core.database.WordEntity
import com.freedomfighter.jeuxdujour.core.seed.SeededRandom
import com.freedomfighter.jeuxdujour.core.seed.SeedGenerator
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HexagoneRepository @Inject constructor(
    private val wordDao: WordDao
) {
    companion object {
        private const val GAME_SALT = "hexagone"
        const val MIN_WORD_LENGTH = 4
        const val PANGRAM_BONUS = 7
        private val ALL_LETTERS = ('a'..'z').toSet()
    }

    data class PuzzleData(
        val centerLetter: Char,
        val allLetters: Set<Char>,
        val validWords: Set<String>,
        val pangrams: Set<String>,
        val maxScore: Int
    )

    suspend fun generatePuzzle(date: LocalDate): PuzzleData {
        val seed = SeedGenerator.seedForGame(date, GAME_SALT)
        val rng = SeededRandom(seed)

        // Pick a pangram candidate using offset (avoids loading 7K rows)
        val count = wordDao.countPangramCandidates()
        if (count == 0) return fallbackPuzzle()

        val pangramOffset = rng.nextInt(count)
        val pangram = wordDao.getPangramCandidateAtOffset(pangramOffset) ?: return fallbackPuzzle()
        val letters = pangram.letterSet.toSet()

        // Pick center letter from the 7
        val letterList = letters.toList()
        val centerIndex = rng.nextInt(letterList.size)
        val centerLetter = letterList[centerIndex]

        // Build SQL that excludes all letters NOT in our 7-letter set
        val validWords = queryValidWords(letters, centerLetter)
        val pangrams = validWords.filter { word ->
            word.toSet() == letters
        }.toSet()

        val maxScore = calculateMaxScore(validWords, pangrams)

        return PuzzleData(
            centerLetter = centerLetter,
            allLetters = letters,
            validWords = validWords,
            pangrams = pangrams,
            maxScore = maxScore
        )
    }

    private suspend fun queryValidWords(letters: Set<Char>, centerLetter: Char): Set<String> {
        // Build NOT LIKE clauses for each letter NOT in our set
        val forbidden = ALL_LETTERS - letters
        val notLikeClauses = forbidden.joinToString(" AND ") { c ->
            "letters_ascii NOT LIKE '%$c%'"
        }

        val sql = """
            SELECT * FROM words
            WHERE letter_count >= $MIN_WORD_LENGTH
            AND letters_ascii LIKE '%$centerLetter%'
            AND $notLikeClauses
        """.trimIndent()

        val query = SimpleSQLiteQuery(sql)
        val results = wordDao.rawQuery(query)
        return results.map { it.lettersAscii }.toSet()
    }

    fun scoreWord(word: String, pangrams: Set<String>): Int {
        return when {
            word.length == MIN_WORD_LENGTH -> 1
            word in pangrams -> word.length + PANGRAM_BONUS
            else -> word.length
        }
    }

    private fun calculateMaxScore(validWords: Set<String>, pangrams: Set<String>): Int {
        return validWords.sumOf { scoreWord(it, pangrams) }
    }

    private fun fallbackPuzzle(): PuzzleData {
        return PuzzleData(
            centerLetter = 'e',
            allLetters = setOf('a', 'b', 'e', 'i', 'l', 'r', 't'),
            validWords = setOf("liberte", "batelier", "litiere", "libre", "litre", "bile", "tire"),
            pangrams = setOf("liberte"),
            maxScore = 30
        )
    }
}
