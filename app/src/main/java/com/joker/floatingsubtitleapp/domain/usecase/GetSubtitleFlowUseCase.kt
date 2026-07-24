package com.joker.floatingsubtitleapp.domain.usecase

import android.content.Intent
import com.joker.floatingsubtitleapp.domain.repository.AudioCaptureRepository
import com.joker.floatingsubtitleapp.domain.repository.SpeechRecognitionRepository
import com.joker.floatingsubtitleapp.domain.repository.TranslateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
    operator fun invoke(
        resultCode: Int,
        data: Intent,
        sourceLang: String,
        targetLang: String
    ): Flow<String> {
        // 1. 오디오 캡처 시작 (Flow<ShortArray>)
        val audioFlow = audioRepo.startCapture(resultCode, data)

        // 2. STT 변환 (Flow<String>)
        val textFlow = sttRepo.recognize(audioFlow)

        // 3. 실시간 번역 적용
        return textFlow.map { originalText ->
            translateRepo.translate(originalText, sourceLang, targetLang)
                .getOrDefault(originalText) // 번역 실패 시 원문 표시
        }
    }

    fun stop() {
        audioRepo.stopCapture()
    }
}