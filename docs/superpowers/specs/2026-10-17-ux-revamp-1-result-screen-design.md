# UX/UI 개편 1번 — 결과 화면 설계

- 작성일: 2026-10-17
- 브랜치: `feat/ux-revamp` (worktree `auto-verdict-aos-ux`, `main` f996862 기준)
- 선행: 0번 디자인 언어 & 정보 구조 (`2026-10-17-ux-revamp-0-design-language-design.md`)
- 상태: 설계 확정, 구현 계획 작성 전

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
| 사용자 문구 위치 | 도메인 문구(룰 결과 문장, 이유, 판매자 질문, 타임라인 라벨, 공유 텍스트)는 **앱 전용 TS 어댑터**가 만든다. 화면 크롬 문구(섹션 제목, 버튼, 결론 문장 템플릿)는 `strings.xml` | 도메인 문구는 데이터와 함께 vitest로 검증 |
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
  → ResultViewModel (상태 머신) → Compose ResultScreen
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
- 빌드 산출물은 기존처럼 `app/src/main/assets/eval-ui/`로 복사된다. 엔트리 HTML은 스크립트만 로드한다.

### 3.2 Android 쪽

| 경로 | 책임 |
|---|---|
| `engine/RuleEngine.kt` | 앱 범위 단일 인스턴스(`AutoVerdictApp`에서 lazy). 메인 스레드에서 WebView 생성, engine 페이지 로드, `onReady` 핸드셰이크 후 요청 큐 처리. `suspend fun evaluate(rawJson): EngineResult` (requestId 매칭, **타임아웃 10초**) |
| `engine/EngineBridge.kt` | `@JavascriptInterface` onReady/onReport/onEngineError |
| `report/EvaluationReport.kt` | 4절 계약의 Kotlin 모델(`@Serializable`), `ReportParser` (알 수 없는 필드 무시, `schemaVersion` 확인) |
| `result/ResultRepository.kt` | 캐시 조회·기록(`title/score/verdict` 채움), 저장 차 조회, 수집기 호출, 저장·삭제 |
| `result/ResultViewModel.kt` | 상태 머신(5.1절). 재시도, 저장 상태, 공유 텍스트 제공 |
| `result/ResultScreen.kt` | 상태별 화면 조립(기존 파일 대체) |
| `result/sections/*.kt` | 섹션별 컴포저블(5.2절). 각 파일 하나의 섹션 |
| `collector/CollectorWebView.kt` | 진행 단계 콜백(`onStage`) 추가. 그 외 동작 유지 |

- `webview/EvalWebView.kt`, `webview/NativeBridge.kt`는 제거한다(저장·삭제는 `ResultRepository`로 이동).
- `proguard-rules.pro`: NativeBridge 규칙을 `EngineBridge`로 교체, kotlinx.serialization 규칙 추가.
- Gradle: `org.jetbrains.kotlin.plugin.serialization`(Kotlin 2.3.21과 같은 버전), `kotlinx-serialization-json` 추가.

## 4. `EvaluationReport` 계약 (schemaVersion 1)

```json
{
  "schemaVersion": 1,
  "carId": "41878112",
  "car": {
    "title": "2015 BMW 5시리즈 520d",
    "year": 2015, "mileageKm": 98000, "priceManwon": 1290,
    "fuel": "디젤", "isPersonal": false
  },
  "verdict": "CAUTION",
  "score": 72,
  "counts": { "danger": 0, "warning": 2, "pass": 8, "unknown": 0, "excluded": 1 },
  "dataGaps": [ { "source": "inspection", "reason": "failed" } ],
  "facts": [
    { "id": "accidents", "label": "사고", "value": "내차 2건", "tone": "neutral" },
    { "id": "owners", "label": "소유자", "value": "6명", "tone": "warning" },
    { "id": "usage", "label": "용도", "value": "자가용", "tone": "neutral" },
    { "id": "floodLoss", "label": "침수·전손", "value": "없음", "tone": "neutral" }
  ],
  "repairs": {
    "panels": [ { "name": "트렁크 리드", "status": "교환", "rank": "outer" } ],
    "systems": [ { "name": "원동기", "status": "양호", "tone": "success" } ]
  },
  "issues": [
    { "ruleId": "R07", "tone": "warning", "label": "주의", "title": "소유자 변경",
      "message": "주인이 5번 바뀌었어요", "why": "…", "sellerQuestion": "직전 주인은 왜 팔았나요?" }
  ],
  "rules": [
    { "ruleId": "R04", "category": "차량 상태", "title": "프레임/외판", "tone": "success",
      "label": "통과", "message": "프레임 손상 없음" }
  ],
  "excluded": [ { "ruleId": "R11", "title": "가격 적정성", "reason": "신차 가격 정보가 없어 평가하지 않았어요" } ],
  "timeline": [
    { "date": "2015-04", "label": "신차 등록", "detail": null, "tone": "neutral" },
    { "date": "2015-05", "label": "사고 · 내차 피해", "detail": "보험 처리 95만원", "tone": "warning" }
  ],
  "shareText": { "summary": "…", "sellerQuestions": "…" }
}
```

**규칙**
- `verdict`·`score`는 엔진 값 그대로(`NEVER|CAUTION|OK|UNKNOWN`, 0~100).
- `tone`: `danger|warning|success|neutral` (Kotlin `AvTone`으로 매핑). `label`: 위험·비공개·주의·통과·확인 필요.
- 엔진 severity → 표시: `killer`=위험(danger), `fail`=**비공개**(danger), `warn`=주의(warning), `pass`=통과(success), `unknown`=확인 필요(neutral).
- `counts`는 저장 데이터 호환을 위해 기존 집계와 같다: `danger = killer + fail`, `warning = warn`, `pass`, `unknown`. `excluded` = 엔진이 null을 반환한 룰 수.
- `issues` = `rules` 중 tone이 danger·warning·neutral인 것. 정렬: danger → warning → neutral, 같은 tone이면 룰 번호 순. 이 항목들은 `rules`에도 그대로 남는다(전체 12개를 한곳에서 보기 위한 의도된 중복).
- `rules`는 카테고리 순서(차량 상태 → 이력 → 사고 → 가격 → 투명성), 카테고리 안에서 룰 번호 순.
- `repairs`: 성능점검이 없으면 `null`. `panels`는 교환·판금·용접 등 정상이 아닌 부위만, `rank`는 `outer`(RANK_ONE)·`structural`(RANK_TWO 이상)·`unknown`. `systems`는 `inners` 최상위 그룹의 자식 중 상태가 있는 항목만, 불량이 먼저.
- `timeline`: 날짜 오름차순. 날짜는 `YYYY-MM`. 같은 날 사고는 하나로 묶고 건수를 라벨에 표시. 사고 `type`: 1·2 = 내차 피해, 3 = 타차 가해(두 테스트 매물의 건수 합으로 확인; 구현 시 엔카 화면과 대조해 확정하고 다르면 이 문서를 고친다). 금액은 만원 단위 반올림.
- 모든 문구는 해요체, 이모지 없음, 개발자 용어 없음(0번 9절). 중고차 용어는 그대로 쓴다.
- 알 수 없는 필드는 Kotlin 파서가 무시한다. `schemaVersion`이 1이 아니면 엔진 오류로 처리한다.

## 5. 화면

### 5.1 상태 머신 (`ResultViewModel`)

| 상태 | 화면 |
|---|---|
| `Loading(step)` | `AvLoadingSteps` 3단계(매물 페이지 열기 → 이력·진단 가져오기 → 12가지 기준으로 평가) + MREC 광고. 캐시나 저장 데이터가 있으면 거의 즉시 지나감 |
| `Ready(report, saved, banner)` | 5.2절 구성. `banner`: 없음 / 일부 데이터 누락(`dataGaps`) / 판매 끝난 매물(저장 당시 결과) |
| `Error(kind)` | `AvEmptyState` + 버튼. kind: `Timeout`·`Network`(다시 시도, 엔카에서 보기), `EngineFailed`(다시 시도), `InvalidUrl`(돌아가기) |
| `Deleted` | 저장하지 않은 삭제 매물: "판매가 끝났거나 삭제된 매물이에요" + 돌아가기 |

**데이터 소스 우선순위:** ① 저장한 차로 연 경우 저장된 `rawJson`을 즉시 평가해 보여주고, 동시에 백그라운드로 다시 수집한다. 수집이 성공하면 최신 결과로 교체하고 저장 요약을 갱신한다. 수집 결과가 삭제 매물이면 저장 당시 결과를 유지하고 "판매 끝난 매물" 배너를 띄운다. ② 그 외에는 24시간 캐시 → 없으면 수집.

**다시 시도:** 실제로 수집을 다시 시작한다(현재 React의 "다시 시도"는 닫기만 함).

### 5.2 Ready 구성 (위→아래, 모두 펼침)

1. `AvTopBar`: 뒤로, "분석 결과", (저장된 매물이면 부제 "저장된 매물"), "엔카에서 보기" 칩
2. 배너(있을 때): 일부 데이터 누락 → "일부 정보를 가져오지 못했어요" + 다시 시도 / 로그인 필요 → "엔카에 로그인하면 더 많은 항목을 확인할 수 있어요" / 판매 종료 → "판매가 끝난 매물이에요 · 저장 당시 결과예요"
3. **판정 헤더**: 차 정보 한 줄(caption1) · 판정 배지 + 점수(`title2`, 판정 톤 text 색) · 얇은 진행 바(판정 톤 fill) · **결론 문장**(`title1`)
4. 개수 칩: 위험 n · 주의 n · 통과 n · 확인 필요 n · 평가 제외 n (0인 칩은 숨김)
5. 핵심 사실 4칸(`AvFactTile`)
6. 교환·수리 부위(있을 때): 부위 목록(배지: 교환/판금·용접, structural이면 위험 톤) + 주요 장치 상태(불량만 강조, 양호는 한 줄 요약)
7. **확인할 것 n**: 항목 카드마다 배지·제목·결과 문장·"왜 중요한가요" 문단·판매자 질문 한 줄
8. **이력**: 세로 타임라인
9. **전체 항목 12**: 카테고리 소제목 + 행(상태 아이콘, 제목, 결과 문장). 이유 문단은 문제 항목에만(7번에 이미 펼쳐짐)
10. **평가에서 빠진 항목**: 제목 + 이유
11. **판매자에게 물어볼 것 n**: 질문 목록 + [복사] [공유]
12. 하단 고정 `AvBottomCta`: [공유 아이콘 버튼(결과 요약 공유)] [저장하기 / 저장됨] — 그 아래 320x50 배너 광고

**결론 문장 템플릿 (`strings.xml`)**

| 판정 | 조건 | 문장 |
|---|---|---|
| NEVER | 첫 위험 항목 제목 X | "{X} 이력이 있어 사지 않는 게 좋아요" |
| CAUTION | 주의 n개 | "사기 전에 {n}가지는 꼭 확인하세요" |
| UNKNOWN | 확인 필요 n개 | "확인하지 못한 항목이 {n}개 있어요" |
| OK | 비공개 항목 있음 | "큰 문제는 없지만 비공개 항목이 있어요" |
| OK | 그 외 | "큰 문제는 발견되지 않았어요" |

### 5.3 저장·캐시·공유

- **저장**: `ResultRepository.save(report, rawJson)`가 `SavedCarEntity`를 upsert한다. 필드는 기존과 같다(title, year, mileageKm, priceWon, fuelType, score, verdict, 4개 집계, rawJson, savedAt/updatedAt). 저장 후 버튼은 "저장됨"(Secondary)이 되고 토스트 "저장했어요 · 보기"(보기 → 저장한 차 탭). 이미 저장된 차에서 "저장됨"을 누르면 확인 다이얼로그 후 삭제한다.
- **캐시**: 결과를 캐시에 쓸 때 `title`, `score`, `verdict`를 채운다(지금은 빈 값). 홈 "최근 조회" 표시 변경은 2번에서 한다.
- **공유**: 결과 요약(`shareText.summary`)과 판매자 질문(`shareText.sellerQuestions`)을 `ACTION_SEND` 텍스트로 공유. 판매자 질문은 클립보드 복사도 제공. 공유 텍스트 끝에 매물 URL과 "AutoVerdict로 분석" 한 줄.

### 5.4 광고
- 로딩 중 300x250 MREC(기존 유지), 로딩 후 하단 CTA 아래 320x50 배너(기존 위치 유지).

## 6. 문구 규칙 (어댑터)

- 결과 문장은 엔진의 `message` 문자열을 쓰지 않고 룰 결과와 수치로 새로 만든다(엔진 문구에는 합니다체·이모지·개발자 용어가 섞여 있음).
- 미확인 사유 매핑 예: 로그인 필요 → "엔카에 로그인하면 확인할 수 있어요", 페이지 데이터 없음 → "엔카에서 정보를 받지 못했어요", 개인 매물 진단 → "개인 매물이라 엔카진단 대상이 아니에요".
- 가격 적정성 이유: "신차 가격 대비 지나치게 싸거나 비싸면 …"으로 고친다(실제 계산은 신차가 대비).
- 판매자 질문은 문제 항목에만 있고, 수치·기간을 넣는다(예: "2023년 11월~2024년 1월에 보험 없이 운행했나요?").

## 7. 범위

**포함**: 3~6절 전부. React 결과 UI 제거. 테스트·QA(8절).

**제외**: 판정·점수 로직 변경, vendored core 수정, DB 스키마 변경, 홈 "최근 조회" 표시 변경(2번), 저장 목록·비교 화면 재설계(3번).

## 8. 검증

| 대상 | 방법 |
|---|---|
| 어댑터 | vitest. 두 테스트 매물(39339524, 41878112)의 실제 raw JSON을 **식별 정보 제거 후** 픽스처로 사용(차량번호, VIN, recordNo, 딜러 연락처·주소, 이미지 URL 제거). facts·repairs·issues·rules·excluded·timeline·counts·shareText 스냅샷과 핵심 단언 |
| 계약 | vitest가 만든 보고서 JSON을 `app/src/test/resources/report/*.json`에 고정하고, Kotlin 파서 테스트가 같은 파일을 읽는다(양쪽 계약 일치) |
| 엔진 | Robolectric은 WebView JS를 못 돌리므로 `RuleEngine`은 브릿지 콜백·타임아웃·requestId 매칭을 가짜 WebView로 단위 테스트 |
| ViewModel | 상태 전이: 캐시 → Ready, 수집 실패 → Error, 다시 시도, 저장 차 즉시 표시 → 백그라운드 갱신, 삭제 매물 + 저장 → 배너 |
| 화면 | Compose UI 테스트: 섹션 노출 규칙(빈 섹션 숨김), 결론 문장 템플릿, 저장 버튼 상태, 접근성(섹션 heading) |
| 기존 | Kotlin 테스트(현재 142), vitest 중 React UI 테스트는 제거·대체 |
| 수동 QA | 전용 AVD(`AV_API_36`, `-no-snapshot`)에서 두 테스트 URL, 오프라인 저장 차 재열기, 다시 시도, 공유·복사, 큰 글꼴 |
| 크기 | release AAB 측정(React 제거로 줄어드는지 기록) |

## 9. 리스크

| 리스크 | 대응 |
|---|---|
| 사고 `type` 의미 추정 오류 | 구현 시 엔카 화면 대조, 다르면 문서·코드 수정 |
| 성능점검·진단 응답 형태가 매물마다 다름 | 어댑터는 필드 누락에 관대하게(없으면 섹션 숨김), 픽스처 2종으로 검증 |
| 화면 없는 WebView의 타이머·수명 | 앱 범위 단일 인스턴스, onReady 핸드셰이크, 요청 타임아웃 10초, 실패 시 재생성 |
| DB 파괴적 마이그레이션 | 스키마 변경 금지(리뷰 체크 항목) |
| 픽스처의 개인정보 | 식별 정보 제거 스크립트를 픽스처 생성 단계에 포함, 커밋 전 grep 확인 |
| 결과 화면이 매우 길어짐 | `LazyColumn` + 섹션 heading으로 TalkBack 탐색 지원 |
