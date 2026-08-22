package com.joker.floatingsubtitleapp.domain.model

/**
 * GetSubtitleFlowUseCase가 내보내는 이벤트.
 *
 * SpeechResult(Partial/Final)를 그대로 흘려보내지 않고 이 타입으로 변환해서
 * 내보내는 이유: Partial은 스로틀링/유실 허용 대상이고 Final은 절대 아니라는
 * 정책 차이를, 이걸 소비하는 쪽(SubtitleLineManager)이 신경 쓰지 않아도 되게
 * 하기 위함이다. 소비자는 그냥 "부분 갱신이냐 확정 추가냐"만 알면 된다.
 *
 * PartialUpdated는 번역문(text)과 원문(originalText)을 같이 담는다 -
 * "원문 같이 보기" 설정이 순수 UI 표시 여부만 결정하도록, 데이터는 항상
 * 둘 다 갖고 있게 한다.
 */
sealed interface SubtitleEvent {
    data class PartialUpdated(val text: String, val originalText: String) : SubtitleEvent
    data class LineFinalized(val line: SubtitleLine) : SubtitleEvent
}