package com.joker.floatingsubtitleapp.di

import com.joker.floatingsubtitleapp.data.audio.AudioCaptureRepositoryImpl
import com.joker.floatingsubtitleapp.domain.repository.AudioCaptureRepository
import com.joker.floatingsubtitleapp.data.stt.VoskSpeechEngine
import com.joker.floatingsubtitleapp.domain.repository.SpeechRecognitionRepository
import com.joker.floatingsubtitleapp.data.translate.MLKitTranslateRepository
import com.joker.floatingsubtitleapp.domain.repository.TranslateRepository
import com.joker.floatingsubtitleapp.data.settings.LanguagePreferenceRepositoryImpl
import com.joker.floatingsubtitleapp.domain.repository.LanguagePreferenceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAudioCaptureRepository(
        audioCaptureRepositoryImpl: AudioCaptureRepositoryImpl
    ): AudioCaptureRepository

    @Binds
    @Singleton
    abstract fun bindSpeechRecognitionRepository(
        voskSpeechEngine: VoskSpeechEngine
    ): SpeechRecognitionRepository

    @Binds
    @Singleton
    abstract fun bindTranslateRepository(
        mlKitTranslateRepository: MLKitTranslateRepository
    ): TranslateRepository

    @Binds
    @Singleton
    abstract fun bindLanguagePreferenceRepository(
        languagePreferenceRepositoryImpl: LanguagePreferenceRepositoryImpl
    ): LanguagePreferenceRepository
}