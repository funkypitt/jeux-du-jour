package com.freedomfighter.jeuxdujour.ui.connexions

import com.freedomfighter.jeuxdujour.core.seed.SeededRandom
import com.freedomfighter.jeuxdujour.core.seed.SeedGenerator
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnexionsRepository @Inject constructor(
    private val groupLoader: GroupLoader
) {
    companion object {
        private const val GAME_SALT = "connexions"
        const val MAX_MISTAKES = 4
        const val WORDS_PER_GROUP = 4
        const val NUM_GROUPS = 4
    }

    data class PuzzleData(
        val groups: List<ConnexionGroup>,
        val shuffledWords: List<String>
    )

    fun getPuzzle(date: LocalDate): PuzzleData {
        val seed = SeedGenerator.seedForGame(date, GAME_SALT)
        val rng = SeededRandom(seed)

        val puzzles = groupLoader.loadPuzzles()
        if (puzzles.isEmpty()) return fallbackPuzzle()

        val puzzleIndex = rng.nextInt(puzzles.size)
        val puzzle = puzzles[puzzleIndex]

        val groups = puzzle.groups.map { g ->
            ConnexionGroup(
                category = g.category,
                words = g.words,
                difficulty = g.difficulty
            )
        }

        val allWords = groups.flatMap { it.words }.toMutableList()
        rng.shuffle(allWords)

        return PuzzleData(groups = groups, shuffledWords = allWords)
    }

    fun checkGuess(selectedWords: Set<String>, groups: List<ConnexionGroup>): ConnexionGroup? {
        return groups.find { group ->
            group.words.toSet() == selectedWords
        }
    }

    fun isOneAway(selectedWords: Set<String>, groups: List<ConnexionGroup>): Boolean {
        return groups.any { group ->
            (group.words.toSet() intersect selectedWords).size == WORDS_PER_GROUP - 1
        }
    }

    private fun fallbackPuzzle(): PuzzleData {
        val groups = listOf(
            ConnexionGroup("Couleurs", listOf("ROUGE", "BLEU", "VERT", "JAUNE"), 0),
            ConnexionGroup("Animaux", listOf("CHAT", "CHIEN", "OURS", "LOUP"), 1),
            ConnexionGroup("Fruits", listOf("POMME", "POIRE", "PECHE", "PRUNE"), 2),
            ConnexionGroup("Villes", listOf("PARIS", "LYON", "NICE", "LILLE"), 3)
        )
        return PuzzleData(groups = groups, shuffledWords = groups.flatMap { it.words }.shuffled())
    }
}
