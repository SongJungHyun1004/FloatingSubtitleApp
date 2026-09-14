package com.joker.floatingsubtitleapp.data.history

import com.joker.floatingsubtitleapp.domain.model.RecordedLine
import com.joker.floatingsubtitleapp.domain.model.RecordedSessionSummary
import com.joker.floatingsubtitleapp.domain.model.SubtitleLine
import com.joker.floatingsubtitleapp.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val dao: HistoryDao
) : HistoryRepository {

    override val sessions: Flow<List<RecordedSessionSummary>> =
        dao.observeSessions().map { list ->
            list.map { RecordedSessionSummary(it.id, it.startedAt, it.sourceLang, it.targetLang, it.title) }
        }

    override suspend fun saveSession(
        startedAt: Long,
        sourceLang: String,
        targetLang: String,
        lines: List<SubtitleLine>
    ) {
        if (lines.isEmpty()) return

        val session = SessionEntity(startedAt = startedAt, sourceLang = sourceLang, targetLang = targetLang)
        val lineEntities = lines.mapIndexed { index, line ->
            SessionLineEntity(
                sessionId = 0, // HistoryDao.saveSession()이 실제 세션ID로 교체해서 넣는다
                orderIndex = index,
                originalText = line.originalText,
                translatedText = line.text
            )
        }
        dao.saveSession(session, lineEntities)
    }

    override suspend fun getLines(sessionId: Long): List<RecordedLine> =
        dao.getLines(sessionId).map { RecordedLine(it.originalText, it.translatedText) }

    override suspend fun renameSession(sessionId: Long, title: String) {
        dao.updateTitle(sessionId, title.ifBlank { null })
    }

    override suspend fun deleteSession(sessionId: Long) {
        dao.deleteSessionWithLines(sessionId)
    }
}