package com.joker.floatingsubtitleapp.presentation.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.joker.floatingsubtitleapp.domain.model.RecordedLine
import com.joker.floatingsubtitleapp.domain.model.RecordedSessionSummary
import java.io.File

fun copySessionToClipboard(
    context: Context,
    session: RecordedSessionSummary,
    lines: List<RecordedLine>
) {
    val text = SessionTextFormatter.format(session, lines)
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("자막 기록", text))
    Toast.makeText(context, "클립보드에 복사됐습니다", Toast.LENGTH_SHORT).show()
}

fun exportSessionAsFile(
    context: Context,
    session: RecordedSessionSummary,
    lines: List<RecordedLine>
) {
    val text = SessionTextFormatter.format(session, lines)
    val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(exportsDir, SessionTextFormatter.fileName(session))
    file.writeText(text)

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "자막 기록 내보내기"))
}