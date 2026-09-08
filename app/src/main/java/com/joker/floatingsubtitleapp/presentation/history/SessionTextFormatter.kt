package com.joker.floatingsubtitleapp.presentation.history

import com.joker.floatingsubtitleapp.domain.model.RecordedLine
import com.joker.floatingsubtitleapp.domain.model.RecordedSessionSummary
import com.joker.floatingsubtitleapp.presentation.settings.SupportedLanguages
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SessionTextFormatter {

    fun format(session: RecordedSessionSummary, lines: List<RecordedLine>): String {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(Date(session.startedAt))
        val sourceLangName = SupportedLanguages.displayNameOf(session.sourceLang)
        val targetLangName = SupportedLanguages.displayNameOf(session.targetLang)

        val header = "[$dateStr] $sourceLangName → $targetLangName\n\n"
        val body = lines.joinToString("\n\n") { "${it.translatedText}\n${it.originalText}" }
        return header + body
    }

    fun fileName(session: RecordedSessionSummary): String {
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(Date(session.startedAt))
        return "subtitle_$dateStr.txt"
    }
}