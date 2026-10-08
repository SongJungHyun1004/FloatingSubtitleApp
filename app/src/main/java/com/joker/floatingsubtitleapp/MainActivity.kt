package com.joker.floatingsubtitleapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.joker.floatingsubtitleapp.domain.model.RecordedSessionSummary
import com.joker.floatingsubtitleapp.domain.model.SubtitleFontSize
import com.joker.floatingsubtitleapp.presentation.history.HistoryScreen
import com.joker.floatingsubtitleapp.presentation.history.SessionDetailScreen
import com.joker.floatingsubtitleapp.presentation.main.MainViewModel
import com.joker.floatingsubtitleapp.presentation.service.SubtitleService
import com.joker.floatingsubtitleapp.presentation.settings.ModelDownloadStatus
import com.joker.floatingsubtitleapp.presentation.settings.SettingsViewModel
import com.joker.floatingsubtitleapp.presentation.settings.SttModelStatus
import com.joker.floatingsubtitleapp.presentation.settings.SupportedLanguage
import com.joker.floatingsubtitleapp.presentation.settings.SupportedLanguages
import com.joker.floatingsubtitleapp.ui.theme.FloatingSubtitleTheme
import dagger.hilt.android.AndroidEntryPoint

/** 네비게이션 라이브러리 없이, 이 앱 안에서만 쓰는 아주 단순한 화면 전환용 상태. */
private sealed interface AppScreen {
    data object Main : AppScreen
    data object History : AppScreen
    data class SessionDetail(val session: RecordedSessionSummary) : AppScreen
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // 캡처 승인 결과가 돌아올 때(비동기) 어떤 언어로 시작할지 알아야 해서
    // 버튼 클릭 시점의 선택값을 잠깐 들고 있는다.
    private var pendingSourceLang: String = "en"
    private var pendingTargetLang: String = "ko"

    // 1. Result Launcher 등록
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // 결과가 돌아왔을 때 다시 확인
        if (Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "오버레이 권한 승인됨", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "오버레이 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val intent = Intent(this, SubtitleService::class.java).apply {
                action = "START_CAPTURE"
                putExtra("RESULT_CODE", result.resultCode)
                putExtra("DATA", result.data)
                putExtra("SOURCE_LANG", pendingSourceLang)
                putExtra("TARGET_LANG", pendingTargetLang)
            }
            startForegroundService(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkInitialPermissions()

        setContent {
            var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Main) }

            FloatingSubtitleTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when (val screen = currentScreen) {
                        is AppScreen.Main -> MainScreen(
                            onStart = { sourceLang, targetLang ->
                                pendingSourceLang = sourceLang
                                pendingTargetLang = targetLang
                                startAudioCaptureWithPermission()
                            },
                            onStop = { stopService(Intent(this, SubtitleService::class.java)) },
                            onOpenHistory = { currentScreen = AppScreen.History }
                        )
                        is AppScreen.History -> {
                            BackHandler { currentScreen = AppScreen.Main }
                            HistoryScreen(
                                onBack = { currentScreen = AppScreen.Main },
                                onOpenSession = { session -> currentScreen = AppScreen.SessionDetail(session) }
                            )
                        }
                        is AppScreen.SessionDetail -> {
                            BackHandler { currentScreen = AppScreen.History }
                            SessionDetailScreen(
                                session = screen.session,
                                onBack = { currentScreen = AppScreen.History }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkInitialPermissions() {
        if (!Settings.canDrawOverlays(this)) {
            overlayPermissionLauncher.launch(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        }
        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissionsLauncher.launch(perms.toTypedArray())
    }

    private fun startAudioCaptureWithPermission() {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    // ───────────────────────────── 메인 화면 (v3: 번역앱 스타일, 브랜드 톤 적용) ─────────────────────────────

    @Composable
    fun MainScreen(
        onStart: (sourceLang: String, targetLang: String) -> Unit,
        onStop: () -> Unit,
        onOpenHistory: () -> Unit,
        settingsViewModel: SettingsViewModel = hiltViewModel(),
        mainViewModel: MainViewModel = hiltViewModel()
    ) {
        val uiState by settingsViewModel.uiState.collectAsState()
        val mainUiState by mainViewModel.uiState.collectAsState()
        val modelsReady = uiState.sourceStatus is SttModelStatus.Ready &&
                uiState.targetStatus == ModelDownloadStatus.READY

        // 서비스가 이미 실행 중이면 시작 버튼을 막는다 - 언어를 바꾼 채로
        // 다시 시작을 눌러서 이전 세션과 겹치는 걸 UI 단에서부터 방지한다.
        // 새 언어를 적용하려면 먼저 명시적으로 중지해야 한다.
        val startEnabled = modelsReady && !mainUiState.isServiceRunning

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "플로팅 자막",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(28.dp))

            LanguagePillBar(
                sourceLang = uiState.selected.sourceLang,
                targetLang = uiState.selected.targetLang,
                enabled = !mainUiState.isServiceRunning,
                onSelectSource = settingsViewModel::selectSourceLang,
                onSelectTarget = settingsViewModel::selectTargetLang,
                onSwap = settingsViewModel::swapLanguages
            )

            Spacer(modifier = Modifier.height(16.dp))

            StatusChip(
                sourceStatus = uiState.sourceStatus,
                targetStatus = uiState.targetStatus,
                isServiceRunning = mainUiState.isServiceRunning,
                languageChangedWhileRunning = mainUiState.languageChangedWhileRunning
            )

            Spacer(modifier = Modifier.height(40.dp))

            PlayStopButton(
                isRunning = mainUiState.isServiceRunning,
                enabled = startEnabled || mainUiState.isServiceRunning,
                onClick = {
                    if (mainUiState.isServiceRunning) {
                        onStop()
                    } else {
                        onStart(uiState.selected.sourceLang, uiState.selected.targetLang)
                    }
                }
            )

            Spacer(modifier = Modifier.height(40.dp))

            SettingsCard(
                showOriginalText = uiState.showOriginalText,
                onToggleShowOriginalText = settingsViewModel::toggleShowOriginalText,
                fontSize = uiState.fontSize,
                onSetFontSize = settingsViewModel::setFontSize,
                onOpenHistory = onOpenHistory
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    @Composable
    private fun LanguagePillBar(
        sourceLang: String,
        targetLang: String,
        enabled: Boolean,
        onSelectSource: (String) -> Unit,
        onSelectTarget: (String) -> Unit,
        onSwap: () -> Unit
    ) {
        // 상용 번역앱들은 언어 바를 "눌러보고 싶게" primary 톤으로 칠한다.
        // 기존엔 무채색 surfaceVariant라 버튼인지 텍스트인지 구분이 안 갔음.
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguagePillSegment(
                    modifier = Modifier.weight(1f),
                    selectedCode = sourceLang,
                    options = SupportedLanguages.sttSupported,
                    enabled = enabled,
                    onSelect = onSelectSource
                )

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp,
                    modifier = Modifier.size(36.dp)
                ) {
                    IconButton(onClick = onSwap, enabled = enabled, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Filled.SwapHoriz,
                            contentDescription = "언어 바꾸기",
                            tint = if (enabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                LanguagePillSegment(
                    modifier = Modifier.weight(1f),
                    selectedCode = targetLang,
                    options = SupportedLanguages.all,
                    enabled = enabled,
                    onSelect = onSelectTarget
                )
            }
        }
    }

    @Composable
    private fun LanguagePillSegment(
        modifier: Modifier = Modifier,
        selectedCode: String,
        options: List<SupportedLanguage>,
        enabled: Boolean,
        onSelect: (String) -> Unit
    ) {
        var expanded by remember { mutableStateOf(false) }

        Box(modifier = modifier) {
            // 텍스트만 덩그러니 있으면 눌리는 버튼인지 알기 어려워서,
            // 드롭다운 캐럿 아이콘을 붙여 "탭하면 바뀐다"는 걸 분명히 한다.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(enabled = enabled) { expanded = true }
                    .padding(vertical = 10.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = SupportedLanguages.displayNameOf(selectedCode),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .size(16.dp)
                )
            }

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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
    }

    private data class StatusInfo(val text: String, val color: Color, val showSpinner: Boolean)

    @Composable
    private fun StatusChip(
        sourceStatus: SttModelStatus,
        targetStatus: ModelDownloadStatus,
        isServiceRunning: Boolean,
        languageChangedWhileRunning: Boolean
    ) {
        val status = when {
            languageChangedWhileRunning ->
                StatusInfo("언어가 바뀌었어요 - 재시작하면 적용됩니다", MaterialTheme.colorScheme.error, false)
            isServiceRunning ->
                StatusInfo("자막 서비스 실행 중", MaterialTheme.colorScheme.primary, false)
            sourceStatus is SttModelStatus.Failed ->
                StatusInfo("음성인식 모델 오류: ${sourceStatus.message}", MaterialTheme.colorScheme.error, false)
            targetStatus == ModelDownloadStatus.FAILED ->
                StatusInfo("번역 모델 다운로드 실패", MaterialTheme.colorScheme.error, false)
            sourceStatus is SttModelStatus.Downloading || targetStatus == ModelDownloadStatus.DOWNLOADING ->
                StatusInfo("언어 모델 준비 중...", MaterialTheme.colorScheme.onSurfaceVariant, true)
            sourceStatus is SttModelStatus.Ready && targetStatus == ModelDownloadStatus.READY ->
                StatusInfo("언어 모델 준비 완료", MaterialTheme.colorScheme.onSecondaryContainer, false)
            else -> null
        } ?: return

        // 글자색만으로 상태를 표현하던 걸, 작은 점(또는 스피너) + 텍스트 조합으로 바꿔서
        // 한눈에 스캔 가능한 "상태 칩" 느낌을 낸다.
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (status.showSpinner) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                    color = status.color
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(status.color)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = status.text, style = MaterialTheme.typography.bodySmall, color = status.color)
        }
    }

    @Composable
    private fun PlayStopButton(
        isRunning: Boolean,
        enabled: Boolean,
        onClick: () -> Unit
    ) {
        val containerColor = when {
            !enabled -> MaterialTheme.colorScheme.surfaceVariant
            isRunning -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.primary
        }
        val contentColor = when {
            !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
            isRunning -> MaterialTheme.colorScheme.onErrorContainer
            else -> MaterialTheme.colorScheme.onPrimary
        }
        val ringColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

        // 버튼 하나만 너무 밋밋하게 떠 있지 않도록 얇은 외곽 링 + 그림자를 줘서
        // 화면에서 가장 중요한 동작이라는 위계를 시각적으로도 드러낸다.
        Box(contentAlignment = Alignment.Center) {
            if (enabled) {
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, ringColor.copy(alpha = 0.25f)),
                    modifier = Modifier.size(112.dp)
                ) {}
            }

            Surface(
                shape = CircleShape,
                color = containerColor,
                shadowElevation = if (enabled) 6.dp else 0.dp,
                modifier = Modifier
                    .size(92.dp)
                    .clickable(enabled = enabled, onClick = onClick)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = if (isRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = if (isRunning) "자막 서비스 중지" else "자막 서비스 시작",
                        tint = contentColor,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }

    @Composable
    private fun SettingsCard(
        showOriginalText: Boolean,
        onToggleShowOriginalText: () -> Unit,
        fontSize: SubtitleFontSize,
        onSetFontSize: (SubtitleFontSize) -> Unit,
        onOpenHistory: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                // 상용 앱 설정 화면은 각 행 앞에 작은 아이콘을 둬서 글자만 쭉 나열된
                // 느낌을 없앤다 - 라벨만 있던 이전 버전과 가장 크게 다른 부분.
                SettingsRow(label = "원문 같이 보기", icon = Icons.Filled.Subtitles) {
                    Switch(
                        checked = showOriginalText,
                        onCheckedChange = { onToggleShowOriginalText() },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                SettingsRow(label = "글자 크기", icon = Icons.Filled.TextFields) {
                    FontSizeSegmentedControl(selected = fontSize, onSelect = onSetFontSize)
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                SettingsRow(
                    label = "자막 기록",
                    icon = Icons.Filled.History,
                    modifier = Modifier.clickable(onClick = onOpenHistory)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "자막 기록 보기",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    /**
     * 소/중/대를 각각 FilterChip으로 늘어놓던 걸, 하나의 트랙 안에서 선택된 칸만
     * 떠 보이게 하는 세그먼트 컨트롤로 바꿨다 (iOS/상용앱에서 흔히 보는 글자 크기 선택 UI).
     */
    @Composable
    private fun FontSizeSegmentedControl(
        selected: SubtitleFontSize,
        onSelect: (SubtitleFontSize) -> Unit
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(modifier = Modifier.padding(3.dp)) {
                SubtitleFontSize.entries.forEach { size ->
                    val isSelected = selected == size
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shadowElevation = if (isSelected) 1.dp else 0.dp,
                        modifier = Modifier.clickable { onSelect(size) }
                    ) {
                        Text(
                            text = size.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun SettingsRow(
        label: String,
        icon: ImageVector,
        modifier: Modifier = Modifier,
        trailing: @Composable () -> Unit
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            trailing()
        }
    }
}