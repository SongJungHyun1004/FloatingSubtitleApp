# CONVENTIONS.md — Floating Subtitle App

## 1. 프로젝트 개요
다른 앱에서 재생 중인 오디오(예: 유튜브 영상)를 실시간으로 캡처하여
음성 인식 → 번역 → 화면 오버레이 자막으로 표시하는 안드로이드 앱.

- `applicationId`: `com.joker.floatingsubtitleapp`
- `namespace`: `com.joker.floatingsubtitleapp`

## 2. SDK / 빌드 설정
```
compileSdk = 36
minSdk = 29      // Android 10 (Q) 이상. AudioPlaybackCapture(API 29+) 필수 기능이라 이 아래는 지원 불가
targetSdk = 36
```
- **API 34(Android 14) 미만/이상 분기 지점은 딱 한 곳**, 포그라운드 서비스 시작부입니다.
```kotlin
if (Build.VERSION.SDK_INT >= 34) {
    startForeground(NOTIFICATION_ID, notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
} else {
    startForeground(NOTIFICATION_ID, notification)
}
```
- 매니페스트에는 `foregroundServiceType="mediaProjection"`을 그대로 선언해두면 됨(구버전에서는 시스템이 무시).

## 3. 아키텍처
Clean Architecture 3계층 + Hilt DI.
```
com.joker.floatingsubtitleapp/
├── data/           # 오디오 캡처, STT 엔진, 번역 API 구현체
│   ├── audio/
│   ├── stt/
│   └── translate/
├── domain/         # UseCase, Repository 인터페이스, 모델
│   ├── usecase/
│   ├── repository/
│   └── model/
├── presentation/   # Compose UI, ViewModel, 오버레이 서비스
│   ├── overlay/
│   ├── settings/
│   └── service/
└── di/             # Hilt 모듈
```

### 네이밍 규칙
- 클래스: UpperCamelCase (`AudioCaptureRepository`)
- 함수/변수: lowerCamelCase
- UseCase: `동사+명사+UseCase` (예: `StartAudioCaptureUseCase`)
- Repository 인터페이스는 `domain/repository`, 구현체는 `data/*/XxxRepositoryImpl`
- Compose 함수: `PascalCase` + 명사 (예: `SubtitleOverlay`)
- 리소스 파일: `snake_case`

## 4. 오디오 캡처
- **캡처 방식**: `MediaProjection` + `AudioPlaybackCaptureConfiguration` (API 29+)
- **캡처 대상**: 사용자가 캡처할 앱을 직접 선택 (전체 통합 캡처 아님)
  - 설치 앱 목록 조회 → 사용자가 선택 → 해당 패키지의 UID를 `addMatchingUid(uid)`로 필터링
  - `RECORD_AUDIO` 권한 필요 (재생 오디오 캡처여도 시스템 요구사항)
- **제약 사항 (사용자 안내 필요)**:
  - 대상 앱이 `android:allowAudioPlaybackCapture="false"`거나 `USAGE_ASSISTANT` 등으로 opt-out한 경우 캡처 불가 (Netflix 등 DRM 콘텐츠 앱 대부분 해당)
  - Android 15+에서는 MediaProjection 토큰이 캐싱되지 않으므로 캡처 세션마다 재동의 필요

## 5. STT (음성 인식)
- **엔진**: Vosk 스트리밍 (온디바이스)
- **선택 이유**: Whisper(TFLite)는 배치/청크 처리 방식이라 2~5초 지연 발생, 실시간 자막 목표에 부적합. Vosk는 ~100ms 단위 오디오 청크를 하나의 장수명 인식기에 흘려 넣어 부분 결과(partial result)를 즉시 출력 가능.
- **엔진 교체 가능하도록 인터페이스 추상화**:
```kotlin
interface SpeechEngine {
    fun start(languageCode: String)
    fun feed(pcmChunk: ByteArray)
    val partialResults: Flow<String>
    val finalResults: Flow<String>
    fun stop()
}
```
- **VAD(무음 구간 감지)**: MVP에서는 제외. 무음 구간에도 계속 STT 실행(구현 단순화 우선). 추후 배터리 최적화가 필요하면 별도 이슈로 추가.

## 6. 언어 설정
- **소스 언어**: 사용자가 앱 실행 전 설정 화면에서 수동 지정 (자동 감지 미지원)
  - 이유: 자동 감지는 판별 단계가 추가로 붙어 초기 지연이 늘고, 스트리밍 초반 오디오만으로는 오판 위험 있음. 대부분 콘텐츠는 단일 언어로 진행되어 자동 감지의 이점이 크지 않음.
- 언어 변경은 캡처 세션 재시작 시 반영 (도중 변경 UX는 MVP 범위 밖)

## 7. 번역
- **엔진**: ML Kit Translation (온디바이스)
- STT 결과(final result 기준)를 UseCase에서 받아 번역
- 언어 모델 미다운로드 시 자동 다운로드 처리, 진행 상태 UI에 노출

## 8. 오버레이 UI
- `SYSTEM_ALERT_WINDOW` 권한 기반 `TYPE_APPLICATION_OVERLAY`
- 자막 박스: **드래그로 위치 이동 가능**, 크기 조절 가능
- 박스 영역 바깥은 자동으로 터치 패스스루 (윈도우가 박스 크기만큼만 화면 차지하므로 별도 FLAG 처리 불필요)
- 기본 위치: 화면 하단 (재생 컨트롤 등 주요 UI 가리지 않도록)
- Compose 기반, ViewModel에서 `Flow<String>`으로 자막 텍스트 실시간 수신

## 9. 성능 목표
- 음성 발화 → 자막 표시까지 지연 **목표 1초 이내** (도전적 목표, 기기 성능/청크 크기에 따라 유동적일 수 있음을 문서화)
- 지연이 목표치를 초과할 경우 Vosk 청크 크기 조정 또는 부분 결과 표시 빈도 조정으로 대응

## 10. 예외 처리
- DRM 보호 콘텐츠 캡처 시도 → 캡처 실패 감지 후 사용자에게 "이 앱은 캡처를 지원하지 않습니다" 안내 메시지 표시 (크래시 방지)
- 오디오 캡처 세션이 대상 앱 종료/전환으로 끊긴 경우 → 서비스는 유지하되 캡처 재시작 유도

## 11. MVP 범위
8단계 전체(뼈대 → 권한/서비스 → 오디오 캡처 → STT → 번역 → 오버레이 UI → 파이프라인 연결 → 예외 처리)를 지금 설계 확정 후 순서대로 구현.