package com.joker.floatingsubtitleapp.data.stt

import android.util.Log
import com.joker.floatingsubtitleapp.domain.model.SpeechResult
import com.joker.floatingsubtitleapp.domain.repository.SpeechRecognitionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.vosk.Model
import org.vosk.Recognizer
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoskSpeechEngine @Inject constructor(
    private val modelManager: VoskModelManager
) : SpeechRecognitionRepository {

    private val TAG = "VoskSpeechEngine"
    private val sampleRate = 16000.0f

    // 모델 로딩(다운로드/디스크IO/네이티브 초기화)이 동시에 겹치지 않도록 하는 락
    private val modelLoadLock = Mutex()

    // 언어별로 한 번 로드한 Model을 캐싱한다. 언어를 왔다갔다 바꿔도
    // 매번 처음부터 다시 로드하지 않기 위함이다. 앱이 살아있는 동안 유지된다.
    private val loadedModels = mutableMapOf<String, Model>()

    private suspend fun getOrLoadModel(langCode: String): Model {
        modelLoadLock.withLock {
            Log.d(TAG, "🔍 getOrLoadModel 요청: langCode=$langCode, 현재 캐시=${loadedModels.keys}")

            loadedModels[langCode]?.let {
                Log.d(TAG, "🔍 캐시 히트: langCode=$langCode, model=${System.identityHashCode(it)}")
                return it
            }

            var finalPath: String? = null
            modelManager.prepareModel(langCode).collect { progress ->
                if (progress is ModelPrepareProgress.Done) {
                    finalPath = progress.modelPath
                }
                // InProgress는 여기서는 무시한다 - 다운로드 진행률 UI는
                // SettingsViewModel이 VoskModelManager를 직접 구독해서 따로 보여준다.
            }
            val path = finalPath ?: error("모델 경로를 가져오지 못했습니다 ($langCode)")
            Log.d(TAG, "🔍 캐시 미스, 새로 로드: langCode=$langCode, path=$path")

            val model = Model(path)
            loadedModels[langCode] = model
            Log.d(TAG, "🔍 로드 완료: langCode=$langCode, model=${System.identityHashCode(model)}")
            return model
        }
    }

    override suspend fun initEngine(sourceLang: String): Result<Unit> = runCatching {
        getOrLoadModel(sourceLang)
        Unit
    }

    override fun recognize(audioData: Flow<ShortArray>, sourceLang: String): Flow<SpeechResult> = callbackFlow {
        Log.d(TAG, "🔍 recognize() 호출됨: sourceLang=$sourceLang")

        val model = try {
            getOrLoadModel(sourceLang)
        } catch (e: Exception) {
            close(IllegalStateException("Vosk 모델 로드 실패($sourceLang): ${e.message}", e))
            return@callbackFlow
        }

        Log.d(TAG, "🔍 Recognizer 생성: sourceLang=$sourceLang, model=${System.identityHashCode(model)}")
        val recognizer = Recognizer(model, sampleRate)

        // 문장이 덜 완성된 상태에서 재번역되며 생기던 심한 반복 문제 완화를 위한 조정.
        // (vosk-android 0.3.50+ 필요, 0.3.75에서 정상 동작 확인됨)
        runCatching {
            recognizer.setEndpointerDelays(5.0f, 0.2f, 20.0f)
        }.onFailure { e ->
            Log.w(TAG, "setEndpointerDelays 적용 실패: ${e.message}", e)
        }

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
            // job.cancel()은 취소를 "요청"만 할 뿐, acceptWaveForm() 같은 네이티브
            // 호출이 이미 실행 중이면 그게 끝날 때까지 즉시 멈추지 않는다.
            // 그 상태에서 recognizer.close()를 바로 부르면 다른 스레드가 아직
            // 쓰고 있는 네이티브 메모리를 해제해버려 SIGSEGV가 날 수 있다.
            // invokeOnCompletion으로 "이 job이 실제로 완전히 끝난 뒤"에만
            // close()가 실행되도록 순서를 강제한다.
            job.invokeOnCompletion {
                runCatching { recognizer.close() }.onFailure { e ->
                    Log.w(TAG, "recognizer.close() 실패: ${e.message}", e)
                }
            }
            job.cancel()
        }
    }
}