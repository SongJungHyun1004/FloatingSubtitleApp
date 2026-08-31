package com.joker.floatingsubtitleapp.data.stt

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ModelPrepareProgress {
    /** fraction: 0f~1f 다운로드 진행률, -1f면 진행률을 알 수 없는 단계(압축 해제 등). */
    data class InProgress(val fraction: Float) : ModelPrepareProgress
    data class Done(val modelPath: String) : ModelPrepareProgress
}

@Singleton
class VoskModelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // 언어별로 독립된 락을 둬서, 서로 다른 언어는 동시에 받되(별개 파일이라
    // 안전함) 같은 언어에 대한 요청은 순서대로만 처리되게 한다. 이게 없으면
    // 같은 언어를 아주 빠르게 두 번 선택했을 때 같은 zip 파일에 동시에
    // 쓰다가 깨질 수 있다.
    private val locksByLang = mutableMapOf<String, Mutex>()
    private val locksMapGuard = Mutex()

    private suspend fun lockFor(langCode: String): Mutex =
        locksMapGuard.withLock {
            locksByLang.getOrPut(langCode) { Mutex() }
        }

    /**
     * 언어별 Vosk 모델을 준비한다. VoskModels.downloadable에 등록된 언어는
     * 전부 이 공통 경로(다운로드 -> 압축 해제)를 탄다. en도 예외가 아니다 -
     * 예전엔 en만 assets에 번들된 Small 모델을 썼는데, 정확도가 더 높은
     * lgraph 모델로 품질을 올리면서 다른 언어들과 동일하게 통일했다.
     * 이미 받아져 있으면(.complete 마커 존재) 재다운로드 없이 바로 Done을 낸다.
     */
    fun prepareModel(langCode: String): Flow<ModelPrepareProgress> = channelFlow {
        lockFor(langCode).withLock {
            val info = VoskModels.infoFor(langCode)
                ?: throw IllegalArgumentException("Vosk가 지원하지 않는 언어입니다: $langCode")

            val modelDir = File(context.getExternalFilesDir(null), "vosk-models/${info.langCode}")
            val markerFile = File(modelDir, ".complete")

            if (markerFile.exists()) {
                send(ModelPrepareProgress.Done(resolveModelRoot(modelDir).absolutePath))
                return@withLock
            }

            // 예전에 받다가 중단된 잔여 파일이 있으면 지우고 새로 받는다.
            if (modelDir.exists()) modelDir.deleteRecursively()
            modelDir.mkdirs()

            val zipFile = File(context.cacheDir, "${info.modelName}.zip")

            downloadFile(info.downloadUrl, zipFile) { fraction ->
                trySend(ModelPrepareProgress.InProgress(fraction))
            }

            trySend(ModelPrepareProgress.InProgress(-1f)) // 압축 해제 단계
            unzip(zipFile, modelDir)
            zipFile.delete()

            val actualRoot = resolveModelRoot(modelDir)
            markerFile.createNewFile()
            send(ModelPrepareProgress.Done(actualRoot.absolutePath))
        }
    }.flowOn(Dispatchers.IO)

    // Vosk zip은 보통 "vosk-model-small-xx-0.xx/" 하위 폴더 하나를 포함한다.
    // Model()이 바로 쓸 수 있도록 그 하위 폴더를 실제 모델 루트로 잡아준다.
    private fun resolveModelRoot(modelDir: File): File =
        modelDir.listFiles()?.firstOrNull { it.isDirectory } ?: modelDir

    private fun downloadFile(urlString: String, outputFile: File, onProgress: (Float) -> Unit) {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
            connect()
        }
        try {
            val totalBytes = connection.contentLength
            var downloadedBytes = 0L

            connection.inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (totalBytes > 0) {
                            onProgress(downloadedBytes.toFloat() / totalBytes)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // 중간에 실패한 조각 파일이 남아서 다음 시도에 혼란을 주지 않도록 정리한다.
            outputFile.delete()
            throw e
        } finally {
            connection.disconnect()
        }
    }

    private fun unzip(zipFile: File, targetDir: File) {
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { output -> zis.copyTo(output) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}