package com.joker.floatingsubtitleapp.presentation.overlay

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
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
import com.joker.floatingsubtitleapp.domain.repository.DisplayPreferenceRepository
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
    private val subtitleLineManager: SubtitleLineManager,
    private val displayPreferenceRepository: DisplayPreferenceRepository
) : ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        private const val TAG = "OverlayController"
        private const val MIN_WIDTH_DP = 200
        private const val MIN_HEIGHT_DP = 100
        // 창의 "기본" 크기. WRAP_CONTENT를 쓰지 않고 항상 이 크기(또는 사용자가
        // 리사이즈한 크기)로 고정한다 - 자막 길이가 바뀔 때마다 창이 같이
        // 흔들리는 걸 막기 위함. 내용이 이 크기를 넘으면 LazyColumn이 자동
        // 스크롤해서 오래된 줄이 위로 밀려 나간다(잘리는 게 아니라 스크롤됨).
        private const val DEFAULT_WIDTH_DP = 260
        private const val DEFAULT_HEIGHT_DP = 120
        // 최소화됐을 때 실제 창(터치 가능 영역) 크기. 지금까지는 최소화해도
        // 창 크기가 기본값 그대로라, 화면상 안 보이는 빈 공간이 여전히 터치를
        // 가로채서 밑에 있는 다른 앱을 못 눌렀다. 이제 최소화 시 이 작은
        // 크기로 실제로 줄여서 그 문제를 없앤다.
        private const val MINIMIZED_SIZE_DP = 48
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null

    // Lifecycle은 한 번 DESTROYED가 되면 같은 인스턴스로는 다시 살릴 수 없다
    // (Android의 정책). OverlayController 자체는 @Singleton이라 앱 프로세스
    // 동안 계속 살아있지만, 그 안의 Lifecycle/ViewModelStore/SavedStateRegistry는
    // show()가 다시 호출될 때마다 "이번 표시 세션"용으로 새로 만든다.
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
    private val defaultWidthPx = (DEFAULT_WIDTH_DP * density).toInt()
    private val defaultHeightPx = (DEFAULT_HEIGHT_DP * density).toInt()
    private val minimizedSizePx = (MINIMIZED_SIZE_DP * density).toInt()
    // Application Context의 resources.displayMetrics.widthPixels는 실제 화면
    // 크기와 어긋나는 경우가 있어서(오버레이처럼 특정 디스플레이에 안 묶인
    // 컨텍스트에서 특히), WindowManager가 실제로 알고 있는 크기를 대신 쓴다.
    private val screenWidthPx: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        windowManager.currentWindowMetrics.bounds.width()
    } else {
        @Suppress("DEPRECATION")
        Point().also { windowManager.defaultDisplay.getRealSize(it) }.x
    }

    // 창은 항상 고정 크기라, Compose 쪽도 항상 구체적인 크기를 갖고 시작한다
    // (더 이상 null=WRAP_CONTENT 같은 특수 상태가 없다).
    private val windowSizeState = mutableStateOf(DpSize(DEFAULT_WIDTH_DP.dp, DEFAULT_HEIGHT_DP.dp))

    private val params = WindowManager.LayoutParams(
        defaultWidthPx,
        defaultHeightPx,
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
        // 이제 초기 크기(defaultWidthPx)를 정확히 알기 때문에, 화면 중앙에
        // 오도록 x를 정확하게 계산할 수 있다 (예전엔 WRAP_CONTENT라 실제
        // 크기를 몰라서 최소 크기로 대충 추정했었다).
        x = (screenWidthPx - defaultWidthPx) / 2
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
                    val showOriginalText by displayPreferenceRepository.showOriginalText.collectAsState()

                    SubtitleOverlay(
                        state = uiState,
                        isLocked = isLocked,
                        isMinimized = isMinimized,
                        fixedSize = windowSize,
                        showOriginalText = showOriginalText,
                        onDrag = { dx, dy -> if (!isLocked) updatePosition(dx, dy) },
                        onResize = { dx, dy -> updateSize(dx, dy) },
                        onToggleLock = { isLockedState.value = !isLockedState.value },
                        onToggleMinimize = {
                            val nowMinimized = !isMinimizedState.value
                            isMinimizedState.value = nowMinimized
                            if (nowMinimized) {
                                applyMinimizedSize()
                            } else {
                                resetSizeToDefault()
                            }
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
            val newWidthPx = (params.width + dw.toInt()).coerceAtLeast(minWidthPx)
            val newHeightPx = (params.height + dh.toInt()).coerceAtLeast(minHeightPx)

            // gravity가 START(왼쪽 기준)라, 폭만 키우면 왼쪽 가장자리는 자동으로
            // 고정되고 오른쪽으로만 늘어난다 - x를 따로 건드릴 필요가 없다.
            params.width = newWidthPx
            params.height = newHeightPx
            windowManager.updateViewLayout(view, params)

            windowSizeState.value = DpSize((newWidthPx / density).dp, (newHeightPx / density).dp)
        }
    }

    private fun applyMinimizedSize() {
        composeView?.let { view ->
            // 그냥 크기만 줄이면 왼쪽 상단 기준으로 줄어들어서, 최소화된
            // 아이콘이 예전 박스의 왼쪽 위 구석에 붙어버린다. 대신 예전 박스의
            // 가로 중앙을 계산해서, 그 중앙에 맞춰 작아지도록 x를 보정한다
            // (위쪽 기준은 그대로 유지 - "가운데 상단"으로 온다).
            val previousCenterX = params.x + params.width / 2
            params.x = previousCenterX - minimizedSizePx / 2

            params.width = minimizedSizePx
            params.height = minimizedSizePx
            windowManager.updateViewLayout(view, params)
            windowSizeState.value = DpSize(MINIMIZED_SIZE_DP.dp, MINIMIZED_SIZE_DP.dp)
        }
    }

    private fun resetSizeToDefault() {
        composeView?.let { view ->
            // 최소화 때와 대칭되는 보정. 그냥 크기만 키우면 최소화 아이콘의
            // 왼쪽 상단 기준으로 커져서 비대칭으로 보인다. 대신 최소화 아이콘의
            // 가로 중앙을 기준으로 커지도록 x를 보정한다.
            val previousCenterX = params.x + params.width / 2
            params.x = previousCenterX - defaultWidthPx / 2

            params.width = defaultWidthPx
            params.height = defaultHeightPx
            windowManager.updateViewLayout(view, params)
            windowSizeState.value = DpSize(DEFAULT_WIDTH_DP.dp, DEFAULT_HEIGHT_DP.dp)
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