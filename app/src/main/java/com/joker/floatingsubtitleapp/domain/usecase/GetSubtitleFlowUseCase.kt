package com.joker.floatingsubtitleapp.domain.usecase

import android.content.Intent
import android.util.Log
import com.joker.floatingsubtitleapp.domain.model.SpeechResult
import com.joker.floatingsubtitleapp.domain.model.SubtitleState
import com.joker.floatingsubtitleapp.domain.repository.AudioCaptureRepository
import com.joker.floatingsubtitleapp.domain.repository.SpeechRecognitionRepository
import com.joker.floatingsubtitleapp.domain.repository.TranslateRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.sample
import javax.inject.Inject

class GetSubtitleFlowUseCase @Inject constructor(
    private val audioRepo: AudioCaptureRepository,
    private val sttRepo: SpeechRecognitionRepository,
    private val translateRepo: TranslateRepository
) {
    /**
     * @param resultCode MediaProjection 승인 결과 코드
     * @param data MediaProjection 승인 데이터
     * @param sourceLang 원본 언어 (예: "en")
     * @param targetLang 대상 언어 (예: "ko")
     */

    private val TAG = "FlowTracker"

    operator fun invoke(
        resultCode: Int,
        data: Intent,
        sourceLang: String,
        targetLang: String
    ): Flow<SubtitleState> {
//        // 1. 오디오 캡처 시작 (Flow<ShortArray>)
//        val audioFlow = audioRepo.startCapture(resultCode, data)
//
//        // 2. STT 변환 (Flow<String>)
//        val textFlow = sttRepo.recognize(audioFlow)
//
//        // 3. 실시간 번역 적용
//        return textFlow.map { originalText ->
//            translateRepo.translate(originalText, sourceLang, targetLang)
//                .getOrDefault(originalText) // 번역 실패 시 원문 표시
//        }
        // 1. 오디오 캡처 파이프라인 검사
        val audioFlow = audioRepo.startCapture(resultCode, data)
//            .onEach { Log.d(TAG, "🎧 오디오 버퍼 수신: ${it.size} 사이즈") }

        // 2. STT 변환 파이프라인 검사
        val textFlow = sttRepo.recognize(audioFlow)
            .onEach { Log.d(TAG, "🗣️ STT 변환 결과: [$it]") }

        // 3. 실시간 번역 적용
        @OptIn(FlowPreview::class)
        return textFlow
            .onStart {
                // 💡 번역 파이프라인 시작 전, 원본 언어와 대상 언어 모델 다운로드 보장
                Log.d(TAG, "🌐 번역 모델 사전 점검 중... ($sourceLang -> $targetLang)")
                translateRepo.downloadModelIfNeeded(sourceLang)
                translateRepo.downloadModelIfNeeded(targetLang)
            }
            .sample(200) // ✅ 부분 결과 과도 호출 방지
            .map { speechResult ->

                val (rawText, isFinal) = when (speechResult) {
                    is SpeechResult.Partial -> speechResult.text to false
                    is SpeechResult.Final -> speechResult.text to true
                }

                Log.d(TAG, "🔄 번역 요청 시작: $rawText")

                val translateResult = translateRepo.translate(rawText, sourceLang, targetLang)

                translateResult.onFailure { e ->
                    Log.e(TAG, "❌ 번역 실패 원인: ${e.message}", e)
                }

                val translated = translateResult.getOrDefault(rawText)

                Log.d(TAG, "✅ 번역 완료: $translated (final=$isFinal)")

                SubtitleState(
                    text = translated,
                    isFinal = isFinal
                )
            }
    }

    fun stop() {
        audioRepo.stopCapture()
    }
}