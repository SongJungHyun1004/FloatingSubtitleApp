package com.joker.floatingsubtitleapp.presentation.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OverlayController @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        private const val TAG = "OverlayController"
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null

    private val subtitleText = mutableStateOf("인식된 자막이 여기에 표시됩니다.")

    override val viewModelStore: ViewModelStore = ViewModelStore()
    override val lifecycle: LifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry = savedStateRegistryController.savedStateRegistry

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        y = 100
    }

    fun show() {
        if (composeView != null) {
            Log.d(TAG, "ComposeView가 이미 생성되어 있습니다.")
            return
        }

        try {
            // ⭕ SavedStateRegistry는 Lifecycle Event 전환 전에 Restore 되어야 함
            savedStateRegistryController.performRestore(null)
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

            composeView = ComposeView(context).apply {
                setViewTreeLifecycleOwner(this@OverlayController)
                setViewTreeViewModelStoreOwner(this@OverlayController)
                setViewTreeSavedStateRegistryOwner(this@OverlayController)

                setContent {
                    SubtitleOverlay(
                        text = subtitleText.value,
                        onDrag = { dx, dy -> updatePosition(dx, dy) }
                    )
                }
            }
            windowManager.addView(composeView, params)
            Log.d(TAG, "오버레이 뷰가 WindowManager에 성공적으로 추가되었습니다.")
        } catch (e: Exception) {
            Log.e(TAG, "오버레이 뷰 추가 실패: ${e.message}", e)
        }
    }

    fun updateText(newText: String) {
        subtitleText.value = newText
    }

    private fun updatePosition(dx: Float, dy: Float) {
        composeView?.let { view ->
            params.x += dx.toInt()
            params.y += dy.toInt()
            windowManager.updateViewLayout(view, params)
        }
    }

    fun hide() {
        composeView?.let { view ->
            try {
                windowManager.removeView(view)
                lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                composeView = null
                Log.d(TAG, "오버레이 뷰가 제거되었습니다.")
            } catch (e: Exception) {
                Log.e(TAG, "오버레이 뷰 제거 실패: ${e.message}", e)
            }
        }
    }
}