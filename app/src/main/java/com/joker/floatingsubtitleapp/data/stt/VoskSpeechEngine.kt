package com.joker.floatingsubtitleapp.data.stt

import android.content.Context
import com.joker.floatingsubtitleapp.domain.repository.SpeechRecognitionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.vosk.Model
import org.vosk.Recognizer
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.text.isNotBlank

@Singleton
class VoskSpeechEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : SpeechRecognitionRepository {

    private var model: Model? = null
    private val sampleRate = 16000.0f

    override suspend fun initEngine(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // TODO: Phase 8에서 실제 모델 파일을 assets에서 복사하여 로드하는 로직 추가 예정
            // 현재는 컴파일 에러를 방지하기 위해 구조만 유지합니다.
            if (model == null) {
                // model = Model("model-path-here")
            }
        }
    }

    override fun recognize(audioData: Flow<ShortArray>): Flow<String> = callbackFlow {
        val currentModel = model
        if (currentModel == null) {
            // 모델이 없으면 일단 닫지 않고 에러 로그만 남기거나 빈 스트림 유지
            // 실제 구현 시에는 initEngine이 먼저 호출되어야 함
            return@callbackFlow
        }

        val recognizer = Recognizer(currentModel, sampleRate)

        // callbackFlow의 Scope를 사용하여 launch 실행
        val job = launch(Dispatchers.Default) {
            audioData.collect { buffer ->
                if (recognizer.acceptWaveForm(buffer, buffer.size)) {
                    val resultJson = recognizer.result
                    val text = JSONObject(resultJson).optString("text", "")
                    if (text.isNotBlank()) {
                        trySend(text)
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