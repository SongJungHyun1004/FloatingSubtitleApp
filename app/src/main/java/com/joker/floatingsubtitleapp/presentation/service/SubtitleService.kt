package com.joker.floatingsubtitleapp.presentation.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import com.joker.floatingsubtitleapp.presentation.overlay.OverlayController
import javax.inject.Inject
import com.joker.floatingsubtitleapp.domain.usecase.GetSubtitleFlowUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class SubtitleService : Service() {
    @Inject
    lateinit var overlayController: OverlayController

    @Inject
    lateinit var getSubtitleFlowUseCase: GetSubtitleFlowUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var captureJob: Job? = null

    companion object {
        private const val CHANNEL_ID = "subtitle_service_channel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_CAPTURE" -> {
                val resultCode = intent.getIntExtra("RESULT_CODE", -1)
                val data = intent.getParcelableExtra<Intent>("DATA")

                if (resultCode != -1 && data != null) {
                    startSubtitlePipeline(resultCode, data)
                }
            }
            "STOP_CAPTURE" -> {
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startSubtitlePipeline(resultCode: Int, data: Intent) {
        captureJob?.cancel() // 기존 작업이 있다면 취소
        overlayController.show()

        captureJob = serviceScope.launch {
            // 엔진 초기화 (Vosk 모델 등 - Phase 4에서 정의한 init)
            // 실제 앱에서는 여기서 언어 모델 다운로드 여부도 체크해야 함

            getSubtitleFlowUseCase(
                resultCode = resultCode,
                data = data,
                sourceLang = "en", // 임시 하드코딩 (Phase 8에서 설정화면 연결)
                targetLang = "ko"
            ).collectLatest { translatedText ->
                overlayController.updateText(translatedText)
            }
        }
    }

    override fun onDestroy() {
        captureJob?.cancel()
        getSubtitleFlowUseCase.stop()
        overlayController.hide()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Floating Subtitle")
            .setContentText("Subtitle service is running...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now) // 임시 아이콘
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Subtitle Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }


}