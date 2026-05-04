package com.freedomfighter.jeuxdujour.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [WordEntity::class], version = 1, exportSchema = false)
abstract class LexiqueDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
}
