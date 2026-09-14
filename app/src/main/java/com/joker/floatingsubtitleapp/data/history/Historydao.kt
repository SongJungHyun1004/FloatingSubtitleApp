package com.joker.floatingsubtitleapp.data.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Insert
    suspend fun insertLines(lines: List<SessionLineEntity>)

    /** 저장할 자막이 하나도 없는 세션(중지만 누르고 아무 말도 없었던 경우)은
     *  기록에 안 남기려고, 세션+줄 저장을 하나로 묶어서 원자적으로 처리한다. */
    @Transaction
    suspend fun saveSession(session: SessionEntity, lines: List<SessionLineEntity>) {
        if (lines.isEmpty()) return
        val sessionId = insertSession(session)
        insertLines(lines.map { it.copy(sessionId = sessionId) })
    }

    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun observeSessions(): Flow<List<SessionEntity>>

    @Query("UPDATE sessions SET title = :title WHERE id = :sessionId")
    suspend fun updateTitle(sessionId: Long, title: String?)

    @Query("SELECT * FROM session_lines WHERE sessionId = :sessionId ORDER BY orderIndex")
    suspend fun getLines(sessionId: Long): List<SessionLineEntity>

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query("DELETE FROM session_lines WHERE sessionId = :sessionId")
    suspend fun deleteLines(sessionId: Long)

    @Transaction
    suspend fun deleteSessionWithLines(sessionId: Long) {
        deleteLines(sessionId)
        deleteSession(sessionId)
    }
}