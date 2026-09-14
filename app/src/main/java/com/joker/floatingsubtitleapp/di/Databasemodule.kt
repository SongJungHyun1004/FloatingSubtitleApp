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
        Room.databaseBuilder(context, HistoryDatabase::class.java, "subtitle_history.db")
            // title 컬럼 추가로 스키마가 바뀌어서 버전을 올렸다. 정식 마이그레이션
            // 대신 기존 기록을 밀고 새로 시작하는 쪽으로 (사용자 확인받고 결정함).
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideHistoryDao(database: HistoryDatabase): HistoryDao = database.historyDao()
}