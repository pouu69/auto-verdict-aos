# UX 개편 1번 (결과 화면) Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 결과 화면을 React WebView에서 Compose로 옮긴다. WebView는 화면 없는 규칙 엔진이 되어 `EvaluationReport` JSON을 돌려주고, 판정 근거·이력·교환 부위·판매자 질문을 한 화면에 모두 펼쳐 보여준다.

**Architecture:** TS 어댑터(`webview-bundle/src/report/*`)가 vendored core의 엔진 결과를 화면용 보고서로 바꾼다(문구 포함). Kotlin은 `RuleEngine`(앱 범위 headless WebView) → `ResultController`(상태 머신) → `result/ResultScreen`(Compose) 순서로 흐른다. 데이터 접근은 `ResultRepository` 인터페이스 뒤에 숨긴다.

**Tech Stack:** TypeScript 5.6 + Vite 5 + vitest 2, Kotlin 2.3.21 + kotlinx.serialization 1.9.0, Compose BOM 2024.12.01, Room 2.8.4, Robolectric 4.16.

**Spec:** `docs/superpowers/specs/2026-10-17-ux-revamp-1-result-screen-design.md` — 결정·문구·규칙은 모두 이 문서가 기준이다. 계획과 스펙이 다르면 스펙을 따르고 차이를 보고한다.

---

## 공통 사항

**작업 위치:** 조율자 worktree `~/study/auto-verdict-aos-ux`, 브랜치 `feat/ux-revamp`. 병렬 에이전트는 각자 별도 worktree(`feat/ux-revamp` 최신 커밋에서 분기)에서 일한다. 원본 저장소(`auto-verdict-aos`)로 `cd`하지 않는다.

**worktree 준비 (에이전트마다):**
```bash
cp ~/study/auto-verdict-aos-ux/local.properties .
ln -s ~/study/auto-verdict-aos-ux/webview-bundle/node_modules webview-bundle/node_modules
```

**명령:**
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:testDebugUnitTest                         # Kotlin 전체
./gradlew :app:testDebugUnitTest --tests "com.car.autoverdict.result.*"
./gradlew :app:assembleDebug
cd webview-bundle && npx vitest run                      # TS 전체
cd webview-bundle && npx tsc --noEmit
```

**절대 규칙 (리뷰 체크 항목):**
1. `webview-bundle/src/core/**` 수정 금지. 판정·점수 로직 변경 금지.
2. Room 엔티티·DAO 스키마 변경 금지(`fallbackToDestructiveMigration`이라 저장한 차가 지워진다). 새 쿼리 추가도 하지 않는다(기존 DAO로 충분).
3. 사용자 문구: 해요체, 이모지 없음, 개발자 용어 없음. 중고차 용어(교환·판금·1랭크·누유 등)는 그대로.
4. 에뮬레이터는 `AV_API_*` AVD만, `-no-snapshot`, `-port 5580`(Task 13에서만 사용). 사용자 AVD(Medium_Phone_API_36.0, Galaxy_S24_2, Pixel_8a)나 이미 떠 있는 에뮬레이터를 켜거나 끄지 않는다.
5. 커밋: Conventional Commits, 끝에 `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. 원격 push 금지.

**이미 끝난 것 (웨이브 0, 조율자):**
- `webview-bundle/tests/node-shim.d.ts`: 테스트용 최소 Node 타입(아래 Chunk 1 참고).
- `153a5f1` 식별 정보 제거 픽스처 4건 `webview-bundle/tests/fixtures/encar-{39339524,41878112,42264483,41623744}.json` + `webview-bundle/scripts/scrub-fixture.mjs`. 엔진 결과는 원본과 같다:
  - 39339524 NEVER 85 — R04 killer, R10 warn, R11 제외(신차가 없음). 외판: 트렁크 리드 교환(RANK_ONE), 쿼터 패널(우) 판금/용접+용접,절단(RANK_TWO). 사고 9건(2021-12-01에 3건 등).
  - 41878112 CAUTION 89 — R07 warn(소유자 5회), R08 warn(미가입 202311~202401, 202604~202607). 외판 없음.
  - 42264483 NEVER 72 — R03 제외(진단 404), R04 killer, R08·R10·R12 warn.
  - 41623744 UNKNOWN 61 — 세 API 모두 404(`not_found`), R04~R10 unknown, R03·R11·R12 제외.
- `3fff9f2` 계약: `webview-bundle/src/report/types.ts`, `app/.../report/EvaluationReport.kt`(+`ReportParser`, `Tone.toAvTone()`), `app/.../engine/ReportEvaluator.kt`(`EngineResult`, `ReportEvaluator`), `app/.../result/ResultContracts.kt`(`ResultRepository`, `CollectOutcome`, `CollectStage`, `CollectErrorKind`, `ResultUiState`, `LoadingStage`, `ResultErrorKind`, `ResultBanner`, `ResultEvent`), 손으로 쓴 샘플 `app/src/test/resources/report/sample-{caution,minimal}.json`, `ReportParserTest`. **계약 파일은 웨이브 1에서 고치지 않는다.** 꼭 바꿔야 하면 조율자에게 보고한다.

### 병렬 실행 계획 (웨이브)

| 웨이브 | 에이전트 | Task | 선행 |
|---|---|---|---|
| 1 | A · C · D · E | A: 1~4 (TS 어댑터) / C: 5 (RuleEngine) / D: 6~7 (수집기·Repository·Controller) / E: 8~9 (화면) | 웨이브 0 |
| 2 | F · G | F: 10 (엔진 엔트리·React 제거) / G: 11 (연결·구 코드 제거) | 웨이브 1 병합 |
| 3 | 조율자 | 12 (전체 검증·크기), 13 (에뮬레이터 QA), 14 (문서) | 웨이브 2 병합 |

웨이브가 끝나면 조율자가 각 브랜치를 리뷰(스펙 준수 → 코드 품질)한 뒤 `feat/ux-revamp`에 병합하고 `./gradlew :app:testDebugUnitTest :app:assembleDebug` + `npx vitest run`을 확인한다. `strings.xml` 충돌은 양쪽 줄을 모두 남긴다.

### 파일 구조

| 경로 | 책임 | Task |
|---|---|---|
| `webview-bundle/src/report/rule-meta.ts` | 룰별 카테고리·짧은 제목·`why`(해요체)·출처 | 1 |
| `webview-bundle/src/report/rule-copy.ts` | 룰 결과 → 결과 문장·판매자 질문·미확인 사유·평가 제외 이유 | 1 |
| `webview-bundle/src/report/facts.ts` | 핵심 사실 4칸 | 2 |
| `webview-bundle/src/report/repairs.ts` | 외판(`outers`)·장치(`inners`) | 2 |
| `webview-bundle/src/report/timeline.ts` | 이력 타임라인 | 2 |
| `webview-bundle/src/report/data-gaps.ts` | 데이터 누락 | 2 |
| `webview-bundle/src/report/headline.ts` | 결론 문장 | 3 |
| `webview-bundle/src/report/share-text.ts` | 공유 텍스트 | 3 |
| `webview-bundle/src/report/car.ts` | `ReportCar` | 3 |
| `webview-bundle/src/report/build-report.ts` | 조립 + 정렬 + 집계 | 3 |
| `webview-bundle/scripts/gen-contract.ts` (vitest로 실행) | 픽스처 → `app/src/test/resources/report/encar-*.json` | 4 |
| `webview-bundle/src/engine.ts`, `engine-bridge.ts`, `engine.html` | 엔진 엔트리 | 10 |
| `app/.../engine/RuleEngine.kt`, `EngineHost.kt`, `EngineBridge.kt` | headless 엔진 | 5 |
| `app/.../collector/CollectorWebView.kt` | `ErrorKind`, `onStage` | 6 |
| `app/.../result/DefaultResultRepository.kt` | Repository 구현 | 6 |
| `app/.../result/ResultController.kt` | 상태 머신 | 7 |
| `app/.../result/ResultScreen.kt`, `result/sections/*.kt` | 화면 | 8~9 |
| `app/.../result/ResultRoute.kt` | Controller 생성·공유·클립보드·토스트 연결 | 11 |
| `app/.../ui/components/AvBottomCta.kt` | `windowInsets` 파라미터 | 8 |
| `app/.../ui/navigation/AppRoot.kt`, `AppScreenContent.kt` | `selectTab`, 새 결과 화면 연결 | 8, 11 |
| `app/.../webview/*`, `ui/screen/ResultScreen.kt`, React 소스 | 제거 | 10, 11 |

---

## Chunk 1: TS 어댑터 (에이전트 A, 웨이브 1)

에이전트 A는 `webview-bundle/` 안에서만 일한다. 기존 React 코드(`App.tsx`, `overlay/*`, `rule-meta.ts`, `verdict-meta.ts`)는 **지우지 않는다**(Task 10에서 제거). 새 파일은 모두 `src/report/` 아래. 테스트는 `tests/report/*.test.ts`. 테스트에서 픽스처는 JSON import로 읽고 `orchestrateMobile(fx as MobileOrchestratorInput)`의 결과를 어댑터에 넘긴다.

**Node 타입:** `@types/node`가 설치돼 있지 않고 `tsconfig`가 `tests/`를 포함하므로, 테스트에서 픽스처는 **JSON import**로 읽는다(`import fx from '../fixtures/encar-41878112.json'`, `resolveJsonModule`이 켜져 있다). 파일 쓰기가 필요한 Task 4만 웨이브 0에서 추가한 `tests/node-shim.d.ts`(`node:fs`의 `readFileSync`/`writeFileSync`/`existsSync`, `process.env` 최소 선언)를 쓴다. 새 npm 패키지를 설치하지 않는다.

어댑터의 공통 입력:
```ts
import type { MobileOrchestratorInput, MobileOrchestratorResult } from '../orchestrate-mobile';
// raw: 수집기 payload (MobileOrchestratorInput), out: orchestrateMobile(raw)
```
`out.report.results`는 엔진이 null을 반환한 룰이 빠진 목록이다. 전체 룰 ID는 `R01`~`R12`.

### Task 1: 룰 메타와 룰 문구

**Files:**
- Create: `webview-bundle/src/report/rule-meta.ts`, `webview-bundle/src/report/rule-copy.ts`
- Test: `webview-bundle/tests/report/rule-copy.test.ts`

`rule-meta.ts`: 기존 `src/rule-meta.ts`의 카테고리·짧은 제목·`why`를 옮기되 `why`는 **해요체**로 고친다(예: "…자료예요. 정상 매물은 공개가 기본이라, 딜러가 굳이 가렸다면 숨기고 싶은 사고가 있을 수 있어요."). 아이콘 필드는 버린다. R11 `why`는 "신차 가격 대비 지나치게 싸거나 비싸면 …"으로 고친다. 각 룰에 `source: 'page' | 'record' | 'inspection' | 'diagnosisOrInspection'`을 붙인다(R01·R02·R03·R11 page, R05~R10 record, R12 inspection, R04 diagnosisOrInspection).

```ts
export interface RuleMeta { category: Category; title: string; why: string; source: RuleSource }
export const RULE_META: Record<string, RuleMeta>;
export const RULE_IDS: readonly string[]; // ['R01', …, 'R12']
export const CATEGORY_ORDER: readonly Category[]; // ['차량 상태','이력','사고','가격','투명성']
```

`rule-copy.ts`:
```ts
export interface RuleCopy { message: string; sellerQuestion: string | null }
/** 엔진 message는 쓰지 않는다. severity·evidence·facts로 해요체 문장을 새로 만든다. */
export function ruleCopy(result: RuleResult, facts: ChecklistFacts): RuleCopy;
export function unknownMessage(result: RuleResult, out: MobileOrchestratorResult): string; // 스펙 6절 사유 매핑
export function excludedReason(ruleId: string): string;                          // 스펙 6절
export function labelFor(severity: Severity): RuleLabel;   // killer 위험, fail 비공개, warn 주의, pass 통과, unknown 미확인
export function toneFor(severity: Severity): Tone;         // killer·fail danger, warn warning, pass success, unknown neutral
```
- 미확인 사유를 읽는 위치(중요): API가 404·실패면 `encarToFacts`는 R04~R10 facts를 `failed('not_derived')`로 두고, 실제 사유(`no_report_for_personal`, `login_required`, `api_fetch_error`, `not_fetched`)는 **`out.parsed.raw.{recordApi, diagnosisApi, inspectionApi}`**(FieldStatus)에만 남는다(41623744로 확인됨). 그러므로 룰 `source`에 맞는 API 상태를 먼저 본다: record → `recordApi`, inspection → `inspectionApi`, diagnosisOrInspection → `diagnosisApi`와 `inspectionApi` 중 실패한 쪽. API 상태가 정상(value)인데 룰이 unknown이면 그때 facts 필드(`kind`, `reason`)를 본다. R01·R02는 facts(`insuranceHistoryDisclosed`, `inspectionReportDisclosed`; `hidden_by_dealer`·`detail_flags_unavailable` 등), R03은 `hasEncarDiagnosis`, R11은 `priceVsMarket`. facts 필드 이름: R04 `frameDamage`, R05 `usageHistory`, R06 `totalLossHistory`, R07 `ownerChangeCount`, R08 `insuranceGap`, R09 `unconfirmedAccident`, R10 `minorAccidents`, R12 `oilLeak`. 모르는 사유 → "정보를 확인하지 못했어요".
- 결과 문장 예(수치는 evidence에서): R07 warn "주인이 {n}번 바뀌었어요", pass "소유자 변경 {n}회로 정상 범위예요"(0이면 "소유자 변경이 없어요"); R08 warn "자차보험 없이 운행한 기간이 {기간} 있어요"(+ 구간이 2개 이상이면 " ({k}개 구간)"); R10 warn "보험 처리 금액이 커요 · 내차 {x}만원 · 타차 {y}만원", pass "경미한 수준이에요 · 내차 …"; R04 killer "프레임까지 손상된 사고 이력이 있어요", pass "프레임 손상은 없어요"(외판 부위가 있으면 " · 외판 {부위들} 교환·수리"); R05 killer "{렌트·영업용} 이력이 있어요", warn "관용 이력이 있어요"; R06 killer "{전손 1회, 침수전손 1회 …} 이력이 있어요"; R01/R02 fail "딜러가 보험이력을 공개하지 않았어요"/"딜러가 성능점검 기록을 공개하지 않았어요"; R11 warn "신차 가격의 {p}%로 지나치게 싸요"/"신차 가격의 {p}%로 비싼 편이에요"; R12 warn "{부위}({상태})가 있어요"; R09 warn "수리비가 확정되지 않은 사고가 있어요".
- 판매자 질문은 danger·warning 항목만(그 외 null). 예: R07 "주인이 {n}번 바뀐 이유를 알 수 있을까요?", R08 "{YYYY년 M월~YYYY년 M월}{, …}에 보험 없이 운행했나요?"(facts `insuranceGap.periods`에서), R10 "보험 처리한 사고는 어느 부위를 어떻게 수리했나요?", R04 "프레임 수리 내역과 사진을 볼 수 있을까요?", R01 "보험이력을 공개해 주실 수 있나요?", R02 "성능점검 기록을 보여주실 수 있나요?", R12 "누유 부위는 수리했나요?", R11 "가격이 시세와 다른 이유가 있나요?", R05 "렌트·영업용으로 얼마나 쓰였나요?", R06 "{침수·전손·도난} 처리 후 어디서 수리했나요?", R09 "미확정 수리비 건은 어떻게 처리됐나요?".
- 이모지·`⚠`·`🚨`·"합니다" 금지.

- [ ] **Step 1: 실패하는 테스트 작성** — 픽스처 4건으로: 41878112의 R07 → message "주인이 5번 바뀌었어요", sellerQuestion에 "5번"; R08 sellerQuestion에 "2023년 11월~2024년 1월"과 "2026년 4월~2026년 7월"(또는 스펙과 같은 표기); 41623744의 R07 unknown → "엔카에 이 차의 보험 이력이 없어요"; `excludedReason('R11')` → "신차 가격 정보가 없어 평가하지 않았어요"; 모든 픽스처의 모든 결과에 대해 message·sellerQuestion·why가 `/[⚠🚨🔒⏱]|합니다|습니다/`와 맞지 않음; `labelFor`/`toneFor` 표.
- [ ] **Step 2: `npx vitest run tests/report/rule-copy.test.ts` → FAIL 확인**
- [ ] **Step 3: 구현**
- [ ] **Step 4: 테스트 통과 + `npx tsc --noEmit`**
- [ ] **Step 5: 커밋** `feat(report): add rule meta and Korean rule copy`

### Task 2: 사실·교환 부위·타임라인·데이터 누락

**Files:**
- Create: `webview-bundle/src/report/{facts,repairs,timeline,data-gaps}.ts`
- Test: `webview-bundle/tests/report/{facts,repairs,timeline,data-gaps}.test.ts`

```ts
export function buildFacts(raw: MobileOrchestratorInput, out: MobileOrchestratorResult): Fact[];
export function buildRepairs(raw: MobileOrchestratorInput): Repairs | null;
export function buildTimeline(raw: MobileOrchestratorInput): TimelineEntry[];
export function buildDataGaps(raw: MobileOrchestratorInput, out: MobileOrchestratorResult): DataGap[];
```

**facts** (항상 4칸, 순서 고정. 정보가 없으면 value "확인 불가", tone neutral. 보험이력(`recordJson`)이 없으면 빈 배열 `[]`):
- `accidents` 라벨 "사고": `myAccidentCnt`·`otherAccidentCnt` → "내차 {a}건 · 타차 {b}건"(0인 쪽 생략, 둘 다 0이면 "없음"). tone: 내차 사고가 있으면 warning, 아니면 neutral.
- `owners` 라벨 "소유자 변경": `ownerChangeCnt` → "{n}회"(0이면 "없음"). tone: 3회 이상 warning(R07 기준 2회 초과와 같게).
- `usage` 라벨 "용도": R05 facts(`usageHistory`)에서 렌트/영업용/관용이면 그 단어(여러 개면 "·"), 아니면 "자가용". 렌트·영업용 danger, 관용 warning, 자가용 neutral.
- `floodLoss` 라벨 "침수·전손": `totalLossCnt`·`floodTotalLossCnt`·`floodPartLossCnt`·`robberCnt` 중 0 초과 → "전손 1회" 등, 없으면 "없음". 있으면 danger.

**repairs** (`inspectionJson`이 없거나 객체가 아니면 null):
- panels: `outers[]` 각 항목 → `name = type.title`, `status = statusTypes.map(title)`을 정리(괄호 이하 제거, "판금/용접"→"판금·용접", 중복 제거, " · "로 연결), `rank`: attributes에서 `RANK_ONE`→"1랭크", `RANK_TWO`→"2랭크", `RANK_A`/`RANK_B`/`RANK_C`→"A랭크"/"B랭크"/"C랭크", 없거나 모르는 값 → null. tone: A/B/C → danger, 1·2랭크 → warning, null → neutral. 정렬: 골격(A·B·C) → 2랭크 → 1랭크 → null, 같은 그룹은 원래 순서.
- systems: `inners[]` 트리를 재귀로 돌며 `statusType`(`{code,title}`)이 있는 **모든 노드**를 본다(core `api-inspection.ts`의 R12 누유 순회와 같은 방식. 잎만 보면 R12와 어긋날 수 있다). `statusType`이 null인 노드는 건너뛰고 자식으로 내려간다. 코드 → tone: `1`,`2`,`3` success / `4`,`6`,`8`,`9` warning / `5`,`7`,`10` danger / 그 외 neutral. `flagged`: success가 아닌 노드(danger 먼저, 그다음 warning, neutral, 같은 tone은 순회 순서). `group`은 **바로 위 부모** 노드의 `type.title`(예: "오일누유"), `name`은 노드 `type.title`(예: "실린더 블록 / 오일팬"), `status`는 `statusType.title`. `okCount`는 success 노드 수. 실제 구조: 최상위(자기진단·원동기·변속기·동력전달·조향·제동·전기·연료 등, `statusType` 없음) → 항목 → 세부 항목(깊이 최대 2).
- 테스트: 39339524 → panels 2개, 첫 번째가 "쿼터 패널(우)"(2랭크, status "판금·용접 · 용접·절단" 또는 정리 규칙대로), 두 번째 "트렁크 리드"(1랭크, "교환"); 41878112 → panels `[]`; 41623744 → null; 42264483 → R12 warn이므로 flagged에 누유 항목이 1개 이상, tone warning 또는 danger. **R12 일치 테스트**: `repairs.ts`는 코드까지 돌려주는 내부 순회 함수 `walkSystems(inners): Array<SystemItem & { code: string }>`를 export하고 `buildRepairs`는 이를 쓴다. 각 픽스처에서 `walkSystems` 결과 중 코드가 6·7인 **노드**가 있으면 R12가 warn이어야 하고, 반대도 성립.

**timeline** (`recordJson`이 없으면 `[]`): 스펙 4절 timeline 규칙 그대로. 날짜 문자열은 `YYYY-MM-DD` 또는 `YYYYMM` → `YYYY-MM`. 보험 미가입 `notJoinDateN` "202311~202401" → `{date:"2023-11", label:"보험 미가입", detail:"2023-11~2024-01", tone:"warning"}`. 사고는 `(date, 내차/타차)`로 묶는다: 라벨 "사고 · 내차 피해"(1건) / "사고 {n}건 · 내차 피해"(여러 건), detail "보험 처리 합계 {합계}만원"(합계 0이면 null). 같은 날 내차·타차가 모두 있으면 두 항목. `carInfoChanges`는 첫 항목(최초 등록과 같은 날짜)을 제외하고 "번호 변경"(carNo는 절대 출력하지 않는다). 정렬: 날짜 오름차순, 같은 달은 신차 등록 → 사고 → 소유자 변경 → 보험 미가입 → 번호 변경 → 침수·전손·도난 순(안정 정렬).
- 테스트: 41878112 → 첫 항목 `{date:"2015-04", label:"신차 등록"}`, "소유자 변경" 5개, "보험 미가입" 2개; 39339524 → 2021-12에 내차 피해 묶음 라벨에 "건" 포함, 사고 항목의 detail이 "만원"으로 끝남; 41623744 → `[]`; 어떤 항목에도 차량번호 패턴(`/\d{2,3}[가-힣]\s?\d{4}|XXXX/`) 없음.

**dataGaps**: 스펙 4절. `raw.httpStatus`의 `recordJson`→record, `diagnosisJson`→diagnosis, `inspectionJson`→inspection. `error`→failed, `unauthorized`→loginRequired. `not_found`·`skipped`·키 없음 → 없음. `out.parsed`에서 페이지 base 추출이 실패했으면(`raw.preloadedState`에서 base를 못 찾음) `{source:'page', reason:'failed'}`. 순서: page, record, diagnosis, inspection.
- 테스트: 픽스처 4건 모두 `[]`(41623744의 404도 누락 아님); 픽스처를 복제해 `httpStatus.recordJson='unauthorized'`·`inspectionJson='error'`로 바꾸면 `[{record,loginRequired},{inspection,failed}]`.

- [ ] **Step 1: 4개 테스트 파일 작성 → FAIL 확인**
- [ ] **Step 2: 구현 → PASS + tsc**
- [ ] **Step 3: 커밋** `feat(report): add facts, repairs, timeline and data gaps`

### Task 3: 결론 문장·차 정보·공유 텍스트·조립

**Files:**
- Create: `webview-bundle/src/report/{headline,car,share-text,build-report}.ts`
- Test: `webview-bundle/tests/report/{headline,build-report}.test.ts`

```ts
export function buildHeadline(report: RuleReport, facts: ChecklistFacts): string; // 스펙 4절 headline 표
export function buildCar(raw: MobileOrchestratorInput): ReportCar;
export function buildShareText(r: Omit<EvaluationReport, 'shareText'>): ShareText;
export function buildReport(raw: MobileOrchestratorInput, out: MobileOrchestratorResult): EvaluationReport;
```
- **headline**: NEVER는 룰 번호가 가장 앞선 killer로 고른다. R05는 실제 해당 태그만(렌트/영업용 → "렌트·영업용 이력이 있어…"), R06도 실제 해당 항목만("침수 이력이 있어…"). CAUTION의 n = warn 수, UNKNOWN의 n = unknown 수. OK: warn 1개 → "큰 문제는 없지만 1가지는 확인해 보세요", fail 있음 → "큰 문제는 없지만 비공개 항목이 있어요", 그 외 "큰 문제는 발견되지 않았어요". 테스트는 6가지 분기를 모두 다룬다(엔진 `RuleReport`를 직접 만들어 넣는다).
- **car**: `title`은 기존 저장 형식과 정확히 같게 `${yearMonth.slice(0,4)} ${manufacturerName} ${modelName}`(base 없으면 "차량 정보 없음"). `year`=yearMonth 앞 4자리 정수, `mileageKm`=`spec.mileage`, `priceWon`=`advertisement.price * 10000`, `fuel`=`spec.fuelName`. `summary`: "{year}년 · {주행} · {fuel} · {가격}"에서 없는 부분 생략. 주행: 1만km 미만 "{n,toLocaleString}km", 이상 "{소수 1자리 반올림, .0 생략}만km". 가격: "{n,toLocaleString}만원".
- **buildReport**:
  - `rules`: `out.report.results`를 `RULE_META` 카테고리 순서 → 룰 번호 순으로 정렬. 각 항목 `title`=`RULE_META.title`, `category`, `tone`/`label`, `message`(unknown이면 `unknownMessage`, 아니면 `ruleCopy().message`), `why`·`sellerQuestion`은 tone이 success가 아니면 채우고(why는 항상, 질문은 danger·warning만) success면 null.
  - `issues`: `rules` 중 success가 아닌 것. 정렬: killer → fail → warn → unknown, 같은 묶음은 룰 번호 순.
  - `excluded`: `RULE_IDS` 중 results에 없는 룰 → `{ruleId, title, reason: excludedReason(id)}`(룰 번호 순).
  - `counts`: danger=killer 수, hidden=fail 수, warning, pass, unknown, excluded=`excluded.length`.
  - `verdict`·`score`는 `out.report` 그대로.
  - `schemaVersion: SCHEMA_VERSION`, `carId`·`url`은 raw에서.
- **share-text**: `summary` = 줄바꿈으로 연결: `car.title` / "판정: {판정 라벨} · {score}점" / `headline` / issues 중 처음 5개의 "- {message}" / `url` / "AutoVerdict로 분석". 판정 라벨은 앱과 같게 NEVER "피하세요", CAUTION "주의", OK "괜찮아요", UNKNOWN "확인 필요". `sellerQuestions` = 질문이 있는 issues의 "{i}. {질문}" + `url` + "AutoVerdict로 분석", 질문이 없으면 null.
- 테스트(`build-report.test.ts`): 픽스처 4건 각각 — `verdict`/`score`가 `out.report`와 같음; `counts`의 합(danger+hidden+warning+pass+unknown)이 `out.report.results.length`와 같음; `counts.excluded + results.length === 12`; `issues`가 `rules`의 부분집합이고 정렬 규칙을 지킴; 41878112의 headline "사기 전에 2가지는 꼭 확인하세요", 39339524의 headline이 "사고(프레임 손상) 이력이 있어"로 시작; 41623744 → `repairs` null, `timeline` `[]`, `dataGaps` `[]`, `facts` `[]`; 모든 문자열 필드에 금지 패턴 없음; `JSON.parse(JSON.stringify(report))`가 같은 값(직렬화 가능).

- [ ] **Step 1: 테스트 작성 → FAIL**
- [ ] **Step 2: 구현 → PASS + tsc**
- [ ] **Step 3: 커밋** `feat(report): assemble EvaluationReport with headline and share text`

### Task 4: 계약 JSON 생성

**Files:**
- Create: `webview-bundle/tests/contract/gen-contract.test.ts`
- Modify: `webview-bundle/package.json` (script `"gen:contract": "GEN_CONTRACT=1 vitest run tests/contract"`)
- Create (생성물): `app/src/test/resources/report/encar-{39339524,41878112,42264483,41623744}.json`

- `gen-contract.test.ts`: 픽스처 4건으로 `buildReport`를 만들고, `process.env.GEN_CONTRACT === '1'`이면 `../app/src/test/resources/report/encar-<id>.json`에 `JSON.stringify(report, null, 2) + '\n'`로 쓴다. 환경 변수가 없으면 **파일과 비교**만 하고 다르면 실패한다("npm run gen:contract로 다시 생성하세요"). 이렇게 하면 어댑터를 고치고 계약 파일을 갱신하지 않은 경우 vitest가 잡는다.
- [ ] **Step 1: 작성 후 `npm run gen:contract`로 생성**
- [ ] **Step 2: `npx vitest run` 전체 통과**
- [ ] **Step 3: Kotlin 쪽 확인** — `./gradlew :app:testDebugUnitTest --tests "com.car.autoverdict.report.ReportParserTest"`(생성 파일을 모두 파싱하는 테스트가 이미 있다) 통과
- [ ] **Step 4: 커밋** `test(report): generate contract JSON for the Kotlin parser`

---

## Chunk 2: Kotlin 엔진·데이터 (에이전트 C, D, 웨이브 1)

### Task 5: `RuleEngine` (에이전트 C)

**Files:**
- Create: `app/src/main/kotlin/com/car/autoverdict/engine/EngineHost.kt`, `RuleEngine.kt`, `EngineBridge.kt`, `WebViewEngineHost.kt`
- Test: `app/src/test/kotlin/com/car/autoverdict/engine/RuleEngineTest.kt`

설계 — WebView 의존 부분을 `EngineHost`로 분리해 `RuleEngine`의 규칙(스펙 3.3)을 가짜로 시험한다:
```kotlin
/** One engine page instance. All calls on the main thread. */
interface EngineHost {
    /** Load the engine page. [callbacks] may be invoked any time after (from the main thread). */
    fun start(callbacks: EngineCallbacks)
    /** Calls window.avEvaluate(requestId, rawJson). */
    fun evaluate(requestId: String, rawJson: String)
    fun destroy()
}
interface EngineCallbacks {
    fun onReady()
    fun onReport(requestId: String, reportJson: String)
    fun onEngineError(requestId: String, message: String)
    fun onRenderProcessGone()
}

class RuleEngine(
    private val hostFactory: () -> EngineHost,
    private val initTimeoutMs: Long = 10_000,
    private val requestTimeoutMs: Long = 10_000,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ReportEvaluator {
    override suspend fun evaluate(rawJson: String): EngineResult
}
```
- 엔진 내부에 `private val engineScope = CoroutineScope(SupervisorJob() + mainDispatcher)`를 두고 초기화 타이머는 여기서 돌린다(특정 호출 코루틴에 묶이지 않게). 요청 타이머는 호출 코루틴의 `withTimeoutOrNull` 또는 engineScope 중 하나로 일관되게.
- `evaluate`는 `withContext(mainDispatcher)` 안에서 동작. 호스트가 없으면 `hostFactory()`로 만들고 `start`. 준비 전 요청은 큐에 쌓고 `onReady`에 보낸다. 요청마다 `requestId`(UUID 또는 증가 숫자 문자열) + `CompletableDeferred`.
- 초기화 타임아웃(호스트 생성 시점부터) → 호스트 `destroy`, 대기 요청 전부 `Failed("init timeout")`, 호스트 참조를 null로(다음 요청 때 재생성).
- 요청 타임아웃(호스트에 `evaluate`를 보낸 시점부터) → 그 요청만 `Failed("request timeout")`.
- `onReport` → `ReportParser.parse` → Ok면 `Success`, Invalid면 `Failed(reason)`. 모르는 requestId는 무시.
- `onEngineError` → 해당 요청 `Failed(message)`.
- `onRenderProcessGone` → 호스트 destroy, 대기 요청 전부 Failed, 참조 null.
- 호출 코루틴이 취소되면 대기 목록에서 제거(`try/finally`).
- `WebViewEngineHost(context)`: `WebView(context.applicationContext)`, `javaScriptEnabled = true`, `allowFileAccess = false`, `allowContentAccess = false`, `addJavascriptInterface(EngineBridge(callbacks), "Android")`, `WebViewClient.onRenderProcessGone` → `callbacks.onRenderProcessGone()` 후 `true`, `loadUrl("file:///android_asset/eval-ui/engine.html")`. `evaluate`는 `webView.evaluateJavascript("window.avEvaluate(${JSONObject.quote(id)}, ${JSONObject.quote(rawJson)})", null)`.
- `EngineBridge`: `@JavascriptInterface fun onReady()`, `fun onReport(requestId: String, json: String)`, `fun onEngineError(requestId: String, message: String)` — 모두 `Handler(Looper.getMainLooper()).post { … }`로 넘긴다.
- JS 쪽 이름(Task 10과 공유): 전역 `window.avEvaluate(requestId: string, rawJson: string): void`, 브릿지 객체 `window.Android`의 `onReady()`, `onReport(requestId, json)`, `onEngineError(requestId, message)`.

테스트(`kotlinx-coroutines-test`. `runTest { val d = StandardTestDispatcher(testScheduler) … }`처럼 **runTest의 스케줄러를 공유하는** 디스패처를 `mainDispatcher`로 주입해야 `advanceTimeBy`가 먹는다, 가짜 `EngineHost`가 호출 기록과 콜백 트리거 제공). 샘플 보고서는 `report/sample-caution.json` 리소스:
1. 준비 후 요청 → `onReport` → Success, report.carId 확인
2. 준비 전 요청 2개 → `onReady` 후 둘 다 호스트로 전달되고 각자 맞는 응답을 받음(동시 요청, requestId 매칭)
3. `onReady`가 오지 않음 → 10초 후 Failed, 호스트 destroy, 다음 요청에서 hostFactory가 다시 호출됨
4. 요청 타임아웃 → Failed, 이후 늦은 `onReport`는 무시(예외 없음)
5. `onRenderProcessGone` → 대기 요청 Failed, 다음 요청에서 재생성
6. 잘못된 JSON·schemaVersion 2 → Failed
7. `onEngineError` → Failed
8. 호출 코루틴 취소 → 대기 목록에서 제거(이후 같은 id의 `onReport`가 와도 아무 일 없음)

- [ ] **Step 1: 테스트 작성 → FAIL** (`./gradlew :app:testDebugUnitTest --tests "com.car.autoverdict.engine.*"`)
- [ ] **Step 2: `EngineHost`·`RuleEngine` 구현 → PASS**
- [ ] **Step 3: `WebViewEngineHost`·`EngineBridge` 구현, `proguard-rules.pro`에 `-keepclassmembers class com.car.autoverdict.engine.EngineBridge { @android.webkit.JavascriptInterface <methods>; }` 추가(기존 NativeBridge 규칙은 Task 11에서 지운다), `./gradlew :app:assembleDebug`**
- [ ] **Step 4: 커밋** `feat(engine): add headless RuleEngine with lifecycle rules`

### Task 6: 수집기 오류 종류·진행 단계, `DefaultResultRepository` (에이전트 D)

**Files:**
- Modify: `app/src/main/kotlin/com/car/autoverdict/collector/CollectorWebView.kt`
- Modify: `app/src/test/kotlin/com/car/autoverdict/collector/CollectorWebViewTest.kt`
- Create: `app/src/main/kotlin/com/car/autoverdict/result/DefaultResultRepository.kt`
- Test: `app/src/test/kotlin/com/car/autoverdict/result/DefaultResultRepositoryTest.kt`

수집기:
- `Result.Error(val kind: CollectErrorKind, val message: String)`. `onReceivedError` → Network, 타임아웃 → Timeout, 추출·파싱 실패 → PageUnreadable. 기존 로그용 message는 유지.
- `collect(url, carId, onStage: (CollectStage) -> Unit = {}, callback)`: `loadUrl` 직전 `OpeningPage`, `fetchApis`에서 API 호출 JS를 실행하기 직전 `FetchingHistory`. 그 외 동작(45초 타임아웃, 커밋 시점 추출, 재시도, Deleted 감지)은 바꾸지 않는다.
- 기존 테스트가 `Result.Error(message)`를 쓰면 맞게 고친다.

`DefaultResultRepository(context: Context, db: AppDatabase, clock: () -> Long = System::currentTimeMillis, collectorFactory: (Context) -> CollectorWebView = ::CollectorWebView)` implements `ResultRepository`:
- `savedCar` → `db.savedCarDao().getByCarId`. `cachedRaw` → `db.cacheDao().getValid(carId, clock())?.rawInputJson`(빈 문자열이면 null).
- `collect`: `withContext(Dispatchers.Main)` + `suspendCancellableCoroutine`. 수집기를 만들고 `collect` 호출, 결과를 `CollectOutcome`으로 바꿔 resume 후 수집기 `destroy`. 취소 시(`invokeOnCancellation`) 메인 스레드에서 `destroy`.
- `writeCache(report, rawJson)`: `CacheEntity(carId, url, title=report.car.title, score, verdict, resultJson="", rawInputJson=rawJson, cachedAt=now, expiresAt=now+24h)`.
- `save(report, rawJson, savedAt)`: `SavedCarEntity` — `title=report.car.title.take(200)`, `year`, `mileageKm`, `priceWon`, `fuelType=report.car.fuel?.take(50)`, `score.coerceIn(0,100)`, `verdict`, `dangerCount = counts.danger + counts.hidden`, `cautionCount = counts.warning`, `passCount`, `unknownCount`, `rawJson`, `savedAt = savedAt ?: now`, `updatedAt = now`.
- `delete` → `deleteByCarId`.
- 테스트(Robolectric + in-memory Room, `AppDatabaseTest` 참고): writeCache 후 cachedRaw가 돌려줌·만료되면 null; save 필드 매핑(샘플 보고서로 dangerCount 등), savedAt 유지; delete. `collect`는 단위 테스트하지 않는다(WebView).

- [ ] **Step 1: 수집기 변경 + 기존 테스트 수정 → PASS**
- [ ] **Step 2: Repository 테스트 → FAIL → 구현 → PASS**
- [ ] **Step 3: 커밋 2개** `feat(collector): report error kinds and progress stages`, `feat(result): add DefaultResultRepository`

### Task 7: `ResultController` (에이전트 D)

**Files:**
- Create: `app/src/main/kotlin/com/car/autoverdict/result/ResultController.kt`
- Test: `app/src/test/kotlin/com/car/autoverdict/result/ResultControllerTest.kt`

```kotlin
class ResultController(
    private val url: String,
    private val repository: ResultRepository,
    private val evaluator: ReportEvaluator,
    private val scope: CoroutineScope,
    private val carIdOf: (String) -> String? = EncarUrl::extractCarId,
) {
    val state: StateFlow<ResultUiState>
    val events: Flow<ResultEvent>            // Channel(BUFFERED).receiveAsFlow()
    fun start()                               // 한 번만 동작(두 번째 호출 무시)
    fun retry()                               // 캐시 건너뛰고 수집부터
    fun onUserScrolled()                      // 이후 갱신은 배너로
    fun applyUpdate()                         // pendingUpdate 적용
    fun save()
    fun unsave()
}
```
동작은 스펙 5.1 그대로. 핵심:
- carId 없음 → `Error(InvalidUrl)`.
- `start`: ① `repository.savedCar(carId)`가 있으면 그 `rawJson` 평가 → `Ready(saved=true)` 후 백그라운드 수집(아래). 평가 실패면 ③으로. ② `cachedRaw`가 있으면 평가 → `Ready(saved=false)`. 평가 실패면 ③. ③ `Loading(OpeningPage)` → `collect(onStage = { Loading(stage 매핑) })` → Success면 `Loading(Evaluating)` → 평가 → Ready(+ `dataGaps` 비었으면 `writeCache`), Failed(kind) → `Error(kind 매핑)`, Deleted → `Deleted`, 평가 실패 → `Error(EngineFailed)`.
- 백그라운드 갱신(저장 차): 수집 실패·평가 실패 → 아무것도 안 함. Deleted → banner `SoldOut`. 성공했지만 새 `dataGaps.size` > 현재 → 무시. 그 외 → 사용자가 스크롤하지 않았으면 즉시 교체, 했으면 `pendingUpdate`에 보관하고 banner `UpdateAvailable`. 교체 시 `repository.save(newReport, newRaw, savedAt = 기존 savedAt)`, `dataGaps` 비었으면 `writeCache`.
- 배너 계산: `SoldOut`(삭제 감지) > `UpdateAvailable` > `LoginRequired`(dataGaps에 loginRequired) > `PartialData`(failed) > null.
- `start`에서 읽은 저장 엔티티(`SavedCarEntity?`)를 필드로 보관한다. ③ 경로의 `Ready.saved`는 이 값(이후 save/unsave로 갱신)으로 정한다. 저장 차에서 ③을 거쳐 Deleted가 오면 `Deleted` 화면이 아니라 저장 결과 `Ready` + `SoldOut` 배너, 수집·평가 실패면 저장 결과 `Ready`를 그대로 둔다(오류 화면으로 바꾸지 않음).
- `retry`: 진행 중인 작업 취소 후 ③부터(캐시 건너뜀).
- `save`/`unsave`: `saving=true` 동안 중복 무시. 성공 → `saved` 갱신 + `ResultEvent.Saved/Unsaved`, 예외 → `SaveFailed`. 저장에 쓰는 rawJson은 현재 보고서를 만든 raw.
- 테스트(가짜 Repository·Evaluator, `TestScope`/`StandardTestDispatcher`): 잘못된 URL; 캐시 → Ready; 수집 단계별 Loading 순서; 오류 3종 매핑; 평가 실패 → EngineFailed; Deleted; 누락 있는 결과는 writeCache 안 함; retry가 캐시를 건너뜀; 저장 차 즉시 Ready → 갱신 성공 시 교체 + save(savedAt 유지); 스크롤 후 갱신 → UpdateAvailable → applyUpdate로 교체; 갱신 실패·누락 증가 → 유지; 저장 차 + Deleted → SoldOut 배너; save/unsave 이벤트와 saved 플래그; 저장 여부는 DB 기준(라우트 정보 없음); 저장 차에서 retry → 삭제 매물 → 저장 결과 + SoldOut.

- [ ] **Step 1: 테스트 작성 → FAIL**
- [ ] **Step 2: 구현 → PASS**
- [ ] **Step 3: 커밋** `feat(result): add ResultController state machine`

---

## Chunk 3: Compose 화면 (에이전트 E, 웨이브 1)

에이전트 E는 새 패키지 `com.car.autoverdict.result`에 화면을 만든다. 기존 `ui/screen/ResultScreen.kt`는 건드리지 않는다(Task 11에서 교체). 0번 디자인 시스템(`AvTheme`, `ui/components/*`)만 쓴다. `DESIGN.md`와 0번 스펙 9절 문구 규칙을 따른다(단, "룰 상세는 바텀시트" 규칙은 이 화면에서 폐기).

### Task 8: 셸 보강과 화면 골격

**Files:**
- Modify: `app/src/main/kotlin/com/car/autoverdict/ui/components/AvBottomCta.kt` — `windowInsets: WindowInsets = WindowInsets.navigationBars.union(WindowInsets.ime)` 파라미터 추가(기본 동작 동일)
- Modify: `app/src/main/kotlin/com/car/autoverdict/ui/navigation/AppRoot.kt` — `ScreenHost`에 `val selectTab: (MainTab) -> Unit` 추가, `NavEvent.SelectTab`으로 연결. 이 data class를 만드는 모든 곳(테스트 포함) 수정
- Create: `app/src/main/kotlin/com/car/autoverdict/result/ResultScreen.kt`
- Modify: `app/src/main/res/values/strings.xml` (`result_` 접두사)
- Test: `app/src/test/kotlin/com/car/autoverdict/result/ResultScreenTest.kt`, AppRoot 테스트에 selectTab 1건

```kotlin
data class ResultActions(
    val onBack: () -> Unit,
    val onOpenInEncar: () -> Unit,
    val onRetry: () -> Unit,
    val onApplyUpdate: () -> Unit,
    val onUserScrolled: () -> Unit,
    val onSave: () -> Unit,
    /** Called after the user confirms the unsave dialog (dialog lives in ResultScreen). */
    val onUnsave: () -> Unit,
    val onShareSummary: () -> Unit,
    val onShareQuestions: () -> Unit,
    val onCopyQuestions: () -> Unit,
)

@Composable
fun ResultScreen(
    state: ResultUiState,
    actions: ResultActions,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    bannerAd: @Composable () -> Unit = {},
    loadingAd: @Composable () -> Unit = {},
)
```
- 바깥 `Column(Modifier.fillMaxSize().background(colors.background).navigationBarsPadding())` → `AvTopBar`(뒤로, "분석 결과", 저장됨이면 부제 "저장된 매물", "엔카에서 보기" 칩 — 기존 `OpenInEncarChip` 모양을 옮긴다) → 상태별 본문(weight 1f) → Ready일 때만 `AvBottomCta(windowInsets = WindowInsets(0))` + `bannerAd()`. 오류·삭제 화면은 `bannerAd()`만.
- Loading: `AvLoadingSteps` 3단계 "매물 페이지 열기"·"이력·진단 가져오기"·"12가지 기준으로 평가"(현재 단계 진행 중, 이전 완료) + `loadingAd()`.
- Error: `AvEmptyState` + 스펙 5.1 오류 표 문구·버튼. `AvEmptyState`는 버튼을 하나만 받으므로 E가 `secondaryActionLabel: String? = null, onSecondaryAction: (() -> Unit)? = null` 파라미터를 추가한다(기본값으로 기존 호출 유지, 갤러리·기존 테스트 통과). Deleted: "판매가 끝났거나 삭제된 매물이에요" + 돌아가기.
- Ready 본문: `LazyColumn(state = listState)`; 첫 스크롤 감지(`snapshotFlow { listState.isScrollInProgress }`가 true가 되면 `onUserScrolled` 1회).
- CTA: 공유 아이콘 버튼(contentDescription "결과 공유") + `AvButton` "저장하기"(Primary) / "저장됨"(Secondary). "저장됨" 누르면 `AvDialog`("저장을 취소할까요?", 확인 "취소하기", 닫기 "그대로 두기") 확인 후 `onUnsave`. `saving`이면 탭 무시.
- 테스트(Robolectric Compose, `sample-caution.json`을 `ReportParser`로 읽어 상태 구성): Loading 단계 문구; 오류 kind별 문구·버튼(5종); Deleted; Ready에서 저장 버튼 텍스트 2종 + 다이얼로그 흐름(확인 시 onUnsave 1회); 공유 버튼 contentDescription.

- [ ] **Step 1: AvBottomCta·ScreenHost 변경 + 기존 테스트 통과**
- [ ] **Step 2: 화면 골격 테스트 → FAIL → 구현 → PASS**
- [ ] **Step 3: 커밋** `feat(result): add Compose result screen shell and states`

### Task 9: Ready 섹션

**Files:**
- Create: `app/src/main/kotlin/com/car/autoverdict/result/sections/{ResultBanner,VerdictHeader,CountChips,FactGrid,RepairsSection,IssuesSection,TimelineSection,AllRulesSection,ExcludedSection,SellerQuestionsSection}.kt`
- Modify: `ResultScreen.kt` (LazyColumn에 섹션 배치), `strings.xml`
- Test: `app/src/test/kotlin/com/car/autoverdict/result/ResultSectionsTest.kt`

스펙 5.2의 2~11번을 위→아래로. 섹션 사이 `AvSectionDivider`, 제목은 `AvSectionHeader` + `Modifier.semantics { heading() }`. 톤 색은 `AvTheme.colors.tone(tone.toAvTone())`.
- **ResultBanner**: `ResultBanner` enum → 스펙 5.2-2 문구, UpdateAvailable는 [보기] → `onApplyUpdate()` 후 `listState.animateScrollToItem(0)`(코루틴은 `rememberCoroutineScope`), PartialData는 [다시 시도] → `onRetry`.
- **VerdictHeader**: `car.summary`(caption1, 비었으면 숨김) / 판정 배지(`AvBadge`, `verdictLabelRes(verdict)`, 톤 `AvTone.fromVerdict`) + 점수 "{score}점"(`title2`, 판정 톤 text 색) / 4dp 진행 바(배경 surfaceMuted, fill 판정 톤, 폭 score%) / `headline`(`title1`, textPrimary). 점수 Text의 contentDescription "100점 만점에 {score}점".
- **CountChips**: 위험(danger)·비공개(hidden, danger 톤)·주의·통과·미확인·평가 제외(neutral). 0이면 숨김. `FlowRow`.
- **FactGrid**: `facts`가 비면 숨김. 2열 `AvFactTile`(label, value, tone).
- **RepairsSection**: `repairs == null`이면 숨김. 제목 "교환·수리 부위". 부위 행: 이름 + 랭크 배지(있으면, 부위 tone) + 상태 텍스트. 장치 이상 행: "{group} · {name}" + 상태 배지(tone). 마지막 줄 "그 외 {okCount}개 항목 양호"(okCount>0일 때). 부위·이상이 모두 없으면 "교환·수리 부위가 없어요" 한 줄 + okCount 줄.
- **IssuesSection**: 비면 숨김. 제목 "확인할 것 {n}". 카드: 배지(label, tone) + title / message(body) / "왜 중요한가요" 소제목 + why / 판매자 질문이 있으면 "판매자에게: {질문}"(textSecondary).
- **TimelineSection**: 비면 숨김. 제목 "이력". 세로 선 + 점(tone 색) + date(caption) / label(body) / detail(caption, 있으면).
- **AllRulesSection**: 제목 "전체 항목 {rules.size}". 카테고리 소제목(카테고리 순서는 보고서 순서 그대로, 바뀌는 지점마다 소제목) + 행: 배지(label, tone) · title · message.
- **ExcludedSection**: 비면 숨김. 제목 "평가에서 빠진 항목". 행: title + reason.
- **SellerQuestionsSection**: 질문 있는 issues가 없으면 숨김. 제목 "판매자에게 물어볼 것 {n}". 번호 목록 + [복사](`onCopyQuestions`) [공유](`onShareQuestions`) 버튼(Secondary, Small).
- 테스트는 `@Config(qualifiers = "w411dp-h4000dp")`로 화면을 길게 잡아 LazyColumn이 모든 섹션을 그리게 한다(아니면 화면 밖 항목은 노드가 없다).
- 테스트: `sample-caution.json` → 헤더 headline·"89점" 표시, 칩 "주의 2"·"통과 9"·"평가 제외 1" 보이고 "위험" 칩 없음, "확인할 것 2", "전체 항목 11", "판매자에게 물어볼 것 2", 섹션 heading 노드 수; `sample-minimal.json` → 교환·수리·이력·평가에서 빠진 항목·판매자 질문 섹션 없음, LoginRequired 배너 문구; UpdateAvailable 배너 [보기] 클릭 → `onApplyUpdate`.

- [ ] **Step 1: 테스트 → FAIL**
- [ ] **Step 2: 섹션 구현 → PASS**
- [ ] **Step 3: debug 빌드 후 `ComponentGalleryScreen`에 결과 화면 미리보기 항목 추가는 하지 않는다(YAGNI). `./gradlew :app:assembleDebug` 통과 확인**
- [ ] **Step 4: 커밋** `feat(result): render report sections fully expanded`

---

## Chunk 4: 연결과 제거 (에이전트 F, G, 웨이브 2)

### Task 10: 엔진 엔트리·IIFE 빌드·React 제거 (에이전트 F)

**Files:**
- Create: `webview-bundle/src/engine.ts`, `webview-bundle/src/engine-bridge.ts`, `webview-bundle/public/engine.html`
- Delete: `webview-bundle/index.html`, `src/App.tsx`, `src/main.tsx`, `src/overlay/`, `src/android-bridge.ts`, `src/rule-meta.ts`, `src/verdict-meta.ts`, `src/tokens.ts`, `src/types/android.d.ts`(새 타입으로 대체), `tests/app-callbacks.test.tsx`, `tests/overlay/`, `tests/android-bridge.test.ts`, `tests/setup.ts`(jest-dom용이면)
- Modify: `webview-bundle/package.json`, `vite.config.ts`, `tsconfig.json`, `app/build.gradle.kts`(buildWebView 입력 `index.html` → `public/engine.html`)
- Test: `webview-bundle/tests/engine.test.ts`

```ts
// engine-bridge.ts
interface AndroidEngineBridge { onReady(): void; onReport(requestId: string, json: string): void; onEngineError(requestId: string, message: string): void }
export const bridge = { ready(), report(id, json), error(id, message) } // window.Android 없으면 아무것도 안 함
// engine.ts
export function handleEvaluate(requestId: string, rawJson: string): void // JSON.parse → orchestrateMobile → buildReport → bridge.report(id, JSON.stringify(report)); 예외 → bridge.error(id, String(e))
if (typeof window !== 'undefined') { window.avEvaluate = handleEvaluate; bridge.ready(); } // node 테스트에서 import해도 안전하게
// bridge는 globalThis.window?.Android 로 접근
```
- `vite.config.ts`: React 플러그인 제거. `build.lib = { entry: 'src/engine.ts', formats: ['iife'], name: 'AvEngine', fileName: () => 'engine.js' }`, `outDir: 'dist'`. `engine.html`은 **`public/engine.html`**에 두고 `copyPublicDir: true`로 `dist/`에 복사한다. 결과: `dist/engine.js` + `dist/engine.html`(`<script src="engine.js"></script>`만, 모듈 아님).
- test 환경 `environment: 'node'`, `setupFiles` 제거. `tsconfig.json`에서 `jsx` 제거.
- `package.json`: `react`, `react-dom`, `@testing-library/*`, `@types/react*`, `@vitejs/plugin-react`, `jsdom`을 목록에서 지운다. **`npm install`은 실행하지 않는다**(모든 worktree가 조율자 `node_modules`를 공유하므로 G가 깨진다). lock 갱신·prune은 웨이브 2 병합 후 조율자가 Task 12에서 한다. 남아 있는 패키지는 해가 없다. `copy-to-assets`의 `sed` 줄 제거(crossorigin 없음).
- `tests/engine.test.ts`: `globalThis.window = { Android: fake }` 형태로 픽스처 raw를 `handleEvaluate`에 넣어 `onReport`가 같은 requestId로 schemaVersion 1 JSON을 받는지, 깨진 JSON이면 `onEngineError`가 불리는지.
- [ ] **Step 1: 테스트 → FAIL → 구현 → PASS**
- [ ] **Step 2: 제거·설정 변경 후 `npm run build && npm run copy-to-assets` → `app/src/main/assets/eval-ui/`에 `engine.html`, `engine.js`만 있는지 확인. `npx vitest run`, `npx tsc --noEmit` 통과**
- [ ] **Step 3: 커밋 2개** `feat(webview): add headless engine entry built as IIFE`, `refactor(webview): remove React result UI`

### Task 11: 연결과 구 코드 제거 (에이전트 G)

**Files:**
- Create: `app/src/main/kotlin/com/car/autoverdict/result/ResultRoute.kt`
- Modify: `AutoVerdictApp.kt`(`val ruleEngine by lazy { RuleEngine({ WebViewEngineHost(this) }) }`, `val resultRepository by lazy { DefaultResultRepository(this, database) }`), `ui/navigation/AppScreenContent.kt`, `proguard-rules.pro`
- Delete: `ui/screen/ResultScreen.kt`, `webview/EvalWebView.kt`, `webview/NativeBridge.kt`, `test/.../webview/EvalWebViewTest.kt`, `NativeBridgeTest.kt`
- Test: `app/src/test/kotlin/com/car/autoverdict/result/ResultRouteTest.kt`(가능한 범위)

```kotlin
@Composable
fun ResultRoute(
    url: String,
    host: ScreenHost,
    repository: ResultRepository,
    evaluator: ReportEvaluator,
    bannerAd: @Composable () -> Unit,
    loadingAd: @Composable () -> Unit,
)
```
- `val scope = rememberCoroutineScope()`; `val controller = remember(url) { ResultController(url, repository, evaluator, scope).also { it.start() } }`. `state` collect(`collectAsStateWithLifecycle`). `events` → `LocalAvToast.current.show(...)`: Saved → "저장했어요" + 액션 "보기" → `host.selectTab(MainTab.Saved)`; Unsaved → "저장을 취소했어요"; SaveFailed → "저장하지 못했어요".
- 공유: `Intent(ACTION_SEND).setType("text/plain").putExtra(EXTRA_TEXT, …)` → `Intent.createChooser(…, null)`. 복사: `ClipboardManager.setPrimaryClip(ClipData.newPlainText("판매자 질문", text))` + 토스트 "복사했어요".
- 엔카에서 보기: `ACTION_VIEW` + url.
- `AppScreenContent`: `val app = LocalContext.current.applicationContext as AutoVerdictApp`로 얻고 `is Route.Result -> ResultRoute(url = route.url, host = host, repository = app.resultRepository, evaluator = app.ruleEngine, bannerAd = { AdBanner(Modifier.fillMaxWidth()) }, loadingAd = { AdMediumRectangle() })`. `route.fromSaved`는 쓰지 않는다(저장 여부는 DB). `Route.Result`의 `fromSaved` 필드는 직렬화 호환을 위해 남겨 둔다.
- ProGuard: NativeBridge 규칙 제거(EngineBridge 규칙은 Task 5에서 추가됨). kotlinx.serialization: 1.9.0은 consumer rules를 포함하므로 `@Serializable` 클래스용 규칙만 확인(`-keepclassmembers class com.car.autoverdict.report.** { *** Companion; kotlinx.serialization.KSerializer serializer(...); }`, `-keep,includedescriptorclasses class com.car.autoverdict.report.**$$serializer { *; }`).
- `grep -rn "EvalWebView\|NativeBridge\|eval-ui/index.html" app/src` → 결과 없음.
- [ ] **Step 1: 연결 구현, 구 코드·테스트 삭제**
- [ ] **Step 2: `./gradlew :app:testDebugUnitTest :app:assembleDebug` 통과, `./gradlew :app:assembleRelease`(서명 키 없으면 `:app:minifyReleaseWithR8`까지)로 R8 경고 확인**
- [ ] **Step 3: 커밋** `feat(result): wire Compose result screen and remove EvalWebView`

---

## Chunk 5: 검증과 문서 (조율자, 웨이브 3)

### Task 12: 전체 검증과 크기

- [ ] `cd webview-bundle && npm install`(Task 10에서 지운 의존성 prune, lock 갱신) 후 `npx vitest run`
- [ ] `JAVA_HOME=… ./build-all.sh` 통과
- [ ] 테스트 수 기록(Kotlin, vitest)
- [ ] release AAB 크기 측정, `docs/superpowers/plans/2026-10-17-ux-revamp-0-baseline.md`의 0번 최종값(10,275,681)과 비교해 같은 파일에 1번 항목으로 기록
- [ ] `git grep -n "src/core" -- webview-bundle/src/core` 변경 없음 확인: `git diff f996862 --stat -- webview-bundle/src/core app/src/main/kotlin/com/car/autoverdict/db` → 비어 있어야 한다

### Task 13: 에뮬레이터 QA

`AV_API_36`, `-no-snapshot -port 5580`, serial `emulator-5580`만. 이미 떠 있으면(조율자가 0번 QA 후 남겨 둔 것) 그대로 쓰고 끄지 않는다.
- [ ] 두 URL(39339524, 41878112) 공유·붙여넣기로 열기: 로딩 단계 → 결과. 헤더·칩·사실·교환 부위·확인할 것·이력·전체 항목·빠진 항목·판매자 질문 확인(엔카 화면과 사고 건수·소유자 변경 대조, 사고 `type` 의미 확인)
- [ ] 저장 → 토스트 "보기" → 저장 탭. 저장 차 다시 열기(즉시 표시) → 갱신. 비행기 모드로 저장 차 열기(저장 결과 유지), 저장 안 한 차 열기(Network 오류 → 다시 시도)
- [ ] 결과 공유, 판매자 질문 복사·공유
- [ ] 큰 글꼴(1.3x)에서 헤더·칩 줄바꿈
- [ ] TalkBack 없이 `uiautomator dump`로 heading 확인(선택)
- [ ] 끝나면 최신 빌드를 설치해 둔 채 에뮬레이터를 남긴다(사용자 확인용)

### Task 14: 문서

- [ ] `CLAUDE.md` Data Flow·Native↔WebView Bridge 절을 새 구조로 고친다(RuleEngine, EvaluationReport, ResultController)
- [ ] 스펙 끝에 "10. 완료 기록"(커밋 범위, 테스트 수, 크기, QA 결과, 남은 일) 추가
- [ ] 커밋 `docs: record sub-project 1 completion`
