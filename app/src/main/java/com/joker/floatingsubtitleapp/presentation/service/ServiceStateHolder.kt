package com.joker.floatingsubtitleapp.presentation.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 지금 SubtitleService가 캡처 중인지, 캡처 중이라면 어떤 언어로 시작됐는지를
 * 담는다. null이면 "실행 중 아님". MainActivity가 이걸 보고 시작 버튼을
 * 막거나, 설정 화면에서 언어를 바꿨는데 실행 중인 세션과 달라졌는지 판단한다.
 */
@Singleton
class ServiceStateHolder @Inject constructor() {

    data class RunningLanguages(val sourceLang: String, val targetLang: String)

    private val _runningLanguages = MutableStateFlow<RunningLanguages?>(null)
    val runningLanguages: StateFlow<RunningLanguages?> = _runningLanguages.asStateFlow()

    fun setRunning(sourceLang: String, targetLang: String) {
        _runningLanguages.value = RunningLanguages(sourceLang, targetLang)
    }

    fun setStopped() {
        _runningLanguages.value = null
    }
}