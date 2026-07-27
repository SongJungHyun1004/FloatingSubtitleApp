package com.joker.floatingsubtitleapp.data.translate

import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.joker.floatingsubtitleapp.domain.repository.TranslateRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MLKitTranslateRepository @Inject constructor() : TranslateRepository {

    private val modelManager = RemoteModelManager.getInstance()
    private val TAG = "MLKitTranslate"

    override suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<String> = runCatching {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLang)
            .setTargetLanguage(targetLang)
            .build()

        val translator = Translation.getClient(options)

        try {
            // 번역 실행 (.await()는 kotlinx-coroutines-play-services 필요)
            translator.translate(text).await()
        } finally {
            translator.close()
        }
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
