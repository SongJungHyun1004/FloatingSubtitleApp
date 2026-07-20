# 프로젝트: 실시간 플로팅 번역 자막 앱

## 목표
- 삼성 실시간 자막(Live Caption)에는 번역 기능이 없음 → 이를 대체하는 앱
- 파이프라인: MediaProjection/AudioPlaybackCapture(오디오 캡처)
  → 온디바이스 STT → ML Kit 번역 → SYSTEM_ALERT_WINDOW 오버레이 자막

## 기술 스택
- Kotlin, Jetpack Compose, MVVM + Clean Architecture
- Hilt, Coroutines/Flow
- ML Kit Translation (온디바이스)
- Whisper(TFLite/ONNX) 또는 대안 STT 엔진

## 제약사항
- Android 14+: FOREGROUND_SERVICE_MEDIA_PROJECTION 필수
- Android 15+: MediaProjection 토큰 세션 캐싱 불가, 매번 재동의 필요
- DRM 보호 콘텐츠는 캡처 불가 (OTT 앱 제외 대상)
- minSdk/targetSdk: [프로젝트에 맞게 명시]

## 코딩 컨벤션
- [기존에 쓰던 패키지 구조, 네이밍 규칙 등 기재]