package com.joker.floatingsubtitleapp.presentation.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.joker.floatingsubtitleapp.data.stt.VoskModels

@Composable
fun LanguageSelectionSection(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        LanguageDropdown(
            label = "원본 언어 (듣는 언어)",
            selectedCode = uiState.selected.sourceLang,
            options = SupportedLanguages.sttSupported,
            statusLabel = sttStatusLabel(uiState.sourceStatus),
            onSelect = viewModel::selectSourceLang
        )
        Column(modifier = Modifier.padding(top = 12.dp)) {
            LanguageDropdown(
                label = "번역 언어 (보여줄 언어)",
                selectedCode = uiState.selected.targetLang,
                options = SupportedLanguages.all,
                statusLabel = statusLabel(uiState.targetStatus),
                onSelect = viewModel::selectTargetLang
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("원문 같이 보기", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = uiState.showOriginalText,
                onCheckedChange = { viewModel.toggleShowOriginalText() }
            )
        }
    }

    val pendingCode = uiState.pendingCellularConfirmLangCode
    if (pendingCode != null) {
        val info = VoskModels.infoFor(pendingCode)
        AlertDialog(
            onDismissRequest = viewModel::cancelCellularDownload,
            title = { Text("이동통신 데이터로 다운로드") },
            text = {
                Text(
                    "${SupportedLanguages.displayNameOf(pendingCode)} 음성인식 모델은 " +
                            "약 ${info?.approxSizeMb ?: "?"}MB입니다. Wi-Fi가 아닌 상태인데 " +
                            "계속 받으시겠어요?"
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmCellularDownload) { Text("계속") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelCellularDownload) { Text("취소") }
            }
        )
    }
}

@Composable
private fun LanguageDropdown(
    label: String,
    selectedCode: String,
    options: List<SupportedLanguage>,
    statusLabel: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)

        Box(modifier = Modifier.padding(top = 4.dp)) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(SupportedLanguages.displayNameOf(selectedCode))
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { lang ->
                    DropdownMenuItem(
                        text = { Text(lang.displayName) },
                        onClick = {
                            onSelect(lang.code)
                            expanded = false
                        }
                    )
                }
            }
        }

        Text(
            text = statusLabel,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

private fun statusLabel(status: ModelDownloadStatus): String = when (status) {
    ModelDownloadStatus.IDLE -> ""
    ModelDownloadStatus.DOWNLOADING -> "⏳ 모델 다운로드 중..."
    ModelDownloadStatus.READY -> "✅ 사용 가능"
    ModelDownloadStatus.FAILED -> "⚠️ 다운로드 실패 - 다시 선택하면 재시도합니다"
}

private fun sttStatusLabel(status: SttModelStatus): String = when (status) {
    is SttModelStatus.Idle -> ""
    is SttModelStatus.Downloading ->
        if (status.fraction >= 0f) "⏳ 다운로드 중... ${(status.fraction * 100).toInt()}%"
        else "⏳ 처리 중..."
    is SttModelStatus.Ready -> "✅ 사용 가능"
    is SttModelStatus.Failed -> "⚠️ 실패: ${status.message}"
}