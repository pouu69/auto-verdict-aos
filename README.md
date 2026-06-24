# AutoVerdict

엔카(encar.com) 중고차 매물을 자동 분석해 사고이력·진단·점수를 한눈에 보여주는 Android 앱.

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-7F52FF?logo=kotlin)](https://kotlinlang.org/)
[![Min SDK](https://img.shields.io/badge/minSdk-26-blue)]()
[![Target SDK](https://img.shields.io/badge/targetSdk-35-blue)]()

## 핵심 기능

- **URL 한 번으로 자동 분석** — 엔카 매물 URL을 입력하거나 공유 인텐트로 받아 즉시 평가
- **12가지 규칙 기반 종합 점수** — 사고이력·정비기록·진단 정보를 가중치로 평가해 0~100점 산출
- **위험/주의/양호 분류** — 매물 신호를 색상으로 그룹화해 빠른 의사결정 지원
- **2대 비교** — 저장한 매물을 나란히 놓고 Hero 카드 + 섹션별 메트릭으로 비교
- **24시간 캐시** — 같은 매물을 다시 열 때 네트워크 없이 즉시 표시

## 기술 스택

| 영역 | 사용 기술 |
|---|---|
| Language | Kotlin 2.x |
| UI | Jetpack Compose, Material 3, Material Icons Extended |
| Database | Room 2.6 (`@Upsert`) |
| Web 평가 엔진 | React + TypeScript (WebView 번들) |
| 빌드 | Gradle 8 + Kotlin DSL, KSP |
| 광고 | Google AdMob (Play Services Ads 23.6) |
| 최소/타겟 SDK | API 26 / API 35 |

## 아키텍처

### 하이브리드 구조

```
사용자 공유 → MainActivity
  → CollectorWebView (엔카 페이지에서 __PRELOADED_STATE__ + API 응답 수집)
    → JSON → EvalWebView (window.receiveEncarData)
      → React App.tsx → orchestrateMobile() → 규칙 엔진 → 결과 렌더
      → Android.saveCar(json) → NativeBridge → Room DB
```

### Native ↔ WebView 브리지

- **Native → JS:** `EvalWebView.sendData(json)` → `evaluateJavascript`로 `window.receiveEncarData()` 호출
- **JS → Native:** `NativeBridge`의 `@JavascriptInterface` 메서드 (`saveCar`, `closeOverlay`, `showToast`)
- **Type-safe wrapper:** `android-bridge.ts`

### 공유 코어 라이브러리

`webview-bundle`의 TypeScript는 `@core` alias로 sibling 저장소의 코어 평가 로직을 import:
```
webview-bundle/src → @core → ../../daksin-car/src/core/
```

`ParsedData`, `ChecklistFacts`, `encarToFacts()` 파서, 규칙 정의, 평가 엔진 포함.

### 데이터 모델 (Room)

| 엔티티 | 설명 |
|---|---|
| `SavedCarEntity` | 사용자 저장 매물 (점수·판정·카운트 포함) |
| `CacheEntity` | 24h TTL의 원본 분석 캐시 |

둘 다 `carId` PK + `@Upsert` 사용.

## 빌드 & 실행

### 사전 준비

- Android Studio Ladybug+ (또는 JBR JDK 17)
- Node.js 20+ (webview-bundle 빌드용)

### 전체 빌드 파이프라인

```bash
# webview-bundle → Android APK → 테스트
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./build-all.sh
```

### 부분 빌드

```bash
# 1) WebView 번들 (React/TS)
cd webview-bundle
npm install
npm run build           # 빌드
npm run copy-to-assets  # dist/ → app/src/main/assets/eval-ui/

# 2) Android Debug APK
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:assembleDebug

# 3) Android Release (서명 필요, docs/release-guide.md 참고)
./gradlew :app:bundleRelease    # → app/build/outputs/bundle/release/app-release.aab
./gradlew :app:assembleRelease  # → app/build/outputs/apk/release/app-release.apk
```

### 테스트

```bash
# Kotlin 단위 테스트
./gradlew :app:testDebugUnitTest

# TypeScript 테스트 (Vitest)
cd webview-bundle && npx vitest run
```

> ⚠️ **WebView 번들은 Android 빌드 전에 반드시 한 번 빌드해야 합니다.** Gradle이 `webview-bundle/dist/`를 `app/src/main/assets/eval-ui/`로 복사합니다 (gitignored).

## 프로젝트 구조

```
.
├── app/
│   ├── src/main/kotlin/com/car/autoverdict/
│   │   ├── MainActivity.kt              # 라우터 (AppScreen enum + when)
│   │   ├── collector/CollectorWebView.kt
│   │   ├── webview/{EvalWebView,NativeBridge}.kt
│   │   ├── ui/screen/                   # Compose 화면들
│   │   │   ├── AnalyzeScreen.kt
│   │   │   ├── SavedListScreen.kt
│   │   │   ├── CompareScreen.kt
│   │   │   ├── ResultScreen.kt
│   │   │   ├── SettingsScreen.kt
│   │   │   ├── OnboardingScreen.kt
│   │   │   └── PrivacyPolicyScreen.kt
│   │   ├── ui/theme/                    # Compose 테마
│   │   ├── db/                          # Room (SavedCarEntity, CacheEntity)
│   │   └── util/                        # EncarUrl, JsEscape
│   └── build.gradle.kts
├── webview-bundle/
│   ├── src/
│   │   ├── App.tsx                      # 결과 UI
│   │   ├── orchestrate-mobile.ts        # 평가 오케스트레이터
│   │   ├── overlay/                     # UI 컴포넌트
│   │   └── android-bridge.ts            # @JavascriptInterface 타입 래퍼
│   └── package.json
├── docs/
│   ├── privacy-policy.md                # 공개용 정책
│   ├── store-listing.md                 # Play Store 카피
│   ├── release-guide.md                 # 서명·AAB·업로드 가이드
│   ├── assets-guide.md                  # 스크린샷·아이콘 캡처 가이드
│   ├── play-store-checklist.md          # 출시 마스터 체크리스트
│   └── assets/                          # 512x512 아이콘, 1024x500 피처 그래픽
└── build-all.sh                         # 전체 빌드 헬퍼
```