package com.joker.floatingsubtitleapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import android.media.projection.MediaProjectionManager
import android.content.Context
import com.joker.floatingsubtitleapp.presentation.service.SubtitleService

class MainActivity : ComponentActivity() {

    // 1. Result Launcher 등록
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // 결과가 돌아왔을 때 다시 확인
        if (Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "권한이 승인되었습니다.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "오버레이 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            // Phase 7에서 이 데이터를 서비스로 전달하여 캡처를 시작할 예정입니다.
            val intent = Intent(this, SubtitleService::class.java).apply {
                action = "START_CAPTURE"
                putExtra("RESULT_CODE", result.resultCode)
                putExtra("DATA", result.data)
            }
            startForegroundService(intent)
        }
    }

    private fun startAudioCaptureWithPermission() {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkOverlayPermission()

        setContent {
            // UI 초기화 코드 (Phase 8에서 진행)
        }
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            // 2. launcher를 통해 실행
            overlayPermissionLauncher.launch(intent)
        }
    }
}