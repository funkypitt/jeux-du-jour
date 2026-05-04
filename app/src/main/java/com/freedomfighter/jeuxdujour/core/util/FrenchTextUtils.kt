package com.freedomfighter.jeuxdujour.core.util

import java.text.Normalizer

object FrenchTextUtils {
    fun stripAccents(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        return normalized.replace(Regex("[\\p{InCombiningDiacriticalMarks}]"), "")
    }

    fun toAsciiUpperCase(input: String): String {
        return stripAccents(input.uppercase())
    }

    fun isAlphaOnly(input: String): Boolean {
        return input.all { it.isLetter() }
    }

    fun sortedUniqueLetters(input: String): String {
        return input.lowercase().toSortedSet().joinToString("")
    }
}
