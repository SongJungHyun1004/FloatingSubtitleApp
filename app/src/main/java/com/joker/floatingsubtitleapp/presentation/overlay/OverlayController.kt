package com.joker.floatingsubtitleapp.presentation.overlay

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.joker.floatingsubtitleapp.MainActivity
import com.joker.floatingsubtitleapp.presentation.service.SubtitleService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 오버레이 창을 WindowManager에 붙이고 떼는 것, 드래그로 위치/크기를 바꾸는 것,
 * 잠금·최소화·앱 복귀·서비스 종료 같은 창 자체의 조작만 책임진다.
 * 자막 줄이 몇 개 떠 있는지, 언제 사라지는지는 전혀 모른다 -
 * 그건 SubtitleLineManager의 uiState를 그대로 구독해서 그릴 뿐이다.
 */
@Singleton
class OverlayController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val subtitleLineManager: SubtitleLineManager
) : ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        private const val TAG = "OverlayController"
        private const val MIN_WIDTH_DP = 200
        private const val MIN_HEIGHT_DP = 100
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null

    // Lifecycle은 한 번 DESTROYED가 되면 같은 인스턴스로는 다시 살릴 수 없다
    // (Android의 정책). OverlayController 자체는 @Singleton이라 앱 프로세스
    // 동안 계속 살아있지만, 그 안의 Lifecycle/ViewModelStore/SavedStateRegistry는
    // show()가 다시 호출될 때마다 "이번 표시 세션"용으로 새로 만든다.
    // (예전 버전은 이 3개를 생성자에서 val로 한 번만 만들어서, 종료 후 재시작하면
    // 오버레이가 안 뜨는 버그가 있었다.)
    override var viewModelStore: ViewModelStore = ViewModelStore()
        private set
    override var lifecycle: LifecycleRegistry = LifecycleRegistry(this)
        private set
    private var savedStateRegistryController = SavedStateRegistryController.create(this)
    override var savedStateRegistry = savedStateRegistryController.savedStateRegistry
        private set

    // 잠금/최소화는 순수 오버레이 UI 상태라 SubtitleLineManager(자막 데이터)와
    // 무관하게 여기서 직접 들고 있는다. mutableStateOf라 Compose가 변화를 관찰한다.
    private val isLockedState = mutableStateOf(false)
    private val isMinimizedState = mutableStateOf(false)

    private val density = context.resources.displayMetrics.density
    private val minWidthPx = (MIN_WIDTH_DP * density).toInt()
    private val minHeightPx = (MIN_HEIGHT_DP * density).toInt()

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

        // 이번 표시 세션을 위한 새 Lifecycle 3종 생성 (위 주석 참고)
        viewModelStore = ViewModelStore()
        lifecycle = LifecycleRegistry(this)
        savedStateRegistryController = SavedStateRegistryController.create(this)
        savedStateRegistry = savedStateRegistryController.savedStateRegistry

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
                    val uiState by subtitleLineManager.uiState.collectAsState()
                    val isLocked by isLockedState
                    val isMinimized by isMinimizedState

                    SubtitleOverlay(
                        state = uiState,
                        isLocked = isLocked,
                        isMinimized = isMinimized,
                        onDrag = { dx, dy -> if (!isLocked) updatePosition(dx, dy) },
                        onResize = { dx, dy -> updateSize(dx, dy) },
                        onToggleLock = { isLockedState.value = !isLockedState.value },
                        onToggleMinimize = {
                            isMinimizedState.value = !isMinimizedState.value
                            resetSizeToWrapContent()
                        },
                        onReturnToApp = { returnToApp() },
                        onStopService = { stopService() }
                    )
                }
            }
            windowManager.addView(composeView, params)
            Log.d(TAG, "오버레이 뷰가 WindowManager에 성공적으로 추가되었습니다.")
        } catch (e: Exception) {
            Log.e(TAG, "오버레이 뷰 추가 실패: ${e.message}", e)
        }
    }

    private fun updatePosition(dx: Float, dy: Float) {
        composeView?.let { view ->
            params.x += dx.toInt()
            params.y += dy.toInt()
            windowManager.updateViewLayout(view, params)
        }
    }

    private fun updateSize(dw: Float, dh: Float) {
        composeView?.let { view ->
            // 처음 리사이즈를 시작하는 순간, WRAP_CONTENT(음수값)였던 크기를
            // 지금 실제 보이는 픽셀 크기로 고정한 뒤부터 드래그만큼 더한다.
            val currentWidth = if (params.width <= 0) view.width else params.width
            val currentHeight = if (params.height <= 0) view.height else params.height

            params.width = (currentWidth + dw.toInt()).coerceAtLeast(minWidthPx)
            params.height = (currentHeight + dh.toInt()).coerceAtLeast(minHeightPx)
            windowManager.updateViewLayout(view, params)
        }
    }

    private fun resetSizeToWrapContent() {
        composeView?.let { view ->
            params.width = WindowManager.LayoutParams.WRAP_CONTENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            windowManager.updateViewLayout(view, params)
        }
    }

    private fun returnToApp() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun stopService() {
        context.stopService(Intent(context, SubtitleService::class.java))
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