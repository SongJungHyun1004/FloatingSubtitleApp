package com.joker.floatingsubtitleapp.domain.usecase

import android.content.Intent
import android.util.Log
import com.joker.floatingsubtitleapp.domain.model.SpeechResult
import com.joker.floatingsubtitleapp.domain.model.SubtitleEvent
import com.joker.floatingsubtitleapp.domain.model.SubtitleLine
import com.joker.floatingsubtitleapp.domain.repository.AudioCaptureRepository
import com.joker.floatingsubtitleapp.domain.repository.SpeechRecognitionRepository
import com.joker.floatingsubtitleapp.domain.repository.TranslateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

class GetSubtitleFlowUseCase @Inject constructor(
    private val audioRepo: AudioCaptureRepository,
    private val sttRepo: SpeechRecognitionRepository,
    private val translateRepo: TranslateRepository
) {
    private val TAG = "FlowTracker"

    // Partial 텍스트가 이 시간(ms) 동안 더 이상 늘어나지 않아야("안정됐다"고 판단)
    // 그제서야 번역한다. 고정 간격(구 sample(600))이 아니라 "안정성" 기준으로 바꾼 이유:
    // 문장이 아직 진행 중인 불완전한 조각을 매번 처음부터 재번역하면 MLKit이
    // 원문에도 없는 반복을 만들어내는 경향이 있었다.
    private val PARTIAL_DEBOUNCE_MS = 500L

    /**
     * @param resultCode MediaProjection 승인 결과 코드
     * @param data MediaProjection 승인 데이터
     * @param sourceLang 원본 언어 (예: "en")
     * @param targetLang 대상 언어 (예: "ko")
     */
    operator fun invoke(
        resultCode: Int,
        data: Intent,
        sourceLang: String,
        targetLang: String
    ): Flow<SubtitleEvent> = channelFlow {
        Log.d(TAG, "번역 모델 사전 점검 중... ($sourceLang -> $targetLang)")
        translateRepo.downloadModelIfNeeded(sourceLang).onFailure { e ->
            Log.e(TAG, "번역 모델 다운로드 실패($sourceLang): ${e.message}", e)
        }
        translateRepo.downloadModelIfNeeded(targetLang).onFailure { e ->
            Log.e(TAG, "번역 모델 다운로드 실패($targetLang): ${e.message}", e)
        }

        val audioFlow = audioRepo.startCapture(resultCode, data)
        val textFlow = sttRepo.recognize(audioFlow, sourceLang)
            .onEach { Log.d(TAG, "STT 결과: [$it]") }

        // 가장 최근 Partial 텍스트(원문). null이면 "지금은 표시할 미리보기 없음" 상태.
        val latestPartialText = MutableStateFlow<String?>(null)

        // Partial 전용 파이프라인: 텍스트가 안정된(=일정 시간 더 안 늘어난) 시점에만 번역.
        launch {
            latestPartialText
                .filterNotNull()
                .debounce(PARTIAL_DEBOUNCE_MS)
                .collect { originalText ->
                    val translated = translateRepo.translate(originalText, sourceLang, targetLang)
                        .getOrDefault(originalText)

                    // 번역하는 동안 더 새 Partial이 왔거나 Final로 확정되면서
                    // 상태가 이미 바뀌었다면, 이 번역 결과는 낡은 것이므로 버린다.
                    if (latestPartialText.value == originalText) {
                        send(SubtitleEvent.PartialUpdated(text = translated, originalText = originalText))
                    }
                }
        }

        // Final 전용 파이프라인: 절대 디바운스/스킵/드롭하지 않고 즉시 처리한다.
        textFlow.collect { speechResult ->
            when (speechResult) {
                is SpeechResult.Partial -> {
                    latestPartialText.value = speechResult.text
                }

                is SpeechResult.Final -> {
                    Log.d(TAG, "번역 요청 시작(Final): ${speechResult.text}")

                    val translateResult = translateRepo.translate(
                        speechResult.text, sourceLang, targetLang
                    )
                    translateResult.onFailure { e ->
                        Log.e(TAG, "번역 실패 원인: ${e.message}", e)
                    }
                    val translated = translateResult.getOrDefault(speechResult.text)

                    if (translated.isNotBlank()) {
                        send(
                            SubtitleEvent.LineFinalized(
                                SubtitleLine(
                                    id = UUID.randomUUID().toString(),
                                    text = translated,
                                    originalText = speechResult.text
                                )
                            )
                        )
                    }

                    // Final이 확정됐으니 대기 중이던 partial 번역은 의미가 없어졌다.
                    latestPartialText.value = null
                    send(SubtitleEvent.PartialUpdated(text = "", originalText = ""))
                }
            }
        }
    }

    fun stop() {
        audioRepo.stopCapture()
    }
}