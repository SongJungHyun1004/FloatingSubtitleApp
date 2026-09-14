package com.joker.floatingsubtitleapp.presentation.history

import com.joker.floatingsubtitleapp.domain.model.RecordedLine
import com.joker.floatingsubtitleapp.domain.model.RecordedSessionSummary
import com.joker.floatingsubtitleapp.presentation.settings.SupportedLanguages
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SessionTextFormatter {

    /** 사용자가 이름을 안 바꿨으면 "원본언어 → 대상언어" 기본 제목을 만든다. */
    fun displayTitle(session: RecordedSessionSummary): String =
        session.title?.takeIf { it.isNotBlank() }
            ?: "${SupportedLanguages.displayNameOf(session.sourceLang)} → ${SupportedLanguages.displayNameOf(session.targetLang)}"

    fun format(session: RecordedSessionSummary, lines: List<RecordedLine>): String {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(Date(session.startedAt))

        val header = "[$dateStr] ${displayTitle(session)}\n\n"
        val body = lines.joinToString("\n\n") { "${it.translatedText}\n${it.originalText}" }
        return header + body
    }

    fun fileName(session: RecordedSessionSummary): String {
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(Date(session.startedAt))
        return "subtitle_$dateStr.txt"
    }
}