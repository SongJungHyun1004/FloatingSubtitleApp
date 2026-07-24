package com.joker.floatingsubtitleapp.data.stt

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.vosk.android.StorageService
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class VoskModelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun prepareModel(): String = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            StorageService.unpack(
                context,
                "model",
                "vosk-model",
                { model ->
                    if (continuation.isActive) {
                        // 모델 경로 문자열 반환
                        continuation.resume("${context.getExternalFilesDir(null)}/vosk-model")
                    }
                },
                { exception ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(exception)
                    }
                }
            )
        }
    }
}