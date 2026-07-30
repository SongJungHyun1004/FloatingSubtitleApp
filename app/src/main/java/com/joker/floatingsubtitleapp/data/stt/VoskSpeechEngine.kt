package com.joker.floatingsubtitleapp.data.stt

import android.content.Context
import android.util.Log
import com.joker.floatingsubtitleapp.domain.model.SpeechResult
import com.joker.floatingsubtitleapp.domain.repository.SpeechRecognitionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock
import org.vosk.Model
import org.vosk.Recognizer
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.text.isNotBlank

@Singleton
class VoskSpeechEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelManager: VoskModelManager
) : SpeechRecognitionRepository {

    private var model: Model? = null
    private val sampleRate = 16000.0f
    // 다중 스레드에서 동시에 초기화되는 것을 방지하는 Mutex
    private val mutex = Mutex()

    override suspend fun initEngine(): Result<Unit> = withContext(Dispatchers.IO) {
        // Mutex를 통해 한 번에 하나의 코루틴만 초기화 블록에 진입하도록 보장
        mutex.withLock {
            runCatching {
                if (model == null) {
                    val basePath = modelManager.prepareModel()

                    // 실제 모델 파일이 하위 "model" 폴더에 있는지 확인하고 경로 보정
                    val actualPath = java.io.File(basePath, "model").takeIf { it.exists() }?.absolutePath ?: basePath

                    model = Model(actualPath)
                }
            }
        }
    }

    override fun recognize(audioData: Flow<ShortArray>): Flow<SpeechResult> = callbackFlow {
        if (model == null) {
            val initResult = initEngine()
            if (initResult.isFailure) {
                close(IllegalStateException("Vosk 모델 초기화 실패", initResult.exceptionOrNull()))
                return@callbackFlow
            }
        }
        val currentModel = model
        if (currentModel == null) {
            close(IllegalStateException("Vosk 모델이 초기화되지 않았습니다. initEngine()을 먼저 호출해주세요."))
            return@callbackFlow
        }

        val recognizer = Recognizer(currentModel, sampleRate)

        // callbackFlow의 Scope를 사용하여 launch 실행
        val job = launch(Dispatchers.Default) {
            audioData.collect { buffer ->
                if (recognizer.acceptWaveForm(buffer, buffer.size)) {
                    val resultJson = recognizer.result
                    val text = JSONObject(resultJson).optString("text", "")
                    Log.d("VOSK_FINAL_RAW", text)
                    if (text.isNotBlank()) {
                        trySend(SpeechResult.Final(text))
                    }
                } else {
                    val partialJson = recognizer.partialResult
                    val partial = JSONObject(partialJson).optString("partial", "")
                    if (partial.isNotBlank()) {
                        trySend(SpeechResult.Partial(partial))
                    }
                }
            }
        }

        awaitClose {
            job.cancel()
            recognizer.close()
        }
    }
}