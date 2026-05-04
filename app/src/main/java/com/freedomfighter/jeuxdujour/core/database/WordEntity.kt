package com.freedomfighter.jeuxdujour.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "words")
data class WordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "ortho")
    val ortho: String,

    @ColumnInfo(name = "letters_ascii")
    val lettersAscii: String,

    @ColumnInfo(name = "letter_count")
    val letterCount: Int,

    @ColumnInfo(name = "letter_set")
    val letterSet: String,

    @ColumnInfo(name = "frequency")
    val frequency: Float,

    @ColumnInfo(name = "cgram")
    val cgram: String,

    @ColumnInfo(name = "is_common")
    val isCommon: Boolean
)
