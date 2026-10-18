# AutoVerdict

엔카(encar.com) 중고차 매물 링크 하나로 보험이력·성능점검·엔카진단을 점검하고 결론부터 알려주는 Android 앱.

현재 버전 **1.1.0** (versionCode 6) · minSdk 26 · targetSdk 36 · 패키지 `com.kcurve.autoverdict`
개인정보처리방침: https://pouu69.github.io/auto-verdict-aos/

> 엔카(SK엔카닷컴)의 공식 서비스가 아니며 제휴 관계도 없습니다. 점수와 판정은 공개 정보를 바탕으로 한 참고용입니다.

## 무엇을 해 주나요

- **12가지 기준 점검** — 보험이력 공개, 성능점검 공개, 엔카진단, 프레임 손상, 렌트·택시·영업용 이력, 전손·침수·도난, 소유자 변경, 자차보험 공백, 수리비 미확정, 보험 처리 금액, 가격 적정성, 오일 누유
- **판정·점수·결론 문장** — 피하세요 / 주의 / 괜찮아요 / 확인 필요 중 하나와 점수, 그리고 "사기 전에 2가지는 꼭 확인하세요" 같은 한 문장 결론
- **사기 전에 확인할 것** — 걸린 항목과 그 이유
- **이력 타임라인** — 사고·소유자 변경·보험 공백
- **교환·수리 부위** — 성능점검의 교환·수리 부위와 주요 장치 상태
- **판매자 질문** — 물어볼 질문을 복사하거나 공유
- **상세 근거 시트** — 항목을 누르면 판단 근거 원본을 보여줌
- **저장·비교** — 마음에 드는 차를 저장하고 2~3대를 나란히 비교. 판매 완료·삭제된 매물은 따로 안내

### 사용법

1. 브라우저에서 엔카 매물을 열고 **공유 → AutoVerdict** 를 고른다.
2. 또는 앱 홈에서 매물 링크를 **붙여넣기** 한다 (클립보드는 버튼을 누를 때만 읽음).

분석한 차 정보는 기기 안에만 저장되며 자체 서버는 없습니다. 광고는 Google AdMob 배너입니다.

## 기술 스택

| 영역 | 사용 기술 |
|---|---|
| 앱 | Kotlin 2.3, Jetpack Compose (Material 3), AGP 9.2, Gradle 9.6, KSP |
| 데이터 | Room 2.8, kotlinx.serialization |
| 평가 엔진 | TypeScript 5.6 + Vite 5 번들을 헤드리스 WebView에서 실행, 테스트는 Vitest |
| 광고 | Google Play Services Ads (AdMob) |
| 테스트 | JUnit 4, Robolectric, Compose UI Test |

## 아키텍처

화면은 모두 Compose입니다. TypeScript 번들은 화면에 보이지 않는 WebView에서 평가 엔진으로만 돌아갑니다.

```
엔카 링크 공유 / 붙여넣기
  → MainActivity(ShareIntentParser) → AppRoot → ResultRoute → ResultController
    → 원본 JSON 확보: 저장한 차(이후 백그라운드 갱신) → 24h 캐시 → CollectorWebView
        (엔카 페이지의 __PRELOADED_STATE__ + 이력/진단/성능점검 API 수집,
         판매 완료 페이지나 기본 정보 API 404는 삭제된 매물로 처리)
    → RuleEngine → window.avEvaluate(requestId, rawJson)        [헤드리스 WebView]
        normalizeInput() → orchestrateMobile() (vendored core) → buildReport() (report 어댑터)
    → Android.onReport(requestId, json) → ReportParser → EvaluationReport
    → Compose ResultScreen (result/sections/*)
    → 저장 → ResultRepository → Room (SavedCarKey 기준)
```

- **엔진 브리지** — `RuleEngine`이 off-screen `WebViewEngineHost`(`eval-ui/engine.html`)를 한 번 만들어 재사용합니다. Native → JS는 `window.avEvaluate`, JS → Native는 `EngineBridge`(`onReady`/`onReport`/`onEngineError`), TS 쪽 래퍼는 `engine-bridge.ts`. 요청마다 `requestId`가 있고 초기화·요청 타임아웃은 각각 10초입니다.
- **입력 보정** — 엔카가 가끔 내려주는 잘못된 모양의 데이터는 `webview-bundle/src/normalize-input.ts`에서 고칩니다. 코어에서 고치지 않습니다.
- **vendored 코어 (수정 금지)** — 판정·점수 로직은 `webview-bundle/src/core/`(`@core` alias)에 있습니다. sibling `daksin-car` 저장소에서 가져온 사본이며, 문구는 코어가 아니라 `webview-bundle/src/report/` 어댑터에서 고칩니다.
- **계약 JSON** — `webview-bundle/src/report/types.ts`와 `report/EvaluationReport.kt`는 서로를 그대로 옮긴 계약입니다. 함께 고치고, 호환이 깨지면 `SCHEMA_VERSION`을 올립니다. 어댑터 출력은 `app/src/test/resources/report/encar-*.json`으로 생성돼 Kotlin 쪽 테스트가 읽습니다.
- **문구 분리** — 결론·규칙 설명·판매자 질문·타임라인·공유 문구는 TS 어댑터가 만들고, 섹션 제목·버튼·오류 같은 화면 문구는 `res/values/strings*.xml`에 둡니다.

## 데이터

| 엔티티 | 내용 |
|---|---|
| `SavedCarEntity` | 저장한 차 — 점수·판정·카운트 + 수집 원본 `rawJson` |
| `CacheEntity` | 24시간 캐시 — `rawInputJson`을 열 때마다 다시 평가 (최근 본 차 목록도 여기서) |

- 둘 다 `carId` PK + `@Upsert`. 리포트 자체는 저장하지 않고 항상 엔진으로 다시 계산합니다.
- 저장 키는 `SavedCarKey` — 엔카의 원본 차량 id(`vehicleId`)를 우선 쓰므로 같은 차의 더미 광고 id 매물도 하나로 묶입니다.
- `AppDatabase`는 `fallbackToDestructiveMigration()`을 씁니다. **실제 마이그레이션 없이 스키마(버전)를 바꾸면 저장한 차가 지워집니다.** DAO 쿼리 추가는 자유롭게 해도 됩니다.

## 디자인 시스템

토스 스타일, 라이트 모드 전용. 토큰은 `AvTheme.colors / typography / shapes / spacing`(`ui/theme`), 공용 컴포넌트는 `ui/components`의 `Av*`. 화면에서 raw 색상을 쓰지 않습니다. 자세한 규칙은 [DESIGN.md](./DESIGN.md).

## 빌드·실행

### 준비물

- Android Studio (내장 JBR을 `JAVA_HOME`으로 사용, JDK 17 이상)와 Android SDK
- Node.js 18 이상 + npm (`webview-bundle/.npmrc`가 registry.npmjs.org로 고정)

### 전체 빌드

```bash
# npm ci → 번들 빌드 → TS 테스트 → assembleDebug → Kotlin 테스트
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./build-all.sh
```

`build-all.sh`는 macOS 기준으로 `ANDROID_HOME=$HOME/Library/Android/sdk`를 씁니다. 결과물은 `app/build/outputs/apk/debug/app-debug.apk`.

### 부분 빌드

```bash
# WebView 엔진 번들
cd webview-bundle
npm ci
npm run build            # tsc + vite → dist/engine.html, dist/engine.js
npm run copy-to-assets   # dist/ → app/src/main/assets/eval-ui/ (git 제외)

# Android
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:assembleDebug
```

Gradle `preBuild`가 `npm run build`와 `copy-to-assets`를 자동으로 실행하므로 `assembleDebug`(Android Studio Run 포함)는 항상 최신 엔진을 담습니다. npm을 못 찾으면 `-PnpmPath=...` 또는 `local.properties`의 `npmPath=`로 지정하세요.

### 테스트

```bash
./gradlew :app:testDebugUnitTest          # Kotlin 단위·Compose 테스트 (Robolectric)
cd webview-bundle && npx vitest run       # TS 테스트 — 계약 JSON이 낡았으면 실패
cd webview-bundle && npm run gen:contract # 어댑터 변경 후 계약 JSON 재생성 (결과도 커밋)
```

### 릴리스

`keystore.properties`(템플릿: `keystore.properties.template`)가 있으면 release 빌드가 서명됩니다. 키스토어 생성, `./gradlew :app:bundleRelease`, 서명 검증은 [docs/release-guide.md](./docs/release-guide.md), Play 게시 절차는 [docs/play-publishing.md](./docs/play-publishing.md)를 보세요.

### 오픈소스 고지

의존성을 바꾸면 `scripts/oss-notices/generate.py`로 `app/src/main/res/raw/oss_notices.json`, `oss_google_notices.json`을 다시 만들어 커밋합니다 (오프라인, Gradle 캐시 사용). 절차는 [scripts/oss-notices/README.md](./scripts/oss-notices/README.md). 직접 의존성이 빠지면 `OssNoticesTest`가 실패합니다.

## 프로젝트 구조

```
.
├── app/
│   ├── src/main/kotlin/com/car/autoverdict/
│   │   ├── AutoVerdictApp.kt, MainActivity.kt
│   │   ├── collector/        # CollectorWebView, ListingAvailability (엔카 수집·삭제 판정)
│   │   ├── engine/           # RuleEngine, WebViewEngineHost, EngineBridge
│   │   ├── report/           # EvaluationReport + ReportParser (계약 모델)
│   │   ├── result/           # ResultRoute/Controller/Screen, SavedCarKey, sections/
│   │   ├── home/             # 홈, 최근 본 차, 공유 안내 시트
│   │   ├── onboarding/       # 온보딩 (첫 실행 / 설정에서 다시 보기)
│   │   ├── saved/            # SavedCarsRepository, SavedIdentity, SavedListModel
│   │   ├── compare/          # CompareController, CompareTable
│   │   ├── licenses/         # 오픈소스 고지 로더
│   │   ├── preferences/      # AppPreferences
│   │   ├── db/               # Room (AppDatabase, 엔티티, DAO)
│   │   ├── ui/
│   │   │   ├── navigation/   # Route, AppNavigator, AppRoot, ShareIntentParser
│   │   │   ├── screen/       # 저장 목록·비교·설정·라이선스·개인정보 화면
│   │   │   ├── saved/        # 저장 목록 행, 정렬 시트
│   │   │   ├── components/   # Av* 공용 컴포넌트
│   │   │   ├── theme/        # 디자인 토큰
│   │   │   └── gallery/      # 컴포넌트 갤러리 (디버그 빌드 전용)
│   │   └── util/             # EncarUrl, ClipboardUtil
│   ├── src/test/             # 단위·Compose 테스트, resources/report/ 계약 JSON
│   ├── schemas/              # Room 스키마
│   └── build.gradle.kts
├── webview-bundle/
│   ├── src/
│   │   ├── engine.ts           # 엔진 진입점 (window.avEvaluate)
│   │   ├── engine-bridge.ts    # window.Android 타입 래퍼
│   │   ├── normalize-input.ts  # 입력 보정
│   │   ├── orchestrate-mobile.ts
│   │   ├── report/             # 리포트 어댑터 (도메인 문구, types.ts 계약)
│   │   └── core/               # vendored 코어 (수정 금지)
│   ├── tests/                  # vitest (contract/, report/, fixtures/)
│   └── public/engine.html
├── scripts/oss-notices/        # 오픈소스 고지 생성기
├── docs/                       # 스토어·게시·릴리스 문서, assets/, legal/
├── build-all.sh                # 전체 빌드 + 테스트
├── run-emulator.sh             # 디버그 APK 빌드 후 에뮬레이터 실행
└── DESIGN.md · CLAUDE.md · RELEASE_NOTES.md
```

## 문서

| 문서 | 내용 |
|---|---|
| [DESIGN.md](./DESIGN.md) | 디자인 언어·토큰·컴포넌트 규칙 |
| [CLAUDE.md](./CLAUDE.md) | 아키텍처와 코드 규칙 상세 |
| [RELEASE_NOTES.md](./RELEASE_NOTES.md) | 버전별 변경 내역과 Play 출시 노트 |
| [docs/play-publishing.md](./docs/play-publishing.md) | Google Play 게시 총정리 (기준 문서) |
| [docs/release-guide.md](./docs/release-guide.md) | 키스토어, AAB 빌드·서명 검증 |
| [docs/store-listing.md](./docs/store-listing.md) | 스토어 문구, 데이터 보안 응답 근거 |
| [docs/privacy-policy.md](./docs/privacy-policy.md) | 개인정보처리방침 원문 (공개용 HTML: `docs/legal/index.html`) |
| [docs/assets-guide.md](./docs/assets-guide.md) | 아이콘·피처 그래픽·스크린샷 제작 |
| [scripts/oss-notices/README.md](./scripts/oss-notices/README.md) | 오픈소스 고지 재생성 |

`docs/play-store-checklist.md`, `docs/play-console-walkthrough.md`는 v1.0.2 시절의 옛 문서입니다. 설계 기록은 `docs/superpowers/`에 있습니다.
