package com.joker.floatingsubtitleapp.domain.model

/**
 * 확정(Final)된 자막 한 줄.
 *
 * @param id 이 줄만의 고유 식별자. Compose가 리스트 내 특정 항목을
 *           추적해서 개별 등장/퇴장 애니메이션을 걸 수 있게 해준다.
 * @param text 번역이 완료된 표시용 텍스트.
 * @param isExiting true가 되면 UI에서 퇴장 애니메이션을 시작해야 한다는 신호.
 *                  실제 리스트에서의 제거는 애니메이션이 끝난 뒤 별도로 일어난다.
 */
data class SubtitleLine(
    val id: String,
    val text: String,
    val isExiting: Boolean = false
)