package com.joker.floatingsubtitleapp.domain.model

/**
 * GetSubtitleFlowUseCase가 내보내는 이벤트.
 *
 * SpeechResult(Partial/Final)를 그대로 흘려보내지 않고 이 타입으로 변환해서
 * 내보내는 이유: Partial은 스로틀링/유실 허용 대상이고 Final은 절대 아니라는
 * 정책 차이를, 이걸 소비하는 쪽(SubtitleLineManager)이 신경 쓰지 않아도 되게
 * 하기 위함이다. 소비자는 그냥 "부분 갱신이냐 확정 추가냐"만 알면 된다.
 */
sealed interface SubtitleEvent {
    data class PartialUpdated(val text: String) : SubtitleEvent
    data class LineFinalized(val line: SubtitleLine) : SubtitleEvent
}