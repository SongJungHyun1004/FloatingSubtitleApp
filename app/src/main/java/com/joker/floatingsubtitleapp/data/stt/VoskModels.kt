package com.joker.floatingsubtitleapp.data.stt

/**
 * Vosk 모델 정보. 출처: https://alphacephei.com/vosk/models
 * 여기 없는 언어는 Vosk가 공식 모델을 제공하지 않는 언어라 STT 소스 언어로 쓸 수 없다
 * (예: 태국어 - 커뮤니티 프로젝트만 있고 Vosk 정식 포맷 모델이 없음).
 */
data class VoskModelInfo(
    val langCode: String,
    val modelName: String,
    val downloadUrl: String,
    val approxSizeMb: Int
)

object VoskModels {
    val downloadable = listOf(
        // en: Small(40MB) 대신 정확도가 더 높은 중간 등급(lgraph, ~128MB) 모델을 쓴다.
        // 품질 개선을 위해 의도적으로 바꾼 것 - assets 번들 대신 다른 언어들처럼
        // 런타임에 다운로드한다.
        VoskModelInfo(
            "en", "vosk-model-en-us-0.22-lgraph",
            "https://alphacephei.com/vosk/models/vosk-model-en-us-0.22-lgraph.zip", 128
        ),
        VoskModelInfo(
            "ko", "vosk-model-small-ko-0.22",
            "https://alphacephei.com/vosk/models/vosk-model-small-ko-0.22.zip", 82
        ),
        VoskModelInfo(
            "ja", "vosk-model-small-ja-0.22",
            "https://alphacephei.com/vosk/models/vosk-model-small-ja-0.22.zip", 48
        ),
        VoskModelInfo(
            "zh", "vosk-model-small-cn-0.22",
            "https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip", 42
        ),
        VoskModelInfo(
            "es", "vosk-model-small-es-0.42",
            "https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip", 39
        ),
        VoskModelInfo(
            "fr", "vosk-model-small-fr-0.22",
            "https://alphacephei.com/vosk/models/vosk-model-small-fr-0.22.zip", 41
        ),
        VoskModelInfo(
            "de", "vosk-model-small-de-0.15",
            "https://alphacephei.com/vosk/models/vosk-model-small-de-0.15.zip", 45
        ),
        VoskModelInfo(
            "vi", "vosk-model-small-vn-0.4",
            "https://alphacephei.com/vosk/models/vosk-model-small-vn-0.4.zip", 32
        ),
        VoskModelInfo(
            "ru", "vosk-model-small-ru-0.22",
            "https://alphacephei.com/vosk/models/vosk-model-small-ru-0.22.zip", 45
        ),
    )

    fun infoFor(langCode: String): VoskModelInfo? = downloadable.firstOrNull { it.langCode == langCode }

    /** STT 소스 언어로 선택 가능한 전체 언어 코드. */
    val allSttSupportedLangCodes: Set<String> = downloadable.map { it.langCode }.toSet()
}