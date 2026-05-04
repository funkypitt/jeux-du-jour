package com.freedomfighter.jeuxdujour.ui.connexions

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class PuzzleJson(
        val groups: List<GroupJson>
    )

    data class GroupJson(
        val category: String,
        val words: List<String>,
        val difficulty: Int
    )

    fun loadPuzzles(): List<PuzzleJson> {
        val json = context.assets.open("connexions_groups.json")
            .bufferedReader().use { it.readText() }
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val type = Types.newParameterizedType(List::class.java, PuzzleJson::class.java)
        val adapter = moshi.adapter<List<PuzzleJson>>(type)
        return adapter.fromJson(json) ?: emptyList()
    }
}
