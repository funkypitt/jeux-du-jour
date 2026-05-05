package com.freedomfighter.jeuxdujour.ui.home

import android.content.Context
import com.freedomfighter.jeuxdujour.core.seed.SeedGenerator
import com.freedomfighter.jeuxdujour.core.seed.SeededRandom
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@JsonClass(generateAdapter = true)
data class MotDuJourEntry(
    val terme: String,
    val prononciation: String? = null,
    val nature: String,
    val definition: String,
    val etymologie: String? = null
)

@Singleton
class MotDuJourRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var entries: List<MotDuJourEntry>? = null

    private fun loadEntries(): List<MotDuJourEntry> {
        entries?.let { return it }

        val json = context.assets.open("mot_du_jour.json")
            .bufferedReader()
            .use { it.readText() }

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val type = Types.newParameterizedType(List::class.java, MotDuJourEntry::class.java)
        val adapter = moshi.adapter<List<MotDuJourEntry>>(type)
        val loaded = adapter.fromJson(json) ?: emptyList()
        entries = loaded
        return loaded
    }

    fun getWordForDate(date: LocalDate = LocalDate.now()): MotDuJourEntry? {
        val allEntries = loadEntries()
        if (allEntries.isEmpty()) return null

        val seed = SeedGenerator.seedForGame(date, "motdujour")
        val rng = SeededRandom(seed)
        val index = rng.nextInt(allEntries.size)
        return allEntries[index]
    }
}
