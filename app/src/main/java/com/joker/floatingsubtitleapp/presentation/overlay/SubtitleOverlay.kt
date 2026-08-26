package com.joker.floatingsubtitleapp.presentation.overlay

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    fixedSize: DpSize,
    showOriginalText: Boolean,
    onDrag: (Float, Float) -> Unit,
    onResize: (Float, Float) -> Unit,
    onToggleLock: () -> Unit,
    onToggleMinimize: () -> Unit,
    onReturnToApp: () -> Unit,
    onStopService: () -> Unit
) {
    if (isMinimized) {
        MinimizedBar(isLocked = isLocked, onDrag = onDrag, onRestore = onToggleMinimize)
        return
    }

    Box(modifier = Modifier.size(fixedSize)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isLocked) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        if (!isLocked) onDrag(dragAmount.x, dragAmount.y)
                    }
                }
                .clip(RoundedCornerShape(8.dp))
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

            val listState = rememberLazyListState()

            // 새 확정 줄이 추가될 때마다 부드럽게 최신 위치로 스크롤한다.
            // animateScrollToItem은 내부 애니메이션 속도를 조절할 수 없어서
            // 대신 animateScrollBy + tween으로 "항상 일정한 시간(400ms) 동안
            // 부드럽게" 움직이게 한다. value를 넉넉히 크게 주면 실제 남은
            // 스크롤 거리에서 자동으로 멈춰서, 매번 정확히 맨 아래까지 간다.
            LaunchedEffect(state.finalizedLines.size) {
                if (state.finalizedLines.isNotEmpty()) {
                    listState.animateScrollBy(
                        value = 10_000f,
                        animationSpec = tween(durationMillis = 400)
                    )
                }
            }

            LazyColumn(
                state = listState,
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(state.finalizedLines, key = { it.id }) { line ->
                    Column {
                        Text(
                            text = line.text,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        if (showOriginalText && line.originalText.isNotBlank()) {
                            Text(
                                text = line.originalText,
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            if (state.partialText.isNotEmpty()) {
                Text(
                    text = state.partialText,
                    color = Color.LightGray,
                    fontSize = 18.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showOriginalText && state.partialOriginalText.isNotBlank()) {
                    Text(
                        text = state.partialOriginalText,
                        color = Color.LightGray.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth()
    ) {
        // 윈도우 창의 "최소화"가 아니라 "창 크기를 줄인다/키운다"는 느낌을
        // 주려고 확대/축소 계열 아이콘을 쓴다 (CloseFullscreen = 축소,
        // OpenInFull = 확대). 복원 버튼(MinimizedBar)과 짝을 이룬다.
        OverlayIconButton(icon = Icons.Default.CloseFullscreen, contentDescription = "최소화", onClick = onToggleMinimize)
        OverlayIconButton(
            icon = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
            contentDescription = "위치 고정",
            onClick = onToggleLock
        )
        OverlayIconButton(icon = Icons.Default.Home, contentDescription = "앱으로 돌아가기", onClick = onReturnToApp)
        OverlayIconButton(icon = Icons.Default.PowerSettingsNew, contentDescription = "서비스 종료", onClick = onStopService)
    }
}

@Composable
private fun OverlayIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(28.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun MinimizedBar(
    isLocked: Boolean,
    onDrag: (Float, Float) -> Unit,
    onRestore: () -> Unit
) {
    Box(
        modifier = Modifier
            .wrapContentSize()
            .pointerInput(isLocked) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    if (!isLocked) onDrag(dragAmount.x, dragAmount.y)
                }
            }
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.6f))
    ) {
        IconButton(onClick = onRestore, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.OpenInFull,
                contentDescription = "복원",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}