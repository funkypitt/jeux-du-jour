package com.freedomfighter.jeuxdujour.core.di

import android.content.Context
import androidx.room.Room
import com.freedomfighter.jeuxdujour.core.database.LexiqueDatabase
import com.freedomfighter.jeuxdujour.core.database.WordDao
import com.freedomfighter.jeuxdujour.core.datastore.PreferencesRepository
import com.freedomfighter.jeuxdujour.core.sound.SoundManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideLexiqueDatabase(@ApplicationContext context: Context): LexiqueDatabase {
        return Room.databaseBuilder(
            context,
            LexiqueDatabase::class.java,
            "lexique.db"
        )
            .createFromAsset("databases/lexique.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideWordDao(database: LexiqueDatabase): WordDao {
        return database.wordDao()
    }

    @Provides
    @Singleton
    fun provideSoundManager(
        @ApplicationContext context: Context,
        prefsRepository: PreferencesRepository
    ): SoundManager {
        return SoundManager(context, prefsRepository)
    }
}
