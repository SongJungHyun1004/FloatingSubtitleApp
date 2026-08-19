package com.joker.floatingsubtitleapp.presentation.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.joker.floatingsubtitleapp.domain.usecase.GetSubtitleFlowUseCase
import com.joker.floatingsubtitleapp.presentation.overlay.OverlayController
import com.joker.floatingsubtitleapp.presentation.overlay.SubtitleLineManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class SubtitleService : Service() {
    @Inject
    lateinit var overlayController: OverlayController

    @Inject
    lateinit var getSubtitleFlowUseCase: GetSubtitleFlowUseCase

    @Inject
    lateinit var subtitleLineManager: SubtitleLineManager

    @Inject
    lateinit var serviceStateHolder: ServiceStateHolder

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var captureJob: Job? = null

    companion object {
        private const val TAG = "SubtitleService"
        private const val CHANNEL_ID = "subtitle_service_channel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate() 호출됨")
        createNotificationChannel()
        startForegroundWithNotification()
    }

    private fun startForegroundWithNotification() {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            } else {
                0
            }
            startForeground(NOTIFICATION_ID, notification, foregroundType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand action: ${intent?.action}")
        when (intent?.action) {
            "START_CAPTURE" -> {
                val resultCode = intent.getIntExtra("RESULT_CODE", Activity.RESULT_CANCELED)
                val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra("DATA", Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Intent>("DATA")
                }

                // MainActivity에서 사용자가 고른 언어를 extra로 전달받는다.
                // 값이 없으면(예: 예전 버전 Intent) en/ko로 안전하게 폴백한다.
                val sourceLang = intent.getStringExtra("SOURCE_LANG") ?: "en"
                val targetLang = intent.getStringExtra("TARGET_LANG") ?: "ko"

                if (resultCode == Activity.RESULT_OK && data != null) {
                    startSubtitlePipeline(resultCode, data, sourceLang, targetLang)
                } else {
                    Log.e(TAG, "MediaProjection Intent data 또는 resultCode가 유효하지 않습니다. (resultCode: $resultCode, data: $data)")
                }
            }
            "STOP_CAPTURE" -> {
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startSubtitlePipeline(
        resultCode: Int,
        data: Intent,
        sourceLang: String,
        targetLang: String
    ) {
        Log.d(TAG, "startSubtitlePipeline 시작 ($sourceLang -> $targetLang)")
        val previousJob = captureJob
        subtitleLineManager.clear()
        overlayController.show()
        serviceStateHolder.setRunning(sourceLang, targetLang)

        captureJob = serviceScope.launch(Dispatchers.IO) {
            // cancel()만 하고 완료를 기다리지 않으면, 이전 세션(이전 언어의
            // 오디오 캡처 + Vosk 인식)과 새 세션이 아주 짧은 순간 동시에
            // 살아있는 경쟁 상태가 생긴다. cancelAndJoin()으로 이전 세션이
            // 실제로 완전히 끝난 뒤에만 새 세션을 시작하도록 강제한다.
            previousJob?.cancelAndJoin()

            try {
                getSubtitleFlowUseCase(
                    resultCode = resultCode,
                    data = data,
                    sourceLang = sourceLang,
                    targetLang = targetLang
                ).collect { event ->
                    // collectLatest가 아니라 collect를 쓴다: onEvent 처리는
                    // 즉시 끝나는 가벼운 작업이라 굳이 이전 이벤트 처리를 취소할
                    // 이유가 없고, "번역 누락 없음" 요구사항과도 맞지 않는다.
                    Log.d(TAG, "수신된 이벤트: $event")
                    subtitleLineManager.onEvent(event)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Subtitle pipeline 에러 발생: ${e.message}", e)
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy() 호출됨")
        captureJob?.cancel()
        getSubtitleFlowUseCase.stop()
        overlayController.hide()
        subtitleLineManager.clear()
        serviceStateHolder.setStopped()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Floating Subtitle")
            .setContentText("Subtitle service is running...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
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
            manager?.createNotificationChannel(serviceChannel)
        }
    }
}