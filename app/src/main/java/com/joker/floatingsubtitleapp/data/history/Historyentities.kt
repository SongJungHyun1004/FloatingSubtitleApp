package com.joker.floatingsubtitleapp.data.history

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val sourceLang: String,
    val targetLang: String
)

@Entity(tableName = "session_lines")
data class SessionLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val orderIndex: Int,
    val originalText: String,
    val translatedText: String
)