package com.joker.floatingsubtitleapp.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface DisplayPreferenceRepository {
    /** 확정 줄/partial 텍스트 아래에 원문(듣는 언어 그대로)을 작고 흐리게 같이 보여줄지 여부. */
    val showOriginalText: StateFlow<Boolean>

    fun setShowOriginalText(enabled: Boolean)
}