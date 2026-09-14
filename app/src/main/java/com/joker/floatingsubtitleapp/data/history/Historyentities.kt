package com.joker.floatingsubtitleapp.data.history

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val sourceLang: String,
    val targetLang: String,
    /** null이면 화면에서 "원본언어 → 대상언어" 기본 제목으로 표시한다. */
    val title: String? = null
)

@Entity(tableName = "session_lines")
data class SessionLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val orderIndex: Int,
    val originalText: String,
    val translatedText: String
)