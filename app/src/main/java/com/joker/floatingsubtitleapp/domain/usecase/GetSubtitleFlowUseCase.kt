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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transform
import java.util.UUID
import javax.inject.Inject

class GetSubtitleFlowUseCase @Inject constructor(
    private val audioRepo: AudioCaptureRepository,
    private val sttRepo: SpeechRecognitionRepository,
    private val translateRepo: TranslateRepository
) {
    private val TAG = "FlowTracker"

    // Partial 전용 스로틀 간격. Final에는 절대 적용하지 않는다.
    private val PARTIAL_THROTTLE_MS = 600L

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
    ): Flow<SubtitleEvent> {
        // 1. 오디오 캡처
        val audioFlow = audioRepo.startCapture(resultCode, data)

        // 2. STT 변환
        val textFlow = sttRepo.recognize(audioFlow)
            .onEach { Log.d(TAG, "STT 결과: [$it]") }

        // 마지막으로 Partial을 내보낸 시각. Final이 오면 0으로 리셋되어
        // 다음 발화의 첫 Partial은 스로틀 없이 바로 나간다.
        var lastPartialEmitAt = 0L

        // 3. Partial/Final을 서로 다른 정책으로 번역 + 변환
        return textFlow
            .onStart {
                Log.d(TAG, "번역 모델 사전 점검 중... ($sourceLang -> $targetLang)")
                translateRepo.downloadModelIfNeeded(sourceLang).onFailure { e ->
                    Log.e(TAG, "번역 모델 다운로드 실패($sourceLang): ${e.message}", e)
                }
                translateRepo.downloadModelIfNeeded(targetLang).onFailure { e ->
                    Log.e(TAG, "번역 모델 다운로드 실패($targetLang): ${e.message}", e)
                }
            }
            .transform { speechResult ->
                when (speechResult) {
                    is SpeechResult.Partial -> {
                        // Partial은 실시간성이 목적이라 하나 건너뛰어도
                        // 바로 다음 게 오므로 스로틀링 대상이다.
                        val now = System.currentTimeMillis()
                        if (now - lastPartialEmitAt < PARTIAL_THROTTLE_MS) return@transform
                        lastPartialEmitAt = now

                        val translated = translateRepo
                            .translate(speechResult.text, sourceLang, targetLang)
                            .getOrDefault(speechResult.text)

                        emit(SubtitleEvent.PartialUpdated(translated))
                    }

                    is SpeechResult.Final -> {
                        // Final은 절대 스로틀링하거나 드롭하지 않는다.
                        // "번역 누락 없음" 요구사항이 여기서 구조적으로 보장된다.
                        Log.d(TAG, "번역 요청 시작(Final): ${speechResult.text}")

                        val translateResult = translateRepo
                            .translate(speechResult.text, sourceLang, targetLang)

                        translateResult.onFailure { e ->
                            Log.e(TAG, "번역 실패 원인: ${e.message}", e)
                        }

                        val translated = translateResult.getOrDefault(speechResult.text)

                        if (translated.isNotBlank()) {
                            emit(
                                SubtitleEvent.LineFinalized(
                                    SubtitleLine(
                                        id = UUID.randomUUID().toString(),
                                        text = translated
                                    )
                                )
                            )
                        }

                        // Vosk는 Final을 낸 직후 내부 상태를 리셋하므로,
                        // 화면에 남아있던 이전 partial 텍스트도 즉시 비워야
                        // "방금 확정된 문장"이 partial 자리에 중복 표시되지 않는다.
                        emit(SubtitleEvent.PartialUpdated(""))
                        lastPartialEmitAt = 0L
                    }
                }
            }
    }

    fun stop() {
        audioRepo.stopCapture()
    }
}