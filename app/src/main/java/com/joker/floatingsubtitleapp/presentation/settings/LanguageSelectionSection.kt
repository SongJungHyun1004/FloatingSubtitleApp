package com.joker.floatingsubtitleapp.presentation.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LanguageSelectionSection(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        LanguageDropdown(
            label = "원본 언어 (듣는 언어)",
            selectedCode = uiState.selected.sourceLang,
            status = uiState.sourceStatus,
            onSelect = viewModel::selectSourceLang
        )
        Column(modifier = Modifier.padding(top = 12.dp)) {
            LanguageDropdown(
                label = "번역 언어 (보여줄 언어)",
                selectedCode = uiState.selected.targetLang,
                status = uiState.targetStatus,
                onSelect = viewModel::selectTargetLang
            )
        }
    }
}

@Composable
private fun LanguageDropdown(
    label: String,
    selectedCode: String,
    status: ModelDownloadStatus,
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
                SupportedLanguages.all.forEach { lang ->
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
            text = statusLabel(status),
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