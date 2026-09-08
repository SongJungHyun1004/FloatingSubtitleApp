package com.joker.floatingsubtitleapp.presentation.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.joker.floatingsubtitleapp.domain.model.RecordedLine
import com.joker.floatingsubtitleapp.domain.model.RecordedSessionSummary

@Composable
fun SessionDetailScreen(
    session: RecordedSessionSummary,
    onBack: () -> Unit,
    viewModel: SessionDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lines by viewModel.lines.collectAsState()

    LaunchedEffect(session.id) { viewModel.load(session.id) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack) { Text("← 뒤로") }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            OutlinedButton(
                onClick = { copySessionToClipboard(context, session, lines) },
                modifier = Modifier.weight(1f)
            ) { Text("복사") }
            OutlinedButton(
                onClick = { exportSessionAsFile(context, session, lines) },
                modifier = Modifier.weight(1f)
            ) { Text("내보내기") }
        }

        if (lines.isEmpty()) {
            Text(
                "불러오는 중...",
                modifier = Modifier.padding(top = 24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(lines) { line -> SessionLineRow(line) }
            }
        }
    }
}

@Composable
private fun SessionLineRow(line: RecordedLine) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(line.translatedText, style = MaterialTheme.typography.bodyLarge)
        Text(
            line.originalText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}