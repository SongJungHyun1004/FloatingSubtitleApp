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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
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
    // 리사이즈 전(WRAP_CONTENT)에는 null -> Compose가 내용에 맞춰 자동 크기.
    // 리사이즈를 시작하면 실제 창 크기(dp)를 넣어줘서, Compose 쪽 내용물도
    // 그 크기에 정확히 맞춰지도록 한다. (전에는 WindowManager 크기만 바뀌고
    // Compose 내용물은 여전히 자기 콘텐츠 크기로만 그려져서 리사이즈 핸들이
    // 따로 노는 것처럼 보였다.)
    private val windowSizeState = mutableStateOf<DpSize?>(null)

    private val density = context.resources.displayMetrics.density
    private val minWidthPx = (MIN_WIDTH_DP * density).toInt()
    private val minHeightPx = (MIN_HEIGHT_DP * density).toInt()
    private val screenWidthPx = context.resources.displayMetrics.widthPixels

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        // CENTER_HORIZONTAL 대신 START(왼쪽 기준)를 쓴다. 중앙 정렬이면 폭이
        // 바뀔 때마다 왼쪽 가장자리를 고정하기 위해 x를 매번 보정해야 하는데,
        // 그 보정이 리사이즈 드래그 도중 뷰 자체를 계속 이동시켜서 터치 좌표
        // 기준이 흔들리는 부자연스러운 동작의 원인이었다. START 기준이면
        // 폭이 늘어나도 왼쪽은 자동으로 고정되고 오른쪽만 늘어나서 x를
        // 리사이즈 중에 전혀 건드릴 필요가 없다.
        gravity = Gravity.TOP or Gravity.START
        // 초기 위치는 대략 화면 가운데 근처에 오도록 추정 배치한다
        // (WRAP_CONTENT라 정확한 초기 폭은 알 수 없어서 최소 폭 기준으로 근사).
        x = (screenWidthPx - minWidthPx) / 2
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
                    val windowSize by windowSizeState

                    SubtitleOverlay(
                        state = uiState,
                        isLocked = isLocked,
                        isMinimized = isMinimized,
                        fixedSize = windowSize,
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

            val newWidthPx = (currentWidth + dw.toInt()).coerceAtLeast(minWidthPx)
            val newHeightPx = (currentHeight + dh.toInt()).coerceAtLeast(minHeightPx)

            // gravity가 START(왼쪽 기준)라, 폭만 키우면 왼쪽 가장자리는 자동으로
            // 고정되고 오른쪽으로만 늘어난다 - x를 따로 건드릴 필요가 없다.
            params.width = newWidthPx
            params.height = newHeightPx
            windowManager.updateViewLayout(view, params)

            // Compose 쪽 내용물도 이 크기를 정확히 알아야 리사이즈 핸들이
            // 실제 창 경계와 같이 움직인다.
            windowSizeState.value = DpSize((newWidthPx / density).dp, (newHeightPx / density).dp)
        }
    }

    private fun resetSizeToWrapContent() {
        composeView?.let { view ->
            params.width = WindowManager.LayoutParams.WRAP_CONTENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            windowManager.updateViewLayout(view, params)
            windowSizeState.value = null
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