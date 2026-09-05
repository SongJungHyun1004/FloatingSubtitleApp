package com.joker.floatingsubtitleapp.di

import android.content.Context
import androidx.room.Room
import com.joker.floatingsubtitleapp.data.history.HistoryDao
import com.joker.floatingsubtitleapp.data.history.HistoryDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideHistoryDatabase(@ApplicationContext context: Context): HistoryDatabase =
        Room.databaseBuilder(context, HistoryDatabase::class.java, "subtitle_history.db").build()

    @Provides
    fun provideHistoryDao(database: HistoryDatabase): HistoryDao = database.historyDao()
}