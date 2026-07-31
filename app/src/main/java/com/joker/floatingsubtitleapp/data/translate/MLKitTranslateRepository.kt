package com.joker.floatingsubtitleapp.data.translate

import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.joker.floatingsubtitleapp.domain.repository.TranslateRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MLKitTranslateRepository @Inject constructor() : TranslateRepository {

    private val modelManager = RemoteModelManager.getInstance()
    private val TAG = "MLKitTranslate"

    // 언어쌍(예: "en-ko")별로 Translator를 캐싱한다.
    // 이전에는 translate() 호출마다 Translator를 새로 만들고 바로 close() 했는데,
    // 실시간 자막처럼 짧은 간격으로 반복 호출되는 상황에서는 불필요한 초기화 비용이었다.
    // 앱 프로세스가 살아있는 동안 재사용하고, 언어쌍 개수는 사실상 1~2개로 고정이라
    // 메모리 누수 걱정 없이 계속 들고 있어도 된다.
    private val translators = mutableMapOf<String, Translator>()
    private val mutex = Mutex()

    private suspend fun getOrCreateTranslator(sourceLang: String, targetLang: String): Translator {
        val key = "$sourceLang-$targetLang"
        mutex.withLock {
            translators[key]?.let { return it }

            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLang)
                .setTargetLanguage(targetLang)
                .build()

            val translator = Translation.getClient(options)
            translators[key] = translator
            return translator
        }
    }

    override suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<String> = runCatching {
        val translator = getOrCreateTranslator(sourceLang, targetLang)
        translator.translate(text).await()
    }

    override suspend fun downloadModelIfNeeded(langCode: String): Result<Unit> = runCatching {
        val model = TranslateRemoteModel.Builder(langCode).build()
        val isDownloaded = modelManager.isModelDownloaded(model).await()

        if (!isDownloaded) {
            Log.d(TAG, "⏳ MLKit 번역 모델 다운로드 시작: [$langCode]")
            val conditions = DownloadConditions.Builder().build()
            modelManager.download(model, conditions).await()
            Log.d(TAG, "✅ MLKit 번역 모델 다운로드 완료: [$langCode]")
        } else {
            Log.d(TAG, "ℹ️ MLKit 번역 모델이 이미 존재합니다: [$langCode]")
        }
    }
}