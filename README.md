# AR 실시간 번역 앱

> Android · Kotlin · Jetpack Compose  
> AR 카메라 번역 + 음성 통역 + 여행 회화 올인원 앱

---

## 📱 주요 기능

| 기능 | 설명 |
|------|------|
| 🔍 AR 번역 | 카메라로 텍스트를 비추면 실시간 번역 말풍선 오버레이 |
| 📝 텍스트 번역 | 직접 입력한 텍스트 번역 (언어 자동 감지) |
| 🎤 음성 통역 | 실시간 양방향 음성 통역 |
| 📚 여행 회화 | 다운로드된 언어별 오프라인 회화집 + TTS 발음 |
| 🌐 언어 관리 | ML Kit 오프라인 모델 다운로드/삭제 |
| 🎨 4가지 테마 | 기본/비즈니스/여행/유학 테마 |

---

## 🏗️ 프로젝트 구조

```
camera_translation/
├── app/                        # 진입점, MainActivity, Navigation, DI
├── core/
│   ├── ui/                     # Material3 테마, 색상 시스템, 공통 컴포넌트
│   ├── translation/            # TranslationRepository (Cloud + ML Kit)
│   └── database/               # Room DB, PhraseDao, 데이터 시더
└── feature/
    ├── ar/                     # AR 카메라 번역 (CameraX + OverlayView)
    ├── text/                   # 텍스트 번역 화면
    ├── voice/                  # 음성 통역 화면
    ├── phrasebook/             # 여행 회화 화면
    └── language/               # 언어 다운로드 관리
```

---

## 🚀 시작하기

### 1. API 키 설정

```bash
cp local.properties.example local.properties
# local.properties 파일에서 TRANSLATION_API_KEY 값을 실제 키로 교체
```

Google Cloud Translation API 키는 [Google Cloud Console](https://console.cloud.google.com/apis/credentials)에서 발급받으세요.

> ⚠️ **보안 주의**: `local.properties`는 절대 git에 커밋하지 마세요. `.gitignore`에 포함되어 있습니다.

### 2. Android Studio에서 열기

```
File → Open → camera_translation 폴더 선택
```

### 3. 빌드 및 실행

```bash
./gradlew assembleDebug
```

---

## 🎨 테마 시스템

앱 설정(우측 상단 ⚙️)에서 4가지 테마를 선택할 수 있습니다.

| 테마 | 배경색 | 강조색 | 특징 |
|------|--------|--------|------|
| **기본** | 딥 다크 `#0D0D0D` | 퍼플 `#534AB7` | 다크 모드, 야간 최적화 |
| **비즈니스** | 딥 네이비 `#0D1520` | 코발트 `#4A7FCC` | 비즈 회화, 전문적 |
| **여행** | 딥 다크 `#0D0D0D` | 앰버 골드 `#D4A847` | 여행 회화, 빈티지 감성 |
| **유학** | 민트 그린 `#F0FAF5` | 틸 `#1D9E75` | 학교 회화, 라이트 모드 |

각 테마는 **AR 오버레이 말풍선 색상**, **하단 탭 이름**(비즈/여행/학교)도 함께 변경됩니다.

---

## 🔧 기술 스택

- **UI**: Jetpack Compose + Material3
- **DI**: Hilt
- **Navigation**: Navigation Compose
- **Camera**: CameraX
- **OCR**: ML Kit Text Recognition (라틴/한중일)
- **온라인 번역**: Google Cloud Translation API v2
- **오프라인 번역**: ML Kit Translate (58개 언어)
- **언어 감지**: ML Kit Language Identification
- **음성 인식**: Android SpeechRecognizer API
- **TTS**: Android TextToSpeech
- **DB**: Room + Flow
- **설정 저장**: DataStore Preferences
- **네트워크**: Retrofit + OkHttp

---

## 📋 필요 권한

| 권한 | 용도 |
|------|------|
| `CAMERA` | AR 카메라 번역 |
| `RECORD_AUDIO` | 음성 통역 |
| `INTERNET` | 온라인 번역 API |
| `ACCESS_NETWORK_STATE` | 온/오프라인 감지 |

---

## ⚡ 성능 최적화

- **AR 번역 디바운스**: 300ms 간격으로 OCR 실행 (배터리 절약)
- **번역 캐시**: `LruCache<String, String>` 200개 항목 (중복 API 호출 방지)
- **OverlayView**: `onDraw()`에서 객체 생성 없음 — Paint/Path 사전 초기화
- **ML Kit 모델**: 앱 시작 시 백그라운드 워밍업
- **ImageAnalysis**: `STRATEGY_KEEP_ONLY_LATEST` 전략으로 프레임 드롭 방지

---

## 🗺️ 개발 로드맵

- [x] 멀티모듈 프로젝트 구조 설정
- [x] Material3 테마 시스템 (4가지 테마)
- [x] TranslationRepository (Cloud + ML Kit 추상화)
- [x] AR 번역 화면 (CameraX + OverlayView)
- [x] 텍스트 번역 화면
- [x] 음성 통역 화면
- [x] 여행 회화 화면 (Room DB)
- [x] 언어 다운로드 관리 화면
- [ ] 단위 테스트 (MockK)
- [ ] Firebase Remote Config로 API 키 관리
- [ ] 사용자 번역 히스토리

---

## 📄 라이선스

본 프로젝트는 개인 프로젝트입니다.
