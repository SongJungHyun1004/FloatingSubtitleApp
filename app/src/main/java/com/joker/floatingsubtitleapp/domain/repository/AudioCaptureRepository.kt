package com.joker.floatingsubtitleapp.domain.repository

import android.content.Intent
import kotlinx.coroutines.flow.Flow

interface AudioCaptureRepository {
    fun startCapture(resultCode: Int, data: Intent): Flow<ShortArray>
    fun stopCapture()
}