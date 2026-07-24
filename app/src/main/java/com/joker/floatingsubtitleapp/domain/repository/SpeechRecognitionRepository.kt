package com.joker.floatingsubtitleapp.domain.repository

import kotlinx.coroutines.flow.Flow

interface SpeechRecognitionRepository {
    /**
     * 오디오 데이터를 받아 인식된 텍스트(문장 단위)를 반환합니다.
     */
    fun recognize(audioData: Flow<ShortArray>): Flow<String>

    /**
     * STT 엔진 초기화 (모델 로드 등)
     */
    suspend fun initEngine(): Result<Unit>
}