# AutoVerdict

엔카(encar.com) 중고차 매물을 자동 분석해 사고이력·진단·점수를 한눈에 보여주는 Android 앱.

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-7F52FF?logo=kotlin)](https://kotlinlang.org/)
[![Min SDK](https://img.shields.io/badge/minSdk-26-blue)]()
[![Target SDK](https://img.shields.io/badge/targetSdk-36-blue)]()

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
| 평가 엔진 | TypeScript (화면에 보이지 않는 헤드리스 WebView 번들) |
| 빌드 | Gradle 8 + Kotlin DSL, KSP |
| 광고 | Google AdMob (Play Services Ads 23.6) |
| 최소/타겟 SDK | API 26 / API 36 |

## 아키텍처

### 하이브리드 구조

```
사용자 공유 → MainActivity → ResultRoute / ResultController
  → 저장한 차 → 24h 캐시 → CollectorWebView 순으로 원본 JSON 확보
    (CollectorWebView: 엔카 페이지에서 __PRELOADED_STATE__ + API 응답 수집)
  → RuleEngine (헤드리스 WebView, window.avEvaluate)
    → orchestrateMobile() + buildReport() → Android.onReport
  → ReportParser → EvaluationReport → Compose ResultScreen
  → 저장 → ResultRepository → Room DB
```

### 헤드리스 엔진 브리지

- **Native → JS:** `RuleEngine` → `WebViewEngineHost`가 `evaluateJavascript`로 `window.avEvaluate(requestId, rawJson)` 호출
- **JS → Native:** `EngineBridge`의 `@JavascriptInterface` 메서드 (`onReady`, `onReport`, `onEngineError`)
- **Type-safe wrapper:** `engine-bridge.ts`
- **계약:** `webview-bundle/src/report/types.ts` ↔ `report/EvaluationReport.kt` (함께 수정, `schemaVersion` 확인)

### 공유 코어 라이브러리

`webview-bundle`의 TypeScript는 `@core` alias로 저장소 안에 vendoring된 코어 평가 로직을 import (원본은 sibling `daksin-car` 저장소):
```
webview-bundle/src → @core → webview-bundle/src/core/
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
# 1) WebView 엔진 번들 (TS)
cd webview-bundle
npm install             # .npmrc가 공식 npm 레지스트리(registry.npmjs.org)로 고정
npm run build           # dist/engine.html + engine.js
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

# TypeScript 테스트 (Vitest) — 계약 JSON이 어댑터 결과와 다르면 실패
cd webview-bundle && npx vitest run

# 어댑터 변경 후 계약 JSON(app/src/test/resources/report/) 재생성
cd webview-bundle && npm run gen:contract
```

> ⚠️ Gradle `preBuild`가 WebView 번들을 빌드해 `webview-bundle/dist/`를 `app/src/main/assets/eval-ui/`로 복사합니다 (gitignored). npm 경로는 `-PnpmPath` 또는 `local.properties`의 `npmPath=`로 지정할 수 있습니다.

## 프로젝트 구조

```
.
├── app/
│   ├── src/main/kotlin/com/car/autoverdict/
│   │   ├── MainActivity.kt              # 공유 인텐트 파싱 + AppRoot 호스트
│   │   ├── collector/                   # CollectorWebView (엔카 수집)
│   │   ├── engine/                      # RuleEngine, WebViewEngineHost, EngineBridge
│   │   ├── report/EvaluationReport.kt   # 리포트 모델 + ReportParser
│   │   ├── result/                      # ResultRoute/Controller/Screen, sections/
│   │   ├── ui/navigation/               # Route, AppNavigator, AppRoot
│   │   ├── ui/components/               # Av* 공용 컴포넌트
│   │   ├── ui/screen/                   # 그 밖의 Compose 화면들
│   │   │   ├── AnalyzeScreen.kt
│   │   │   ├── SavedListScreen.kt
│   │   │   ├── CompareScreen.kt
│   │   │   ├── SettingsScreen.kt
│   │   │   ├── OnboardingScreen.kt
│   │   │   └── PrivacyPolicyScreen.kt
│   │   ├── ui/theme/                    # Compose 테마
│   │   ├── db/                          # Room (SavedCarEntity, CacheEntity)
│   │   └── util/                        # EncarUrl, ClipboardUtil
│   └── build.gradle.kts
├── webview-bundle/
│   ├── src/
│   │   ├── engine.ts                    # 헤드리스 엔진 진입점 (window.avEvaluate)
│   │   ├── engine-bridge.ts             # window.Android 타입 래퍼
│   │   ├── orchestrate-mobile.ts        # 평가 오케스트레이터
│   │   ├── report/                      # 리포트 어댑터 (도메인 문구, types.ts 계약)
│   │   └── core/                        # vendored 코어 (수정 금지)
│   ├── public/engine.html               # 엔진 페이지
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