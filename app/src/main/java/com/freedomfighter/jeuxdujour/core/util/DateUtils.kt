package com.freedomfighter.jeuxdujour.core.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    private val DISPLAY_FORMAT = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)
    private val KEY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun today(): LocalDate = LocalDate.now()

    fun formatDisplay(date: LocalDate): String {
        return date.format(DISPLAY_FORMAT).replaceFirstChar { it.uppercase() }
    }

    fun formatKey(date: LocalDate): String {
        return date.format(KEY_FORMAT)
    }
}
