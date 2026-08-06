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

    private val TAG = "VoskSpeechEngine"

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

        // Vosk의 기본 무음 임계값(대략 0.5~1.0초)이 빠른 대사(쉼이 짧은 구간)에서는
        // 문장을 안 끊고 계속 이어붙이는 문제가 있어서 좀 더 예민하게 조정한다.
        // (vosk-android 0.3.50+ 부터 지원되는 API. 0.3.47에는 없어서 0.3.75로 업그레이드 후 사용)
        // 단위는 초(second)다.
        //  - startSilenceMax(5.0s): 말이 시작되기 전 허용하는 무음 시간. 그대로 둔다.
        //  - endSilence(0.3s): "뭔가 인식된 뒤" 이만큼 조용하면 문장이 끝났다고 판단.
        //    기본값보다 낮춰서 짧은 쉼에도 좀 더 잘 끊기게 한다.
        //    단, 너무 낮추면 한 문장 안의 자연스러운 쉼(쉼표 등)까지 끊어버릴 수 있으니
        //    실제 사용해보면서 0.2~0.5 사이에서 값을 조정해봐야 한다.
        //  - maxUtteranceLength(20.0s): 아무리 안 끊겨도 이 시간이 지나면 강제로 끊는다.
        runCatching {
            recognizer.setEndpointerDelays(5.0f, 0.2f, 20.0f)
        }.onFailure { e ->
            Log.w(TAG, "setEndpointerDelays 적용 실패: ${e.message}", e)
        }

        // callbackFlow의 Scope를 사용하여 launch 실행
        val job = launch(Dispatchers.Default) {
            audioData.collect { buffer ->
                if (recognizer.acceptWaveForm(buffer, buffer.size)) {
                    val resultJson = recognizer.result
                    val text = JSONObject(resultJson).optString("text", "")
                    Log.d("VOSK_FINAL_RAW", text)
                    if (text.isNotBlank()) {
                        val sendResult = trySend(SpeechResult.Final(text))
                        if (sendResult.isFailure) {
                            Log.w(TAG, "⚠️ Final 전송 실패 - 채널 backpressure로 유실 가능성: [$text]")
                        }
                    }
                } else {
                    val partialJson = recognizer.partialResult
                    val partial = JSONObject(partialJson).optString("partial", "")
                    if (partial.isNotBlank()) {
                        val sendResult = trySend(SpeechResult.Partial(partial))
                        if (sendResult.isFailure) {
                            Log.d(TAG, "Partial 전송 실패 (허용 가능한 수준의 유실): [$partial]")
                        }
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