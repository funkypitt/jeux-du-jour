package com.freedomfighter.jeuxdujour.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
interface WordDao {
    @Query("SELECT COUNT(*) FROM words WHERE is_common = 1 AND letter_count = :length")
    suspend fun countCommonWordsByLength(length: Int): Int

    @Query("SELECT * FROM words WHERE is_common = 1 AND letter_count = :length ORDER BY letters_ascii LIMIT 1 OFFSET :offset")
    suspend fun getWordAtOffset(length: Int, offset: Int): WordEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM words WHERE letters_ascii = :asciiWord AND letter_count = :length)")
    suspend fun isValidWord(asciiWord: String, length: Int): Boolean

    @Query("SELECT COUNT(*) FROM words WHERE is_common = 1 AND letter_count >= 4 AND length(letter_set) = 7")
    suspend fun countPangramCandidates(): Int

    @Query("""
        SELECT * FROM words
        WHERE is_common = 1
        AND letter_count >= 4
        AND length(letter_set) = 7
        ORDER BY frequency DESC
        LIMIT 1 OFFSET :offset
    """)
    suspend fun getPangramCandidateAtOffset(offset: Int): WordEntity?

    // Raw query for Hexagone — built dynamically to exclude letters not in the set
    @RawQuery
    suspend fun rawQuery(query: SupportSQLiteQuery): List<WordEntity>
}
