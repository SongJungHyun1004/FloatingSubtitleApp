package com.joker.floatingsubtitleapp.domain.repository

import com.joker.floatingsubtitleapp.domain.model.RecordedLine
import com.joker.floatingsubtitleapp.domain.model.RecordedSessionSummary
import com.joker.floatingsubtitleapp.domain.model.SubtitleLine
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    /** 최신순으로 정렬된 세션 목록. 새로 저장/삭제되면 자동으로 갱신된다. */
    val sessions: Flow<List<RecordedSessionSummary>>

    /** lines가 비어있으면(말 한마디 없이 끝난 세션) 아무것도 저장하지 않는다. */
    suspend fun saveSession(
        startedAt: Long,
        sourceLang: String,
        targetLang: String,
        lines: List<SubtitleLine>
    )

    suspend fun getLines(sessionId: Long): List<RecordedLine>

    suspend fun renameSession(sessionId: Long, title: String)

    suspend fun deleteSession(sessionId: Long)
}