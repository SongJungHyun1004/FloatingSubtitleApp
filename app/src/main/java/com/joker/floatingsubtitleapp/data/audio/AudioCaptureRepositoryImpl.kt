package com.joker.floatingsubtitleapp.data.audio

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjectionManager
import com.joker.floatingsubtitleapp.domain.repository.AudioCaptureRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import javax.inject.Inject

class AudioCaptureRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AudioCaptureRepository {

    private var audioRecord: AudioRecord? = null
    private val sampleRate = 16000 // Vosk 권장 샘플 레이트
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    @SuppressLint("MissingPermission")
    override fun startCapture(resultCode: Int, data: Intent): Flow<ShortArray> = flow {
        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        // mediaProjection을 가져오고 null 체크를 수행합니다.
        val mediaProjection = projectionManager.getMediaProjection(resultCode, data)
            ?: throw IllegalStateException("MediaProjection is null. 권한이 거부되었거나 초기화에 실패했습니다.")

        // 이제 mediaProjection은 null이 아님이 보장됩니다.
        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .build()

        audioRecord = AudioRecord.Builder()
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSize)
            .setAudioPlaybackCaptureConfig(config)
            .build()

        audioRecord?.startRecording()

        val buffer = ShortArray(bufferSize / 2)
        while (currentCoroutineContext().isActive) {
            val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: 0
            if (readBytes > 0) {
                emit(buffer.copyOf(readBytes))
            }
        }
    }.flowOn(Dispatchers.IO)

    override fun stopCapture() {
        audioRecord?.apply {
            stop()
            release()
        }
        audioRecord = null
    }
}