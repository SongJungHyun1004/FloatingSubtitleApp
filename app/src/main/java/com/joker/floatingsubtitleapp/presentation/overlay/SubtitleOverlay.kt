package com.joker.floatingsubtitleapp.presentation.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joker.floatingsubtitleapp.domain.model.SubtitleUiState

@Composable
fun SubtitleOverlay(
    state: SubtitleUiState,
    isLocked: Boolean,
    isMinimized: Boolean,
    fixedSize: DpSize?,
    onDrag: (Float, Float) -> Unit,
    onResize: (Float, Float) -> Unit,
    onToggleLock: () -> Unit,
    onToggleMinimize: () -> Unit,
    onReturnToApp: () -> Unit,
    onStopService: () -> Unit
) {
    if (isMinimized) {
        MinimizedBar(onRestore = onToggleMinimize)
        return
    }

    // 리사이즈 전(fixedSize == null)에는 내용에 맞춰 자동 크기.
    // 리사이즈를 시작한 뒤(fixedSize != null)에는 실제 창 크기를 그대로 따라야
    // 리사이즈 핸들이 눈에 보이는 박스 경계와 같이 움직인다.
    val outerModifier = if (fixedSize != null) Modifier.size(fixedSize) else Modifier.wrapContentSize()
    val columnSizeModifier = if (fixedSize != null) Modifier.fillMaxSize() else Modifier.wrapContentSize()

    Box(modifier = outerModifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = columnSizeModifier
                .pointerInput(isLocked) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        if (!isLocked) onDrag(dragAmount.x, dragAmount.y)
                    }
                }
                .background(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(8.dp)
        ) {
            ControlRow(
                isLocked = isLocked,
                onToggleLock = onToggleLock,
                onToggleMinimize = onToggleMinimize,
                onReturnToApp = onReturnToApp,
                onStopService = onStopService
            )

            // 각 줄을 고유 id로 key() 처리 -> 리스트 전체가 아니라
            // "이 줄 하나"에 대해서만 등장/퇴장 애니메이션이 걸린다.
            state.finalizedLines.forEach { line ->
                key(line.id) {
                    val visibleState = remember { MutableTransitionState(false) }
                    visibleState.targetState = !line.isExiting

                    AnimatedVisibility(
                        visibleState = visibleState,
                        enter = fadeIn() + slideInVertically { fullHeight -> fullHeight / 3 },
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Text(
                            text = line.text,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            if (state.partialText.isNotEmpty()) {
                Text(
                    text = state.partialText,
                    color = Color.LightGray,
                    fontSize = 18.sp
                )
            }
        }

        // 우측 하단 리사이즈 핸들. 바깥 Column의 이동 드래그와 겹치지 않도록
        // 독립된 작은 영역에서 자기만의 드래그 제스처를 처리한다.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(2.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.5f))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onResize(dragAmount.x, dragAmount.y)
                    }
                }
        )
    }
}

@Composable
private fun ControlRow(
    isLocked: Boolean,
    onToggleLock: () -> Unit,
    onToggleMinimize: () -> Unit,
    onReturnToApp: () -> Unit,
    onStopService: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        modifier = Modifier.wrapContentSize()
    ) {
        OverlayIconButton(symbol = "🔽", onClick = onToggleMinimize)
        OverlayIconButton(symbol = if (isLocked) "🔒" else "🔓", onClick = onToggleLock)
        OverlayIconButton(symbol = "🏠", onClick = onReturnToApp)
        OverlayIconButton(symbol = "✕", onClick = onStopService)
    }
}

@Composable
private fun OverlayIconButton(symbol: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(28.dp)) {
        Text(text = symbol, fontSize = 14.sp, color = Color.White)
    }
}

@Composable
private fun MinimizedBar(onRestore: () -> Unit) {
    Box(
        modifier = Modifier
            .wrapContentSize()
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.6f))
    ) {
        IconButton(onClick = onRestore, modifier = Modifier.size(32.dp)) {
            Text(text = "🔼", fontSize = 14.sp, color = Color.White)
        }
    }
}