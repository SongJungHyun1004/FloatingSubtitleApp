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

@AndroidEntryPoint
class SubtitleService : Service() {
    @Inject
    lateinit var overlayController: OverlayController

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
        if (intent?.action == "START_CAPTURE") {
            overlayController.show()
            // 여기서 나중에 오디오 캡처 파이프라인을 연결합니다.
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayController.hide()
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