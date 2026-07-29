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
                val resultCode = intent.getIntExtra("RESULT_CODE", Activity.RESULT_CANCELED) // 기본값 RESULT_CANCELED(0)
                val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra("DATA", Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Intent>("DATA")
                }

                // ⭕ 수정: RESULT_OK (-1) 인지 정확히 검사
                if (resultCode == Activity.RESULT_OK && data != null) {
                    startSubtitlePipeline(resultCode, data)
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
    private fun startSubtitlePipeline(resultCode: Int, data: Intent) {
        Log.d(TAG, "startSubtitlePipeline 시작")
        captureJob?.cancel()
        overlayController.show()

        captureJob = serviceScope.launch(Dispatchers.IO) {
            try {
                getSubtitleFlowUseCase(
                    resultCode = resultCode,
                    data = data,
                    sourceLang = "en",
                    targetLang = "ko"
                ).collectLatest { state ->
                    Log.d(TAG, "수신된 자막: ${state.finalizedLines}, partial=${state.partialText}")

                    withContext(Dispatchers.Main) {
                        overlayController.updateText(state)
                    }
                }
            } catch (e: Exception) {
                // 3. captureJob?.cancel() 호출 시 발생하는 취소 예외는 정상 처리
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