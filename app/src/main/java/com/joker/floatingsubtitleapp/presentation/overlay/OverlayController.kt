package com.joker.floatingsubtitleapp.presentation.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
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

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null

    private val subtitleText = mutableStateOf("인식된 자막이 여기에 표시됩니다.")

    // ComposeView를 위한 Lifecycle 설정
    override val viewModelStore: ViewModelStore = ViewModelStore()
    override val lifecycle: LifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry = savedStateRegistryController.savedStateRegistry

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        y = 100
    }

    fun show() {
        if (composeView != null) return

        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        savedStateRegistryController.performRestore(null)

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
    }

    fun updateText(newText: String) {
        subtitleText.value = newText
    }

    private fun updatePosition(dx: Float, dy: Float) {
        params.x += dx.toInt()
        params.y += dy.toInt()
        windowManager.updateViewLayout(composeView, params)
    }

    fun hide() {
        composeView?.let {
            windowManager.removeView(it)
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            composeView = null
        }
    }
}