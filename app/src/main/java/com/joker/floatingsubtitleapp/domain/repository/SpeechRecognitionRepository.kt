package com.joker.floatingsubtitleapp.domain.repository

import com.joker.floatingsubtitleapp.domain.model.SpeechResult
import kotlinx.coroutines.flow.Flow

interface SpeechRecognitionRepository {
    /**
     * 오디오 데이터를 받아 인식된 텍스트(문장 단위)를 반환합니다.
     * @param sourceLang 인식할 음성의 언어 (예: "en", "ko"). 이전에 로드된 언어와
     *                   다르면 내부적으로 해당 언어 모델을 새로 로드합니다.
     */
    fun recognize(audioData: Flow<ShortArray>, sourceLang: String): Flow<SpeechResult>

    /**
     * STT 엔진 초기화 (해당 언어 모델 로드, 필요시 다운로드).
     */
    suspend fun initEngine(sourceLang: String): Result<Unit>
}