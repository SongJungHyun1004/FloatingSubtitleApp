package com.joker.floatingsubtitleapp.data.stt

/**
 * Vosk 공식 "small" 모델 정보. 출처: https://alphacephei.com/vosk/models
 * en은 앱에 이미 assets로 번들되어 있어서 이 목록에 없다 (다운로드 불필요).
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

    /** STT 소스 언어로 선택 가능한 전체 언어 코드 (en 포함, 번들 여부 무관). */
    val allSttSupportedLangCodes: Set<String> = (downloadable.map { it.langCode } + "en").toSet()
}