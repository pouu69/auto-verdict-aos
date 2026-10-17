# UX/UI 개편 1번 — 결과 화면 설계

- 작성일: 2026-10-17
- 브랜치: `feat/ux-revamp` (worktree `auto-verdict-aos-ux`, `main` f996862 기준)
- 선행: 0번 디자인 언어 & 정보 구조 (`2026-10-17-ux-revamp-0-design-language-design.md`)
- 상태: 설계 확정(스펙 리뷰 15건 반영), 구현 계획 작성 전

## 1. 목표

결과 화면을 React(WebView) UI에서 Compose로 옮기고, 판정 근거와 다음 행동을 한 화면에 펼쳐 보여준다.

- 결론 문장이 맨 위, 점수는 판정 옆에 중간 크기로 보여준다.
- 세부 정보는 접거나 시트로 숨기지 않는다. 스크롤만 하면 모두 보인다.
- 지금은 수집만 하고 쓰지 않는 데이터를 활용한다: 이력 타임라인, 교환·수리 부위, 장치 상태.
- 로딩·에러·삭제 매물·저장 흐름을 네이티브로 다시 만든다.

## 2. 결정 사항

| 항목 | 결정 | 근거 |
|---|---|---|
| 판정·점수 로직 | **바꾸지 않는다.** vendored core(`webview-bundle/src/core/`)는 수정 금지 | 사용자 결정 |
| 사실과 다른 문구 | 판정은 그대로 두고 **설명 문구만 사실대로** 쓴다(예: OK여도 비공개 항목이 있으면 그대로 알림) | 로직 불변 원칙 안에서 정확성 확보 |
| 점수 표시 | 판정 배지 옆 중간 크기(`title2`), 판정 톤 색, 아래 얇은 진행 바(판정 톤 fill) | 사용자 결정(결론 우선 + 적당한 점수) |
| 세부 정보 | 모두 펼친 상태. 아코디언·상세 시트 없음 | 사용자 결정(추가 액션 불필요) |
| 렌더링 | 규칙 엔진은 화면 없는 WebView에서 실행하고 결과를 `EvaluationReport` JSON으로 넘긴다. 화면은 Compose | 0번 8절 |
| 사용자 문구 위치 | 도메인 문구(결론 문장, 룰 결과 문장, 이유, 판매자 질문, 타임라인 라벨, 공유 텍스트)는 **앱 전용 TS 어댑터**가 만든다. 화면 크롬 문구(섹션 제목, 버튼, 배너, 오류)는 `strings.xml` | 도메인 문구는 데이터와 함께 vitest로 검증 |
| DB 스키마 | **변경 금지.** `fallbackToDestructiveMigration` 설정이라 스키마가 바뀌면 저장한 차가 지워진다 | 기존 컬럼(`cache.title/score/verdict`)으로 충분 |
| 추가 범위 | 저장 차 오프라인 재열기, 부분 데이터 안내, 엔진 타임아웃, 어댑터 문구 생성, 집계 호환, 교환·수리 부위, 사고 묶기, 최근 조회 정보, 엔진 재사용, 설명 문구 수정, 결과 요약 공유 | 사용자 선택(A1~5, B6~10, C11) |

## 3. 구조

```
엔카 URL
  → CollectorWebView (기존. 진행 단계 콜백 추가)
  → raw JSON (url, carId, preloadedState, recordJson, diagnosisJson, inspectionJson, httpStatus)
  → RuleEngine (앱 범위 단일 인스턴스, 화면 없는 WebView, engine 번들)
       window.avEvaluate(requestId, rawJson)
         → orchestrateMobile(raw)              ← vendored core, 수정 없음
         → buildReport(raw, orchestrated)      ← 앱 어댑터 (src/report/*)
       → Android.onReport(requestId, reportJson) | Android.onEngineError(requestId, message)
  → Kotlin EvaluationReport (kotlinx.serialization)
  → ResultController (상태 머신) → Compose ResultScreen
```

### 3.1 TS 쪽 (`webview-bundle/`)

| 경로 | 책임 |
|---|---|
| `src/engine.ts` | 엔트리. `window.avEvaluate`를 등록하고 준비되면 `Android.onReady()` 호출. 예외는 `onEngineError`로 |
| `src/engine-bridge.ts` | `window.Android`(onReady/onReport/onEngineError) 타입 안전 래퍼 |
| `src/report/types.ts` | `EvaluationReport` 타입(4절 계약과 1:1) |
| `src/report/build-report.ts` | 아래 모듈을 조합해 보고서 생성 |
| `src/report/rule-copy.ts` | 룰별 결과 문장·이유·판매자 질문 템플릿(기존 `rule-meta.ts` 대체·흡수). 룰 결과 + facts 수치로 해요체 문장 생성 |
| `src/report/facts.ts` | 핵심 사실 4칸 |
| `src/report/repairs.ts` | 교환·수리 부위(성능점검 `outers`), 주요 장치 상태(`inners`) |
| `src/report/timeline.ts` | 이력 타임라인(원본 `recordJson` 직접 읽기: `firstDate`, `ownerChanges`, `accidents`, `notJoinDate1..5`, `floodDate`, `totalLossDate`, `robberDate`, `carInfoChanges`) |
| `src/report/data-gaps.ts` | API 실패·로그인 필요 판별(`httpStatus` + 룰 unknown 사유) |
| `src/report/share-text.ts` | 결과 요약 공유 텍스트, 판매자 질문 공유 텍스트 |

- React, `App.tsx`, `overlay/*`, `android-bridge.ts`(UI 메서드), `verdict-meta.ts`, `tokens.ts`는 제거한다. `react`/`react-dom` 의존성도 제거한다.
- `orchestrate-mobile.ts`는 유지한다(어댑터 입력).
- 빌드 산출물은 기존처럼 `app/src/main/assets/eval-ui/`로 복사된다. `index.html`은 `engine.html`로 바꾸고 IIFE `engine.js`를 일반 `<script>`로 로드한다(3.3절).
- vitest 환경은 `jsdom` → `node`, React 테스트(`app-callbacks`, `overlay/*`, `android-bridge`)는 제거하고 어댑터 테스트로 대체한다.
- 픽스처: `tests/fixtures/encar-{39339524,41878112}.json`. 원본을 받아 식별 정보를 지우는 `scripts/scrub-fixture.mjs`를 함께 둔다(입력 파일 경로 → 출력 파일). 계약 JSON은 `npm run gen:contract`가 `app/src/test/resources/report/`에 쓴다.

### 3.2 Android 쪽

| 경로 | 책임 |
|---|---|
| `engine/RuleEngine.kt` | 앱 범위 단일 인스턴스(`AutoVerdictApp`에서 lazy). 아래 3.3절 수명 규칙. `suspend fun evaluate(rawJson): EngineResult` |
| `engine/EngineBridge.kt` | `@JavascriptInterface` onReady/onReport/onEngineError. 메인 스레드로 넘겨 처리 |
| `report/EvaluationReport.kt` | 4절 계약의 Kotlin 모델(`@Serializable`), `ReportParser` (알 수 없는 필드 무시, `schemaVersion` 확인) |
| `result/ResultRepository.kt` | 캐시 조회·기록, 저장 차 조회(`getByCarId`), 수집기 호출, 저장·삭제. 인터페이스로 두고 테스트에서 가짜로 교체 |
| `result/ResultController.kt` | 상태 머신(5.1절). 재시도, 저장 상태, 백그라운드 갱신, 공유 텍스트 |
| `result/ResultScreen.kt` | 상태별 화면 조립(기존 `ui/screen/ResultScreen.kt` 대체) |
| `result/sections/*.kt` | 섹션별 컴포저블(5.2절). 파일 하나에 섹션 하나 |
| `collector/CollectorWebView.kt` | 진행 단계 콜백(`onStage`) 추가, 오류 종류(`ErrorKind`) 추가. 그 외 동작 유지 |
| `ui/components/AvBottomCta.kt` | `windowInsets` 파라미터 추가(기본값은 지금과 같음). 결과 화면은 `WindowInsets(0)`을 넘긴다 |
| `ui/navigation/AppRoot.kt` | `ScreenHost.selectTab(MainTab)` 추가(기존 `NavEvent.SelectTab` 사용) |

- `webview/EvalWebView.kt`, `webview/NativeBridge.kt`와 테스트(`EvalWebViewTest`, `NativeBridgeTest`)는 제거하고 `RuleEngine`·`ResultRepository` 테스트로 대체한다.
- `proguard-rules.pro`: NativeBridge 규칙을 `EngineBridge`로 교체, kotlinx.serialization 규칙 추가.
- Gradle: `org.jetbrains.kotlin.plugin.serialization`(Kotlin 2.3.21과 같은 버전), `kotlinx-serialization-json` 추가. `buildWebView` 입력에 `engine.html` 추가(`index.html` 제거).

**상태 보관 범위.** 결과 상태는 androidx `ViewModel`이 아니라 `ResultController`(일반 클래스)에 둔다. `AppScreenContent`에서 `remember(route)`로 만들고, 코루틴은 `rememberCoroutineScope()`를 쓴다. 이유: Activity 범위 `ViewModelStore`에 두면 같은 차를 다시 열거나 화면을 닫아도 지워지지 않는다. 결과 화면은 항상 스택 맨 위에서만 보이므로(위에 다른 화면을 쌓지 않음) 컴포지션을 떠나면 상태를 버려도 된다. 회전·프로세스 종료 뒤에는 저장 차·캐시에서 즉시 다시 평가한다.

### 3.3 `RuleEngine` 수명 규칙

- **로드**: Vite가 엔진을 **IIFE 단일 스크립트**(`engine.js`)와 `engine.html`(일반 `<script src>`)로 만든다. `file:///android_asset/eval-ui/engine.html`을 로드한다. 모듈 스크립트가 아니므로 `allowFileAccess`·`allowFileAccessFromFileURLs`는 끈다(asset 경로는 이 설정과 무관하게 열림).
- **초기화 타임아웃 10초**: `onReady`가 오지 않으면 엔진을 버리고 대기 중인 요청을 모두 `EngineResult.Failed`로 끝낸다. 다음 요청 때 새로 만든다.
- **요청 타임아웃 10초**: 준비된 뒤 `avEvaluate`를 호출한 시점부터 잰다.
- **렌더러 종료**: `onRenderProcessGone`에서 `true`를 반환하고 WebView를 파기, 대기 요청은 실패 처리, 다음 요청 때 재생성.
- **취소**: 호출한 코루틴이 취소되면 대기 목록에서 지운다. 늦게 온 `onReport`(알 수 없는 requestId)는 버린다.
- **동시성**: 요청은 requestId로 구분해 여러 개가 동시에 대기할 수 있다. 모든 상태 변경은 메인 스레드에서만.

## 4. `EvaluationReport` 계약 (schemaVersion 1)

```json
{
  "schemaVersion": 1,
  "carId": "41878112",
  "url": "https://fem.encar.com/cars/detail/41878112",
  "car": {
    "title": "2015 BMW 5시리즈",
    "year": 2015, "mileageKm": 98000, "priceWon": 12900000,
    "fuel": "디젤", "summary": "2015년 · 9.8만km · 디젤 · 1,290만원"
  },
  "verdict": "CAUTION",
  "score": 72,
  "headline": "사기 전에 2가지는 꼭 확인하세요",
  "counts": { "danger": 0, "hidden": 0, "warning": 2, "pass": 8, "unknown": 0, "excluded": 1 },
  "dataGaps": [ { "source": "inspection", "reason": "failed" } ],
  "facts": [
    { "id": "accidents", "label": "사고", "value": "내차 2건", "tone": "neutral" },
    { "id": "owners", "label": "소유자 변경", "value": "5회", "tone": "warning" },
    { "id": "usage", "label": "용도", "value": "자가용", "tone": "neutral" },
    { "id": "floodLoss", "label": "침수·전손", "value": "없음", "tone": "neutral" }
  ],
  "repairs": {
    "panels": [ { "name": "트렁크 리드", "status": "교환", "rank": "1랭크", "tone": "warning" } ],
    "systems": {
      "flagged": [ { "group": "원동기", "name": "오일 누유", "status": "미세누유", "tone": "warning" } ],
      "okCount": 31
    }
  },
  "issues": [
    { "ruleId": "R07", "category": "이력", "title": "소유자 변경", "tone": "warning", "label": "주의",
      "message": "주인이 5번 바뀌었어요", "why": "…", "sellerQuestion": "직전 주인은 왜 팔았나요?" }
  ],
  "rules": [
    { "ruleId": "R04", "category": "차량 상태", "title": "프레임/외판", "tone": "success", "label": "통과",
      "message": "프레임 손상은 없어요", "why": null, "sellerQuestion": null }
  ],
  "excluded": [ { "ruleId": "R11", "title": "가격 적정성", "reason": "신차 가격 정보가 없어 평가하지 않았어요" } ],
  "timeline": [
    { "date": "2015-04", "label": "신차 등록", "detail": null, "tone": "neutral" },
    { "date": "2021-12", "label": "사고 3건 · 내차 피해", "detail": "수리비 합계 95만원", "tone": "warning" }
  ],
  "shareText": { "summary": "…", "sellerQuestions": "…" }
}
```

**규칙**
- `verdict`·`score`는 엔진 값 그대로(`NEVER|CAUTION|OK|UNKNOWN`, 0~100).
- **null 규칙**: 최상위 필드는 모두 항상 있다. `car`의 `year`·`mileageKm`·`priceWon`·`fuel`만 null 가능, `car.title`은 정보가 없으면 "차량 정보 없음". `repairs`는 성능점검을 못 읽었으면 null. `why`·`sellerQuestion`·`detail`은 null 가능. 배열은 비어 있어도 `[]`로 온다. `shareText.sellerQuestions`는 질문이 없으면 null.
- `car.title`은 기존 저장 형식과 같다: `"{연식 4자리} {제조사} {모델}"`(저장 목록·비교 화면이 이 값을 쓴다). `car.summary`는 헤더 한 줄용.
- `tone`: `danger|warning|success|neutral` (Kotlin `AvTone`으로 매핑).
- 엔진 severity → 표시: `killer`=**위험**(danger), `fail`=**비공개**(danger), `warn`=**주의**(warning), `pass`=**통과**(success), `unknown`=**미확인**(neutral). 판정 배지 "확인 필요"와 겹치지 않도록 룰 단위는 "미확인"으로 쓴다.
- `counts`: 화면용으로 `danger`(killer)와 `hidden`(fail)을 나눈다. 저장할 때는 기존과 같이 `dangerCount = danger + hidden`, `cautionCount = warning`, `passCount`, `unknownCount`. `excluded` = 엔진이 null을 반환한 룰 수.
- `headline` (결론 문장, 어댑터가 만든다. 룰 이름이 들어가 도메인 문구로 본다):

| 판정 | 조건 | 문장 |
|---|---|---|
| NEVER | 룰 번호가 가장 앞선 killer | R04는 근거 출처별: 성능점검 "성능점검에 사고 이력이 있어 사지 않는 게 좋아요", 엔카진단 "엔카진단에서 프레임 손상이 확인돼 사지 않는 게 좋아요", 출처 불명 "사고 이력이 있어 사지 않는 게 좋아요" / R05 "{렌트·택시·영업용} 이력이 있어 사지 않는 게 좋아요" / R06 "{전손·침수·도난} 이력이 있어 사지 않는 게 좋아요" (실제 해당 항목만 나열) |
| CAUTION | 주의 n개 | "사기 전에 {n}가지는 꼭 확인하세요" |
| UNKNOWN | 미확인 n개 | "확인하지 못한 항목이 {n}개 있어요" |
| OK | 주의 1개 | "큰 문제는 없지만 1가지는 확인해 보세요" |
| OK | 비공개 있음 | "큰 문제는 없지만 비공개 항목이 있어요" |
| OK | 그 외 | "큰 문제는 발견되지 않았어요" |

- `issues` = `rules` 중 tone이 danger·warning·neutral인 것. 정렬: danger(위험 → 비공개) → warning → neutral, 같은 묶음 안에서는 룰 번호 순. `rules`에도 그대로 남는다(전체 항목을 한곳에서 보기 위한 의도된 중복). `issues`의 `why`는 항상 채운다.
- `rules`는 카테고리 순서(차량 상태 → 이력 → 사고 → 가격 → 투명성), 카테고리 안에서 룰 번호 순. `why`·`sellerQuestion`은 issues에만 채우고 통과 항목은 null.
- **`repairs.panels`**: 성능점검 `outers` 중 상태가 있는 부위. `rank`는 원본 표기를 도메인 용어로: `RANK_ONE`→"1랭크", `RANK_TWO`→"2랭크"(둘 다 외판, tone warning), `RANK_A`·`RANK_B`·`RANK_C`→"A랭크"·"B랭크"·"C랭크"(주요 골격, tone danger), 그 외 → null(tone neutral). 정렬: 골격 → 2랭크 → 1랭크. 판정의 프레임 여부는 계속 R04(`master.accdient`)만 쓴다. 이 목록은 근거 표시용이다.
- **`repairs.systems`**: `inners` 트리 전체를 돌며 `statusType`이 있는 모든 노드의 상태 코드를 읽는다(core R12 순회와 같음). `group`은 바로 위 부모 이름. 코드 → tone: 1 양호·2 적정·3 없음 = success, 4 미세누수·6 미세누유·8 부족·9 과다 = warning, 5 누수·7 누유·10 불량 = danger, 11 있음·그 외 = neutral. `flagged`는 success가 아닌 항목(danger 먼저), `okCount`는 success 개수. R12(오일 누유) 판정과 표시가 같은 코드표(core `OIL_LEAK_CODES`)를 따르는지 테스트로 확인한다.
- **`timeline`**: 날짜 오름차순, `YYYY-MM`. 원본 `recordJson`을 직접 읽는다. 항목: 최초 등록(`firstDate`), 소유자 변경(`ownerChanges`, 각각), 사고(`accidents`, 같은 날짜·같은 종류는 하나로 묶고 "사고 n건"), 보험 미가입 기간(`notJoinDate1..5`, "보험 미가입 2023-11~2024-01"), 침수·전손·도난(`floodDate`·`totalLossDate`·`robberDate`), 번호 변경(`carInfoChanges`). 사고 `type` 1·2 = "내차 피해", 3 = "타차 가해"(두 테스트 매물의 건수와 `myAccidentCnt`·`otherAccidentCnt`가 일치함을 확인했다). `detail`은 묶인 사고의 수리비(`partCost`+`laborCost`+`paintingCost`) 합계를 만원 단위 반올림("수리비 합계 95만원"), 0이면 null. 보험금(`insuranceBenefit`)이 아니라 수리비 기준이라 종류별 합계가 R10 문구의 `myAccidentCost`·`otherAccidentCost`와 같다(보험이력이 있는 세 매물에서 테스트로 확인). tone: 침수·전손·도난 danger, 사고 warning, 보험 미가입 warning, 나머지 neutral.
- **`dataGaps`**: `source` ∈ `page|record|diagnosis|inspection`, `reason` ∈ `failed|loginRequired`. `httpStatus`가 `error`면 failed, `unauthorized`면 loginRequired. `not_found`(개인 매물의 진단 없음 등)·`skipped`·호출 없음은 **누락이 아니다**(원래 없는 정보).
- 모든 문구는 해요체, 이모지 없음, 개발자 용어 없음(0번 9절). 중고차 용어는 그대로 쓴다.
- 알 수 없는 필드는 Kotlin 파서가 무시한다. `schemaVersion`이 1이 아니면 엔진 오류로 처리한다.

## 5. 화면

### 5.1 상태 머신 (`ResultController`)

| 상태 | 화면 |
|---|---|
| `Loading(step)` | `AvLoadingSteps` 3단계(매물 페이지 열기 → 이력·진단 가져오기 → 12가지 기준으로 평가) + MREC 광고. 저장 차·캐시가 있으면 이 상태를 건너뛴다 |
| `Ready(report, saved, banner, pendingUpdate)` | 5.2절 구성 |
| `Error(kind)` | `AvEmptyState` + 버튼 |
| `Deleted` | 저장하지 않은 삭제 매물: "판매가 끝났거나 삭제된 매물이에요" + 돌아가기 |

**오류 종류**: 수집기는 `Result.Error(kind, message)`를 돌려준다. kind와 화면 문구:

| kind | 원인 | 문구 | 버튼 |
|---|---|---|---|
| `Network` | `onReceivedError` | "인터넷 연결을 확인해 주세요" | 다시 시도 |
| `Timeout` | 수집 45초 초과 | "엔카 응답이 늦어요" | 다시 시도 · 엔카에서 보기 |
| `PageUnreadable` | 페이지 데이터 추출·파싱 실패 | "엔카 페이지를 읽지 못했어요" | 다시 시도 · 엔카에서 보기 |
| `EngineFailed` | 엔진 초기화·평가 실패, 계약 오류 | "결과를 계산하지 못했어요" | 다시 시도 |
| `InvalidUrl` | carId 추출 실패 | "엔카 매물 주소가 아니에요" | 돌아가기 |

**저장 여부**는 라우트의 `fromSaved`가 아니라 DB(`savedCarDao.getByCarId`)로 정한다. 저장·삭제 후에는 상태를 직접 갱신한다.

**데이터 소스 순서**
1. **저장한 차**(DB에 있음): 저장된 `rawJson`을 바로 평가해 `Ready`로 보여주고, 백그라운드로 다시 수집한다.
   - 수집 실패: 조용히 저장 결과를 유지한다.
   - 삭제 매물: 저장 결과를 유지하고 "판매 끝난 매물" 배너.
   - 성공했지만 `dataGaps`가 저장 결과보다 많음(예: 로그인 풀림): 저장 결과를 유지한다.
   - 성공: 사용자가 아직 스크롤하지 않았으면 바로 바꾼다. 스크롤했으면 `pendingUpdate`에 두고 배너 "최신 정보가 있어요 · 보기"를 띄워 누르면 바꾸고 맨 위로 이동한다. 바꿀 때 저장 행을 갱신한다(`score`·`verdict`·집계·`rawJson`·`updatedAt`, `savedAt`은 유지).
2. **캐시**(24시간): 있으면 `rawInputJson`을 평가해 보여준다.
3. **수집**: 진행 단계를 `Loading(step)`으로 보여준다.

**다시 시도**: 캐시를 건너뛰고 실제로 다시 수집한다.
- 화면에 보고서가 있을 때(예: 일부 누락 배너의 [다시 시도])는 로딩 화면으로 바꾸지 않고 현재 결과를 `refreshing` 상태로 유지한다. 성공하면 바로 교체한다(누락이 늘면 교체하지 않음). 실패하면 기존 결과를 유지하고 토스트 "다시 가져오지 못했어요"를 띄운다. 삭제 매물이면 저장 차는 판매 종료 배너, 저장 안 한 차는 삭제 화면.
- 보고서가 없을 때(오류 화면)는 로딩 화면부터 시작한다.

**엔진 무응답**: 요청 타임아웃이 연속 2회 나면 엔진 WebView를 파기하고 다음 요청 때 다시 만든다(3.3절 보강).

**캐시 기록**: 수집 성공 + `dataGaps`가 비었을 때만 쓴다. `title`·`score`·`verdict`·`rawInputJson`을 채운다(`resultJson`은 빈 값 유지). 누락이 있는 결과는 다음에 다시 수집하도록 캐시하지 않는다.

**배너**(하나만, 우선순위 순): 판매 종료 > 최신 정보 있음 > 로그인 필요 > 일부 누락 > 없음.

### 5.2 Ready 구성 (위→아래, 모두 펼침)

1. `AvTopBar`: 뒤로, "분석 결과", (저장된 매물이면 부제 "저장된 매물"), "엔카에서 보기" 칩
2. 배너(있을 때):
   - 판매 종료 → "판매가 끝난 매물이에요 · 저장 당시 결과예요"
   - 최신 정보 → "최신 정보가 있어요" + [보기]
   - 로그인 필요 → "엔카에 로그인하면 더 많은 항목을 확인할 수 있어요"
   - 일부 누락 → "일부 정보를 가져오지 못했어요" + [다시 시도]
3. **판정 헤더**: `car.summary`(caption1) · 판정 배지(`verdictLabelRes`) + 점수(`title2`, 판정 톤 text 색, "72점") · 얇은 진행 바(판정 톤 fill) · **결론 문장** `headline`(`title1`)
4. 개수 칩: 위험 · 비공개 · 주의 · 통과 · 미확인 · 평가 제외 (0인 칩은 숨김)
5. 핵심 사실 4칸(`AvFactTile`)
6. **교환·수리 부위**(`repairs`가 있고 내용이 있을 때): 부위 행(이름 + 랭크 배지 + 상태 배지) → 주요 장치 중 이상 있는 항목 행 → "그 외 {okCount}개 항목 양호" 한 줄. 부위·이상이 모두 없으면 "교환·수리 부위가 없어요" 한 줄
7. **확인할 것 {issues 수}**: 카드마다 배지·제목·결과 문장·"왜 중요한가요" 문단·판매자 질문 한 줄(있을 때)
8. **이력**: 세로 타임라인, **최신순**으로 표시(보고서의 `timeline`은 오름차순이고 화면에서 뒤집는다). 7개 이상이면 최근 5개만 보여주고 아래에 "이전 이력 {n}개 더 보기" 버튼, 펼치면 "접기". 6개 이하면 전부 표시. 비었으면 섹션 숨김. (사용자 결정 2026-10-17: 이력이 길어 스크롤이 많아지는 문제. "모두 펼침" 원칙의 예외)
9. **전체 항목 {rules 수}**: 카테고리 소제목 + 행(상태 배지, 제목, 결과 문장)
10. **평가에서 빠진 항목**(있을 때): 제목 + 이유
11. **판매자에게 물어볼 것 {n}**(있을 때): 질문 목록 + [복사] [공유]
12. 하단 고정 `AvBottomCta(windowInsets = 0)`: [공유 아이콘 버튼(결과 요약)] [저장하기 / 저장됨] — 그 아래 320x50 배너. 바깥 `Column`이 내비게이션 바 인셋을 한 번만 적용한다.

- 0번 9절의 "룰 상세는 바텀시트" 규칙은 이 화면에서 폐기한다(모두 펼침).
- 섹션 제목은 모두 `semantics { heading() }`.

### 5.3 저장·캐시·공유

- **저장**: `ResultRepository.save(report, rawJson)`가 `SavedCarEntity`를 upsert한다. 필드 의미는 기존과 같다(`title`=`car.title`, `priceWon`, 집계는 4절 규칙). 저장 후 버튼은 "저장됨"(Secondary), 토스트 "저장했어요" + 액션 "보기"(`host.selectTab(MainTab.Saved)`). "저장됨"을 누르면 확인 다이얼로그 후 삭제하고 토스트 "저장을 취소했어요".
- **공유**: 결과 요약(`shareText.summary`)과 판매자 질문(`shareText.sellerQuestions`)을 `ACTION_SEND` 텍스트로 공유(chooser). 판매자 질문은 클립보드 복사도 제공(토스트 "복사했어요"). 공유 텍스트 끝에 매물 URL과 "AutoVerdict로 분석" 한 줄. 결과 요약 형식: 차 제목 / 판정·점수 / 결론 문장 / 확인할 것 최대 5줄 / URL.

### 5.4 광고
- 로딩 중 300x250 MREC(기존 유지), 로딩 후 하단 CTA 아래 320x50 배너(기존 위치 유지). 오류·삭제 화면은 배너만.

## 6. 문구 규칙 (어댑터)

- 결과 문장은 엔진의 `message` 문자열을 쓰지 않고 룰 결과와 수치로 새로 만든다(엔진 문구에는 합니다체·이모지·개발자 용어가 섞여 있음). 수치는 `evidence`·facts에서 읽는다.
- 미확인 사유 매핑: `login_required` → "엔카에 로그인하면 확인할 수 있어요", `api_fetch_error`·`not_fetched` → "엔카에서 정보를 받지 못했어요", 딜러 비공개 → "딜러가 공개하지 않았어요", `no_report_for_personal`(API 404) → 룰의 출처별 문구: 보험이력(R05~R10) "엔카에 이 차의 보험 이력이 없어요", 성능점검(R12) "엔카에 성능점검 기록이 없어요", 진단·성능점검(R04) "프레임 상태를 확인할 자료가 없어요". 그 외 사유 → "정보를 확인하지 못했어요".
- 평가 제외 이유(엔진이 null을 반환한 룰): R03 "엔카진단을 받지 않은 차예요(감점 없음)", R11 "신차 가격 정보가 없어 평가하지 않았어요", R12 "성능점검 기록이 없어 누유를 평가하지 않았어요".
- R04 killer 결과 문장은 근거 출처를 밝힌다. 출처는 어댑터(`report/frame-source.ts`)가 코어 `encarToFacts`의 R04 우선순위(엔카진단 CHECKER_COMMENT → 성능점검 `master.accdient` → 리본)를 코어 헬퍼(`getFrameIntact`·`getFrameFromInspection`)로 그대로 재현해 정한다. 성능점검 "성능점검에 사고 이력이 있어요"(외판 부위가 있으면 " · 외판 {부위들} 교환·수리"), 판매자 질문 "성능점검 사고 부위의 수리 내역과 사진을 볼 수 있을까요?"; 엔카진단 "엔카진단에서 프레임 손상이 확인됐어요", 질문 "프레임 수리 내역과 사진을 볼 수 있을까요?"; 출처 불명 "사고 이력(프레임 손상)이 있어요", 질문은 엔카진단과 같다. 진단이 프레임 정상이라 해도 판정이 모호하면(예: 39339524 "외부패널 단순교환") 성능점검이 근거가 되므로, 진단을 근거로 단정하는 문구를 쓰지 않는다.
- 가격 적정성 이유: "신차 가격 대비 지나치게 싸거나 비싸면 …"으로 고친다(실제 계산은 신차가 대비).
- 판매자 질문은 문제 항목에만 있고, 수치·기간을 넣는다(예: "2023년 11월~2024년 1월에 보험 없이 운행했나요?").
- 기존 `rule-meta.ts`의 `why` 문단은 합니다체를 해요체로 고쳐 옮긴다.

## 7. 범위

**포함**: 3~6절 전부. React 결과 UI 제거. 테스트·QA(8절).

**제외**: 판정·점수 로직 변경, vendored core 수정, DB 스키마 변경, 홈 "최근 조회" 표시 변경(2번), 저장 목록·비교 화면 재설계(3번).

## 8. 검증

| 대상 | 방법 |
|---|---|
| 어댑터 | vitest. 실제 raw JSON 4건을 **식별 정보 제거 후** 픽스처로 사용(`tests/fixtures/encar-*.json`): 39339524(NEVER·R04, 외판 교환), 41878112(CAUTION·소유자·보험 공백), 42264483(NEVER, 진단 404), 41623744(UNKNOWN, 세 API 모두 404). 제거 후에도 엔진 결과가 원본과 같음을 확인했다. facts·repairs·issues·rules·excluded·timeline·counts·shareText 스냅샷과 핵심 단언 |
| 계약 | vitest가 만든 보고서 JSON을 `app/src/test/resources/report/*.json`에 고정하고, Kotlin 파서 테스트가 같은 파일을 읽는다(양쪽 계약 일치) |
| 엔진 | Robolectric은 WebView JS를 못 돌리므로 `RuleEngine`의 WebView 부분을 인터페이스(`EngineHost`)로 빼고 가짜로 테스트: 초기화 타임아웃, 요청 타임아웃, 렌더러 종료 후 재생성, 취소, 늦은 응답 무시, 동시 요청. 실제 JS는 에뮬레이터 QA로 확인 |
| Controller | 가짜 Repository·Engine으로 상태 전이: 캐시 → Ready, 오류 종류별 Error, 다시 시도(캐시 건너뜀), 저장 차 즉시 표시 → 백그라운드 갱신(스크롤 전 교체 / 스크롤 후 배너 / 실패·누락 증가 시 유지), 삭제 매물 + 저장 → 배너, 누락 있는 결과는 캐시 안 함, 저장 여부는 DB 기준 |
| 화면 | Compose UI 테스트(계약 JSON 픽스처 사용): 섹션 노출 규칙(빈 섹션 숨김), 개수 칩, 배너 우선순위, 저장 버튼 상태, 오류 종류별 문구·버튼, 접근성(섹션 heading) |
| 기존 | Kotlin 테스트(현재 142), vitest 중 React UI 테스트는 제거·대체 |
| 수동 QA | 전용 AVD(`AV_API_36`, `-no-snapshot`)에서 두 테스트 URL, 오프라인 저장 차 재열기, 다시 시도, 공유·복사, 큰 글꼴 |
| 크기 | release AAB 측정(React 제거로 줄어드는지 기록) |

## 9. 리스크

| 리스크 | 대응 |
|---|---|
| 사고 `type` 의미 추정 오류 | 건수 합으로 확인함. QA 때 엔카 화면과 대조, 다르면 문서·코드 수정 |
| 성능점검 랭크 값 | 테스트 매물에는 `RANK_ONE`·`RANK_TWO`만 있다. 골격 랭크 표기는 추정이므로 모르는 값은 rank null·neutral로 안전하게 표시 |
| 저장 차 갱신 중 내용 바뀜 | 스크롤 전에만 바로 교체, 그 뒤엔 배너로 사용자가 고르게 함 |
| 성능점검·진단 응답 형태가 매물마다 다름 | 어댑터는 필드 누락에 관대하게(없으면 섹션 숨김), 픽스처 2종으로 검증 |
| 화면 없는 WebView의 타이머·수명 | 3.3절 규칙(초기화·요청 타임아웃, 렌더러 종료 처리, 재생성) |
| DB 파괴적 마이그레이션 | 스키마 변경 금지(리뷰 체크 항목) |
| 픽스처의 개인정보 | 식별 정보 제거 스크립트를 픽스처 생성 단계에 포함, 커밋 전 grep 확인 |
| 결과 화면이 매우 길어짐 | `LazyColumn` + 섹션 heading으로 TalkBack 탐색 지원 |
