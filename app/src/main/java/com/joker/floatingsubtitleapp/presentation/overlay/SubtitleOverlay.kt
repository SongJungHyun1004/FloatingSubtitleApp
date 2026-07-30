package com.joker.floatingsubtitleapp.presentation.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joker.floatingsubtitleapp.domain.model.SubtitleUiState

@Composable
fun SubtitleOverlay(
    state: SubtitleUiState,
    onDrag: (Float, Float) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .wrapContentSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
            .background(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        // 각 줄을 고유 id로 key() 처리 -> 리스트 전체가 아니라
        // "이 줄 하나"에 대해서만 등장/퇴장 애니메이션이 걸린다.
        // 위쪽 줄이 shrinkVertically로 줄어들면 아래 partial 텍스트는
        // Column의 일반적인 레이아웃 흐름에 따라 자연스럽게 위로 밀려 올라간다.
        state.finalizedLines.forEach { line ->
            key(line.id) {
                val visibleState = remember { MutableTransitionState(false) }
                // isExiting이 true가 되는 순간 목표 상태가 false로 바뀌면서
                // 퇴장 애니메이션이 재생된다. 실제 리스트에서 제거되는 건
                // SubtitleLineManager가 애니메이션 시간만큼 뒤에 처리한다.
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
}