package com.freedomfighter.jeuxdujour.core.seed

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object SeedGenerator {
    private val EPOCH = LocalDate.of(2024, 1, 1)

    fun seedForDate(date: LocalDate): Long {
        val days = ChronoUnit.DAYS.between(EPOCH, date)
        return murmurFinalize(days)
    }

    fun seedForGame(date: LocalDate, gameSalt: String): Long {
        val baseSeed = seedForDate(date)
        val saltHash = murmurFinalize(gameSalt.hashCode().toLong())
        return baseSeed xor saltHash
    }

    private fun murmurFinalize(value: Long): Long {
        var h = value
        h = h xor (h ushr 33)
        h *= -49064778989728563L // 0xff51afd7ed558ccdL
        h = h xor (h ushr 33)
        h *= -4265267296055464877L // 0xc4ceb9fe1a85ec53L
        h = h xor (h ushr 33)
        return h
    }
}
