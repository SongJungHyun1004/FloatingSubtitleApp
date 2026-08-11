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
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.joker.floatingsubtitleapp.presentation.service.SubtitleService
import com.joker.floatingsubtitleapp.presentation.settings.LanguageSelectionSection
import com.joker.floatingsubtitleapp.presentation.settings.ModelDownloadStatus
import com.joker.floatingsubtitleapp.presentation.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

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
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        onStart = { sourceLang, targetLang ->
                            pendingSourceLang = sourceLang
                            pendingTargetLang = targetLang
                            startAudioCaptureWithPermission()
                        },
                        onStop = { stopService(Intent(this, SubtitleService::class.java)) }
                    )
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

    @Composable
    fun MainScreen(
        onStart: (sourceLang: String, targetLang: String) -> Unit,
        onStop: () -> Unit,
        settingsViewModel: SettingsViewModel = hiltViewModel()
    ) {
        val uiState by settingsViewModel.uiState.collectAsState()
        val modelsReady = uiState.sourceStatus is com.joker.floatingsubtitleapp.presentation.settings.SttModelStatus.Ready &&
                uiState.targetStatus == ModelDownloadStatus.READY

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Floating Subtitle Settings", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))

            LanguageSelectionSection(viewModel = settingsViewModel)

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onStart(uiState.selected.sourceLang, uiState.selected.targetLang) },
                enabled = modelsReady,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (modelsReady) "자막 서비스 시작" else "언어 모델 준비 중...")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                Text("자막 서비스 중지")
            }
        }
    }
}