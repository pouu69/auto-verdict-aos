# UX/UI 개편 3번 — 저장한 차 & 비교 설계

- 작성일: 2026-10-18
- 기준: `main` f73fdbd (1번 결과 화면 병합 후)
- 선행: 0번 디자인 언어(`2026-10-17-ux-revamp-0-design-language-design.md`), 1번 결과 화면(`2026-10-17-ux-revamp-1-result-screen-design.md`)
- 상태: 설계 확정(사용자 부재 중 자율 결정, 결정마다 근거 기록), 구현 계획 작성 전

## 1. 목표

저장한 차 탭과 비교 화면을 0번 디자인 언어로 다시 만들고, 결과 화면과 같은 말·같은 숫자를 쓰게 한다.

- 저장 목록 한 줄만 보고도 "어떤 차, 판정, 점수, 문제 개수, 언제 저장"을 알 수 있게 한다.
- 결과 화면의 라벨(위험·비공개·주의·통과·미확인)을 목록과 비교 화면에서 그대로 쓴다. 지금은 목록이 "양호 9", 결과가 "통과 9"라 같은 숫자를 다른 말로 부른다.
- 같은 차가 두 번 저장된 기존 데이터(엔카 "더미 광고" id와 원래 매물 id)를 화면에서 한 대로 보여준다. DB는 건드리지 않는다.
- 비교는 2~3대를 나란히 놓고, 나은 값은 표시하되 판단을 대신하는 문구는 쓰지 않는다. 비교에서 각 차의 결과 화면으로 갈 수 있게 한다.
- 0번 14절 후속 과제(비교 화면 "미확인" 칩, 가격 행 굵기, ▲ 기호 글꼴, 포커스 링 잘림)를 이 화면들에서 정리한다.

## 2. 결정 사항

| 항목 | 결정 | 근거 |
|---|---|---|
| DB 스키마 | **변경 금지.** DAO 메서드 추가만 한다(쿼리 추가는 스키마 변경이 아님) | `fallbackToDestructiveMigration`이라 스키마가 바뀌면 저장한 차가 지워진다(1번 2절) |
| 판정·점수 로직, vendored core | **변경 금지** | 1번과 같은 원칙 |
| 개수 라벨 | 결과 화면 문자열(`result_count_*`)을 그대로 재사용. "양호"는 쓰지 않는다 | 용어 통일. 문자열이 하나면 다시 어긋나지 않는다 |
| 목록의 위험·비공개 구분 | 저장 행은 `dangerCount = 위험 + 비공개` 하나뿐이다. **판정이 NEVER가 아니면** 위험은 0이므로(코어: killer가 있으면 반드시 NEVER) `dangerCount`를 전부 "비공개"로, **NEVER이고 1개면** "위험 1", **NEVER이고 2개 이상**이면 구분할 수 없어 "위험·비공개 n"으로 쓴다(4.3절) | 스키마를 바꾸지 않고, 목록에서 엔진을 돌리지 않으면서, 사실과 다른 라벨을 붙이지 않는 방법. "괜찮아요" 옆에 "위험 2"가 뜨는 현재 오표기(비공개 2를 위험으로 부름)를 없앤다 |
| 목록 데이터 원천 | **저장 행의 컬럼만** 쓴다. 목록에서는 규칙 엔진을 돌리지 않는다 | 탭 진입마다 엔진(화면 없는 WebView)을 깨우지 않는다. 엔진 결과로 나중에 덮으면 제목·개수가 깜빡인다 |
| 비교 데이터 원천 | 고른 2~3대의 `rawJson`을 **엔진으로 다시 평가**해 보고서로 그린다. 평가에 실패한 차만 저장 행 컬럼으로 대체 | 대수가 적어 비용이 작고, 위험·비공개 분리·핵심 사실·확인할 것·`car.name`을 정확히 보여줄 수 있다. 결과 화면도 저장 차를 다시 평가하므로 두 화면 숫자가 같다 |
| 중복 저장 | **화면에서 묶는다.** 식별자 = `rawJson`의 `preloadedState.cars.base.vehicleId`(없으면 `carId`). 같은 식별자의 행은 목록·탭 개수·비교에서 한 대 | 일반 매물은 `vehicleId == carId`(픽스처 4건 확인), 더미 광고는 원래 차의 id를 가리킨다. 저장된 데이터만으로 판별 가능, 스키마 무변경 |
| 비교 대수 | **2~3대** | 360dp 폭에서 3열까지 값이 읽힌다(6.2절). 4대 이상은 가로 스크롤이 필요해 제외 |
| 비교 레이아웃 | 열 비교(나란히). 행 라벨은 왼쪽 열이 아니라 **값 위 한 줄**로 둔다. 차 이름 줄은 위에 고정 | 왼쪽 라벨 열(현재 72dp)을 없애야 3열이 들어간다. 스크롤해도 어느 열이 어느 차인지 보인다 |
| 나은 값 표시 | 객관적 순서가 있는 항목만, **유일한 1등만** `accentWeak` 배경 + `accentText` SemiBold. 동점·값 부족이면 표시 없음. "가장 나쁜 값" 표시와 종합 1등("TOP", "추천")은 없앤다 | 점수 1등이 곧 "살 차"는 아니다. 판단을 대신하지 않고 사실만 짚는다 |
| 가격 | **나은 값 표시 없음**, 다른 행과 같은 굵기 | 싼 차가 나은 차라는 뜻이 아니다. 0번 14절 "가격 행 굵기" 후속 과제 해소 |
| ▲ 기호 | 쓰지 않는다. 배경·굵기 + 접근성 문구로 대신한다 | 0번 14절 "▲ 글꼴 대체" 과제를 기호 제거로 해소. 색만으로 의미를 전하지 않도록 낭독 문구를 붙인다 |
| 정렬 | 최근 저장순(기본) · 점수 높은 순 · 가격 낮은 순. `AppPreferences`에 저장 | "점수 낮은 순"은 쓸 일이 드물고, 가격은 차 고를 때 자주 보는 기준. 탭을 오가도 유지 |
| 삭제 | **왼쪽으로 밀어 삭제 + 되돌리기 토스트.** 확인 다이얼로그 없음. 접근성 사용자 동작 "삭제"를 함께 제공 | 0번 로드맵의 "스와이프 삭제". 되돌릴 수 있으면 확인을 묻지 않는 편이 토스 방식에 맞다 |
| 비교 진입 | 상단바 텍스트 버튼 "비교" → 선택 모드 → 하단 `AvBottomCta` "{n}대 비교하기" | 기존 아이콘(CompareArrows)은 뜻이 잘 안 읽히고, 기존 FAB는 디자인 시스템 밖 |
| 비교 → 결과 | 각 열 머리(이름·판정 영역)를 누르면 `Route.Result(url, fromSaved = true)` push | 0번 7.1절에서 3번으로 미룬 경로 |
| 광고 | 위치 유지: 저장한 차 탭은 탭바 위 배너(AppRoot), 비교 화면은 광고 없음(기존과 같음) | 0번 10절 |

## 3. 데이터 계약 — 중복 저장과 식별자

### 3.1 현재 상황

- 수집기는 사용자가 연 URL의 id를 `carId`로 쓰고(DB·캐시 키, 저장 URL), API 호출만 `base.vehicleId`로 한다(`CollectorWebView.fetchApis` 주석). 그래서 같은 차를 더미 광고 URL과 원래 URL로 각각 저장하면 행이 둘 생긴다.
- 저장되는 `rawJson`은 수집기 페이로드 전체(`url`, `carId`, `preloadedState.cars`, `recordJson` …)이고, 1번 이전(React UI) 저장 행도 같은 형식이다. 1번 이전 행의 집계 매핑도 지금과 같다(`dangerCount = killer + fail`, 삭제된 `App.tsx`에서 확인).

### 3.2 식별자 규칙

```
savedIdentity(row) = vehicleIdOf(row.rawJson) ?: row.carId
vehicleIdOf(raw)   = raw.preloadedState.cars.base.vehicleId   // 숫자 또는 숫자 문자열, > 0 → 10진 문자열
```

- 파싱 실패, 필드 없음, 0 이하이면 `carId`로 대체한다(옛 행·손상 행도 목록에서 사라지지 않는다).
- 픽스처 4건(39339524, 41623744, 41878112, 42264483) 모두 `vehicleId == carId`라 일반 매물에서는 결과가 바뀌지 않는다.

### 3.3 저장 경로 변경(다른 작업)과의 약속

다른 에이전트가 새 저장을 정규 키 하나로 쓰도록 저장 경로를 바꾸고 있다. 3번은 다음만 가정한다.

| 항목 | 3번이 기대하는 것 |
|---|---|
| `rawJson` | 지금처럼 수집기 페이로드 원문을 저장한다. 특히 `preloadedState.cars.base.vehicleId`가 남아 있어야 한다(**3번이 의존하는 유일한 필드**) |
| 새 행의 `carId` | 정규 키(= `vehicleId`, 없으면 URL id)를 권장한다. 다른 값을 써도 3번 묶기는 `rawJson`으로 판별하므로 동작한다 |
| 새 행의 `url` | 결과 화면을 다시 열 수 있는 엔카 상세 URL(`EncarUrl.buildDetailUrl`). 목록·비교는 이 값을 그대로 `Route.Result`에 넘긴다 |
| 집계 컬럼 | 1번 4절 매핑 유지(`dangerCount = danger + hidden`, `cautionCount = warning`, `passCount`, `unknownCount`) |
| 기존 쌍둥이 행 정리 | 저장 경로가 저장 시 쌍둥이 행을 지우든 말든 3번 묶기는 같은 결과를 낸다(멱등). 3번은 **DB에서 행을 합치거나 지우지 않는다**(사용자가 삭제할 때만 묶음 전체 삭제) |

### 3.4 묶음 규칙

- 같은 식별자의 행들을 한 **항목**(`SavedCarItem`)으로 묶는다. 항목은 대표 행 하나와 묶인 `carIds` 전체를 가진다.
- 대표 행: `updatedAt`이 가장 큰 행(가장 최근 평가). 같으면 `carId == 식별자`인 행, 그래도 같으면 `carId` 사전순 첫 행.
- 화면의 모든 값(이름, 요약, 판정, 점수, 집계, 저장일, URL)은 대표 행에서 읽는다.
- 삭제는 묶음의 `carIds` 전체를 지운다(하나만 지우면 숨어 있던 쌍둥이가 다시 나타난다). 되돌리기는 지운 행들을 그대로 다시 쓴다.
- 비교에 넘기는 id는 대표 행의 `carId`다. 같은 차가 두 열로 나오지 않는다.
- 탭바 "저장한 차 {n}"의 n도 **항목 수**다(지금은 `countFlow()` 행 수라 목록과 어긋날 수 있다).

### 3.5 구현 위치

- `saved/SavedIdentity.kt`: `vehicleIdOf(rawJson): String?` 순수 함수. `kotlinx.serialization`의 `Json.parseToJsonElement`로 읽는다(이미 의존성 있음, JVM 단위 테스트 가능).
- `saved/SavedCarsRepository.kt`(앱 범위, `AutoVerdictApp`에서 lazy):
  - `items: Flow<List<SavedCarItem>>` — `savedCarDao().getAllFlow()`를 받아 `Dispatchers.Default`에서 식별자를 구하고 묶는다. 식별자는 `(carId, updatedAt)` 키로 메모리에 기억해 행이 바뀔 때만 다시 파싱한다.
  - `itemCount: Flow<Int>` — 탭바용(`items.map { it.size }.distinctUntilChanged()`).
  - `delete(item): List<SavedCarEntity>` — 지운 행들을 돌려준다(되돌리기용).
  - `restore(rows)` — `upsertAll`로 원래 값(`savedAt` 포함) 그대로 복원.
  - `observe(carIds): Flow<List<SavedCarEntity>>` — 비교 화면용.
- `SavedCarDao` 추가: `deleteByCarIds(carIds)`, `upsertAll(rows)`(`@Insert(REPLACE)`), `getByCarIdsFlow(carIds)`. 엔티티·테이블은 그대로.
- `MainActivity`의 `savedCount`는 `countFlow()` 대신 `itemCount`를 쓴다. `countFlow()`는 다른 사용처가 없으면 남겨 두기만 한다(삭제는 선택).

## 4. 저장한 차 목록

### 4.1 구성

```
┌──────────────────────────────────────┐
│ 저장한 차                       비교 │  AvTopBar(onBack = null), 항목 2개 이상일 때 "비교"
├──────────────────────────────────────┤
│ 5대                    최근 저장순 ⌄ │  개수(caption1 textTertiary) + 정렬 버튼(textSecondary)
│                                      │
│ BMW 5시리즈                 [주의]   │  이름 body1 textPrimary          판정 배지(AvBadge)
│ 2015년 · 9.8만km · 디젤 · 1,290만원  72점│  요약 body2 textTertiary          점수 body2 SemiBold, 판정 톤 text
│ [주의 2] [통과 8]         10월 18일 저장│  개수 배지(caption2)              저장일 caption1 textTertiary
│ ──────────────────────────────────── │  (행 사이 구분선 없음, 세로 여백 4dp)
│ 현대 아반떼                 [괜찮아요]│
│ ...                                  │
└──────────────────────────────────────┘
  배너 광고 · 탭바 (AppRoot, 변경 없음)
```

- 바탕은 흰색(`background`). 지금의 회색 바탕 + 흰 카드 + 테두리는 없앤다(0번 원칙: 리스트 행 중심, 테두리 카드 없음).
- 행은 화면 로컬 컴포저블 `SavedCarRow`(`ui/saved/`)로 만든다. 3줄이라 `AvListRow`(2줄)에 맞지 않는다. 색·글꼴·모양은 토큰만 쓴다.
- **포커스 링 잘림 해소(0번 14절)**: 행 바깥 좌우 여백 8dp, 행 안쪽 좌우 12dp(글자는 화면 끝에서 20dp로 기존과 같음). `avPressable(focusShape = AvTheme.shapes.tile)`의 링(바깥 5dp)이 화면 안에 들어온다. `LazyColumn` 위아래 `contentPadding`도 8dp 이상.
- 눌림 반응은 `avPressable`(96% 축소). 리플 없음.

### 4.2 행 내용

| 요소 | 값 | 비고 |
|---|---|---|
| 이름 | 대표 행 `title`에서 앞의 4자리 연식과 공백을 뗀 값("2015 BMW 5시리즈" → "BMW 5시리즈"). 비었거나 "차량 정보 없음"이면 "차량 정보 없음" | 연식은 요약 줄에 있다. 저장 행에는 1번의 `car.name`(등급 포함)이 없어 `title`을 쓴다. "매물 #id" 같은 개발자식 대체는 쓰지 않는다. 1줄, 넘치면 말줄임 |
| 요약 | `{연식}년 · {주행} · {연료} · {가격}`, 값이 없는 칸은 빼고, 모두 없으면 줄 숨김 | 결과 헤더 `car.summary`와 같은 형식. 주행 "9,500km"/"9.8만km"/"12만km"(1번 `formatMileage`와 같은 규칙), 가격 "1,290만원"(만원 단위, 원 단위 버림). 1줄 말줄임 |
| 판정 배지 | `AvBadge(verdictLabel, AvTone.fromVerdict(verdict))` — 피하세요·주의·괜찮아요·확인 필요 | 알 수 없는 값은 확인 필요(Neutral) |
| 점수 | "{score}점", `body2` SemiBold, 판정 톤 `*Text` 색 | 점수 그라디언트(하드코딩 RGB, `COLOR_STOPS`)는 삭제 |
| 개수 배지 | 4.3절. 0인 배지는 숨김. `FlowRow`로 줄바꿈 | 결과 화면 `CountChips`와 같은 순서·톤·문자열 |
| 저장일 | 대표 행 `savedAt`. 올해면 "10월 18일 저장", 아니면 "2025년 10월 18일 저장" | `updatedAt`(백그라운드 갱신 시각)이 아니라 사용자가 저장한 날 |

### 4.3 개수 배지 규칙

저장 행 컬럼만으로 결과 화면 라벨을 붙인다.

| 컬럼 | 판정 | 배지 |
|---|---|---|
| `dangerCount` | NEVER가 아님 | "비공개 n" (Danger) — killer가 있으면 판정이 NEVER이므로 전부 비공개 |
| `dangerCount` | NEVER, n = 1 | "위험 1" (Danger) |
| `dangerCount` | NEVER, n ≥ 2 | "위험·비공개 n" (Danger) — 나눌 수 없음을 그대로 표기 |
| `cautionCount` | — | "주의 n" (Warning) |
| `passCount` | — | "통과 n" (Success) |
| `unknownCount` | — | "미확인 n" (Neutral) |

- 순서: 위험 → 위험·비공개 → 비공개 → 주의 → 통과 → 미확인. "평가 제외"는 저장 컬럼에 없어 목록에는 없다(결과·비교 화면에는 있음).
- 이 규칙은 순수 함수(`savedCountBadges(row)`)로 두고 단위 테스트한다. NEVER 판정 조건(killer ≥ 1)은 코어 `rules/index.ts`의 `evaluate`에서 확인했다. 코어가 바뀌면 이 테스트의 근거 주석을 갱신한다.

### 4.4 정렬

- 개수 줄 오른쪽 "최근 저장순 ⌄"(`AvButton` Secondary가 아니라 텍스트 버튼 형태: `body2` `textSecondary` + `Icons.Rounded.KeyboardArrowDown`, 48dp 터치) → `AvBottomSheet(title = "정렬")`에 라디오 행 3개(`AvListRow` + 선택 표시 `Icons.Rounded.Check` `accentText`, `Role.RadioButton`).
- 최근 저장순: `savedAt` 내림차순. 점수 높은 순: `score` 내림차순, 같으면 `savedAt` 내림차순. 가격 낮은 순: `priceWon` 오름차순, 가격 없는 차는 맨 뒤, 같으면 `savedAt` 내림차순.
- 선택은 `AppPreferences`(`saved_sort`, 기본 recent)에 저장한다. 알 수 없는 값은 기본으로.

### 4.5 비교 선택 모드

- 진입: 상단바 "비교"(텍스트 버튼, `buttonM` `accentText`). 항목이 2개 미만이면 숨긴다.
- 모드 중 상단바: 제목 "비교할 차 선택", 오른쪽 "취소". 시스템 뒤로가기도 모드만 끝낸다(화면 안 `BackHandler`, AppRoot 것보다 나중에 등록되어 우선함 — 구현 시 확인).
- 행 앞에 선택 원(24dp, 선택 시 `accent` 채움 + 흰 체크 / 미선택 시 `border` 테두리). 행 전체가 토글, `Role.Checkbox`, `toggleableState`.
- 최대 3대. 4번째를 누르면 선택하지 않고 토스트 "최대 3대까지 비교할 수 있어요".
- 하단 `AvBottomCta(windowInsets = WindowInsets(0))` — 탭 루트라 내비게이션 바 인셋은 탭바가 가진다(0번 6절). 버튼 Primary L: 0~1대 선택이면 비활성 "2대 이상 골라 주세요", 2~3대면 "{n}대 비교하기". 누르면 `Route.Compare(대표 carIds, 화면 표시 순서)` push, 모드 종료.
- 선택 모드에서는 밀어서 삭제와 정렬 버튼을 끈다. 개수 줄은 "{n}대 선택"으로 바뀐다.
- 선택 상태는 `rememberSaveable`(회전 유지). 선택한 차가 다른 경로로 지워지면 선택에서 뺀다.
- 기존 모드 배너(`CompareModeBanner`)와 FAB는 없앤다.

### 4.6 삭제

- 행을 왼쪽으로 밀면 뒤에 `dangerBg` 바탕 + `Icons.Rounded.Delete`(`dangerText`)가 드러나고, 끝까지 밀면 삭제한다(M3 `SwipeToDismissBox`, 끝→시작 방향만). 행 모양(tile 12dp)으로 clip.
- 삭제 즉시 목록에서 빠지고(DB 삭제 → Flow 갱신) 토스트 "삭제했어요" + 액션 "되돌리기". 되돌리기는 지운 행 전체를 원래 값(`savedAt`·`updatedAt` 포함)으로 다시 쓴다. 다시 쓰면 같은 정렬 위치로 돌아온다.
- 되돌리기 토스트는 길게(`SnackbarDuration.Long`) 띄운다: `AvToastState.show`에 `long: Boolean = false` 파라미터를 추가한다(기본 동작 불변). 접근성 서비스가 켜져 있으면 M3가 시간을 더 늘린다.
- 실패: 삭제 실패 "삭제하지 못했어요", 복원 실패 "되돌리지 못했어요".
- 접근성: 행에 `customActions = [삭제]`. TalkBack 사용자는 밀지 않고 삭제할 수 있다.
- 토스트는 AppRoot 범위에서 돌아 화면을 떠나도 되돌리기가 동작한다. 저장소가 앱 범위라 콜백이 화면에 묶이지 않는다.
- 결과 화면이 같은 차를 열고 있는 경우는 없다(결과 화면 위로 다른 화면을 쌓지 않음).

### 4.7 빈 상태·긴 목록

- 빈 상태: `AvEmptyState(icon = Icons.Rounded.BookmarkBorder, title = "저장한 차가 없어요", description = "분석 결과에서 저장하면 여기서 모아 보고 비교할 수 있어요", actionLabel = "차 분석하러 가기")` → `host.selectTab(MainTab.Home)`. 상단바 "비교"와 개수 줄은 숨긴다.
- 목록을 처음 읽는 동안(첫 Flow 값 전)은 아무것도 그리지 않는다(빈 상태가 잠깐 깜빡이지 않게 `initial = null`로 구분).
- 긴 목록: `LazyColumn`, 키는 식별자. 페이지 나눔·검색은 하지 않는다(저장 수가 수십 대 수준). 선택 모드에서는 하단 CTA 높이만큼 아래 여백.

## 5. 결과 화면 열기

- 목록 행 누름 → `Route.Result(대표 행 url, fromSaved = true)`. 결과 화면은 DB에서 저장 행을 찾아 `rawJson`을 다시 평가해 즉시 보여주고 백그라운드로 갱신한다(1번 5.1절). 저장 행 조회 키는 URL의 id이므로 대표 행의 `url`·`carId`가 짝이 맞아야 한다(3.3절 약속).
- **1번 이전에 저장한 행**: 같은 수집기 페이로드라 다시 평가된다. 엔진이 못 읽으면 결과 화면이 새로 수집한다(1번 `ResultController.start`). URL에 추적 파라미터가 붙어 있어도 `EncarUrl.extractCarId`가 처리한다. 3번에서 따로 이전 작업을 하지 않는다.
- 결과 화면에서 "저장됨"을 눌러 저장을 취소하고 돌아오면 Flow로 목록에서 빠진다. 백그라운드 갱신으로 점수가 바뀌면 목록도 바뀐다(`updatedAt` 변경 → 식별자 메모 재계산).

## 6. 비교 화면

### 6.1 데이터

- 입력: `Route.Compare(carIds)`(2~3개, 3개 넘으면 앞 3개).
- `CompareController`(일반 클래스, `ResultController`와 같은 수명 규칙: `AppScreenContent`에서 `remember(route)` + `rememberCoroutineScope`):
  1. `SavedCarsRepository.observe(carIds)`로 행을 구독(입력 순서 유지).
  2. 행마다 `ReportEvaluator.evaluate(rawJson)`을 동시에 요청한다. `(carId, updatedAt)`이 같으면 다시 평가하지 않는다.
  3. 모두 끝나면(성공 또는 실패) `Ready(columns)`. 성공한 차는 보고서 값, 실패한 차는 저장 행 컬럼(`fallback = true`).
- 상태: `Loading` → `Ready(columns)` / `TooFew`(남은 차가 2대 미만 — 결과 화면에서 저장 취소하고 돌아온 경우 등).
- 엔진 비용: 2~3건, 엔진 수명·타임아웃은 1번 3.3절 규칙을 그대로 따른다(초기화 10초, 요청 10초). 로딩은 보통 1초 안쪽이다.

### 6.2 레이아웃

```
┌──────────────────────────────────────┐
│ <  차량 비교                          │  AvTopBar(onBack, title, subtitle = "{n}대")
├──────────────────────────────────────┤
│ BMW 5시리즈 520d │ 현대 아반떼 CN7   │  ← 이름 줄, 스크롤해도 위에 고정(stickyHeader)
├──────────────────┼───────────────────┤
│ [주의]           │ [괜찮아요]        │  판정 배지
│ 72점             │ 86점 ■            │  점수 title2, 판정 톤 text 색 / 1등은 accentWeak 배경
│ ▬▬▬▬▬▬▬░░░       │ ▬▬▬▬▬▬▬▬░        │  얇은 진행 바(판정 톤 fill)
│ 결과 보기 >      │ 결과 보기 >       │  accentText caption1, 열 머리 전체가 누름 영역
│▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓│  AvSectionDivider
│ 기본 정보                             │  AvSectionHeader (heading)
│ 가격                                  │  행 라벨 caption1 textTertiary, 값 위 한 줄
│ 1,290만원        │ 1,850만원         │  값 body2 textPrimary (표시 없음)
│ 연식                                  │
│ 2015년           │ 2021년 ■          │
│ 주행거리 · 연료 …                     │
│▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓│
│ 점검 결과                             │
│ 위험 / 비공개 / 주의 / 통과 / 미확인 / 평가 제외 │
│▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓│
│ 핵심 사실 (사고 · 소유자 변경 · 용도 · 침수·전손) │
│▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓│
│ 확인할 것                             │
│ 주의 소유자 변경 │ 없어요            │
│ 주의 자차보험 공백│                   │
│▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓│
│ 파란 표시는 … (안내 caption1)          │
└──────────────────────────────────────┘
```

- 바탕 흰색, 섹션 사이 `AvSectionDivider`(8dp 띠). 테두리 카드·그림자·세로 막대 장식(현재 `CompareSection`)은 없앤다.
- 열: 좌우 화면 여백 20dp, 열 간격 12dp, 열 폭은 균등(`weight(1f)`). 360dp에서 3열이면 열당 약 101dp.
- 행: 라벨 한 줄(`caption1` `textTertiary`, 화면 폭 전체) → 값 줄(열마다). 행 사이 세로 12dp. 값은 최대 2줄, 넘치면 말줄임. 숫자에는 tnum(`fontFeatureSettings = "tnum"`)을 유지한다(0번 14절).
- 이름 줄: `body2` SemiBold, 열마다 최대 2줄. 보고서가 있으면 `car.name`, 없으면 4.2절 이름 규칙. stickyHeader 배경은 `background`, 아래 1dp `border`.
- 열 머리(판정 배지·점수·진행 바·"결과 보기")는 열마다 하나의 누름 영역(`avPressable`, `onClickLabel = "결과 보기"`) → `host.navigate(Route.Result(url, fromSaved = true))`.

### 6.3 행과 값

| 섹션 | 행 | 값 | 나은 값 표시 |
|---|---|---|---|
| (열 머리) | 판정 | `AvBadge(verdictLabel, tone)` | 없음 |
| (열 머리) | 점수 | "{score}점" `title2`, 판정 톤 text + 진행 바 | 가장 높음 |
| 기본 정보 | 가격 | "1,290만원" | **없음** |
| 기본 정보 | 연식 | "2015년" | 가장 최근 |
| 기본 정보 | 주행거리 | "9.8만km" | 가장 적음 |
| 기본 정보 | 연료 | "디젤" | 없음 |
| 점검 결과 | 위험 · 비공개 · 주의 · 미확인 | 0이면 "0"(`textTertiary`), 1 이상이면 해당 톤 `AvBadge` 숫자. 미확인은 Neutral(`neutralBg`, 0번 14절 과제) | 가장 적음 |
| 점검 결과 | 통과 · 평가 제외 | 위와 같음(통과 Success, 평가 제외 Neutral) | 없음 — 평가된 항목 수가 차마다 달라 많다고 낫다고 할 수 없다 |
| 핵심 사실 | 보고서 `facts` 4칸(id: accidents·owners·usage·floodLoss) | `value`, `tone`이 neutral이 아니면 해당 톤 text 색 | 없음(문장 값) |
| 확인할 것 | 보고서 `issues` 앞 3개 | 줄마다 "{label} {title}"(라벨은 톤 text 색 SemiBold, 예: "주의 소유자 변경"), 4개 이상이면 "외 n개", 없으면 "없어요" | 없음 |

- **나은 값 규칙**: 값이 있는 차가 2대 이상이고 최고값이 유일할 때만 그 칸 하나를 `accentWeak` 배경(tile 모양) + `accentText` SemiBold로 칠한다. 동점·값 없음은 표시하지 않는다. "가장 나쁜 값" 표시는 하지 않는다.
- **대체 열(평가 실패)**: 판정·점수·기본 정보는 저장 컬럼, 점검 결과는 4.3절 규칙(위험·비공개를 나눌 수 없으면 "위험·비공개" 한 행으로 합쳐 보여주고 이 열은 위험·비공개 행에서 나은 값 비교에서 빠진다), 평가 제외·핵심 사실·확인할 것은 "—". 이름 아래 `caption1` `textTertiary` "저장 당시 요약이에요".
- **점검 결과 행 노출**: 위험·비공개·주의·통과·미확인은 항상 보인다(0이 비교 정보다). 평가 제외는 모든 차가 0이면 숨긴다.
- 화면 맨 아래 안내(`caption1` `textTertiary`): "파란색은 비교한 차 중 점수가 가장 높거나, 연식이 가장 최근이거나, 주행거리·문제 항목이 가장 적은 값이에요. 가격은 차 상태에 따라 달라 표시하지 않아요."
- 행·값 계산은 순수 함수 `buildCompareTable(columns): CompareTable`(Compose·Android 의존 없음)로 두고 단위 테스트한다. 현재 `computeWinners`/`uniqueWinner`의 유일 1등 규칙을 옮겨 쓴다.

### 6.4 상태별 화면

| 상태 | 화면 |
|---|---|
| `Loading` | 상단바 + 가운데 `body2` `textTertiary` "차 정보를 불러오고 있어요". 300ms 안에 끝나면 보이지 않게 300ms 지연 후 표시 |
| `Ready` | 6.2절 |
| `TooFew` | `AvEmptyState(icon = Icons.AutoMirrored.Rounded.CompareArrows, title = "비교할 차가 2대보다 적어요", description = "저장한 차에서 다시 골라 주세요", actionLabel = "돌아가기")` → `host.back` |

- 현재의 "01" 자리표시 로딩 화면, "TOP" 배지, "/100", "총 n건 점검" 분포 막대(`DiagnosisStackBar`), 점수 그라디언트(`COLOR_STOPS`), 영문 판정 문자열 노출(`VerdictCell`이 `verdict`를 그대로 출력하고 색을 점수로 정함)은 모두 없앤다.

## 7. 문구

해요체, 이모지 없음. 크롬 문구는 모두 `res/values/strings.xml`. 개수 라벨은 결과 화면 문자열을 재사용한다.

| 키 | 문구 |
|---|---|
| `result_count_danger` · `_hidden` · `_warning` · `_pass` · `_unknown` · `_excluded` | (기존) 위험 %1$d · 비공개 %1$d · 주의 %1$d · 통과 %1$d · 미확인 %1$d · 평가 제외 %1$d |
| `saved_count_danger_or_hidden` | 위험·비공개 %1$d |
| `saved_title` | 저장한 차 |
| `saved_action_compare` | 비교 |
| `saved_total` | %1$d대 |
| `saved_sort_title` | 정렬 |
| `saved_sort_recent` · `saved_sort_score` · `saved_sort_price` | 최근 저장순 · 점수 높은 순 · 가격 낮은 순 |
| `saved_row_saved_on` | %1$s 저장 |
| `saved_no_name` | 차량 정보 없음 |
| `saved_score` | %1$d점 |
| `saved_select_title` | 비교할 차 선택 |
| `saved_select_cancel` | 취소 |
| `saved_select_count` | %1$d대 선택 |
| `saved_select_cta` | %1$d대 비교하기 |
| `saved_select_cta_min` | 2대 이상 골라 주세요 |
| `saved_select_max` | 최대 3대까지 비교할 수 있어요 |
| `saved_delete` | 삭제 |
| `saved_toast_deleted` · `saved_toast_undo` | 삭제했어요 · 되돌리기 |
| `saved_toast_delete_failed` · `saved_toast_restore_failed` | 삭제하지 못했어요 · 되돌리지 못했어요 |
| `saved_empty_title` · `saved_empty_desc` · `saved_empty_action` | 저장한 차가 없어요 · 분석 결과에서 저장하면 여기서 모아 보고 비교할 수 있어요 · 차 분석하러 가기 |
| `compare_title` · `compare_subtitle` | 차량 비교 · %1$d대 |
| `compare_open_result` | 결과 보기 |
| `compare_section_basic` · `_checks` · `_facts` · `_issues` | 기본 정보 · 점검 결과 · 핵심 사실 · 확인할 것 |
| `compare_row_verdict` · `_score` · `_price` · `_year` · `_mileage` · `_fuel` | 판정 · 점수 · 가격 · 연식 · 주행거리 · 연료 |
| `compare_issues_more` · `compare_issues_none` | 외 %1$d개 · 없어요 |
| `compare_fallback_note` | 저장 당시 요약이에요 |
| `compare_missing` / `compare_missing_a11y` | — / 정보 없음 |
| `compare_best_high` · `_recent` · `_low` | 가장 높아요 · 가장 최근이에요 · 가장 적어요 (접근성 상태 문구) |
| `compare_legend` | 파란색은 비교한 차 중 점수가 가장 높거나, 연식이 가장 최근이거나, 주행거리·문제 항목이 가장 적은 값이에요. 가격은 차 상태에 따라 달라 표시하지 않아요. |
| `compare_loading` | 차 정보를 불러오고 있어요 |
| `compare_too_few_title` · `_desc` · `_action` | 비교할 차가 2대보다 적어요 · 저장한 차에서 다시 골라 주세요 · 돌아가기 |

- 날짜 형식: 올해 "10월 18일", 그 외 "2025년 10월 18일"(`Locale.KOREA`). 숫자는 0번 9절("9.8만km", "1,290만원").
- `verdict_*`(피하세요·주의·괜찮아요·확인 필요)는 기존 문자열을 쓴다.
- 0번 7.3절의 비교 화면 제목 "비교 분석"/"2대 매물을 한눈에"는 "차량 비교"/"{n}대"로 바꾼다(2~3대 지원, "분석"은 결과 화면과 헷갈림).

## 8. 접근성

- 목록 행: `semantics(mergeDescendants = true)`, 낭독 순서 "이름, 판정 {라벨} {점수}점, {개수 배지들}, {날짜} 저장". 클릭 라벨 "결과 보기". `customActions = [삭제]`(선택 모드에서는 없음).
- 선택 모드 행: `Role.Checkbox` + `toggleableState`. CTA 비활성 문구가 이유를 말한다("2대 이상 골라 주세요").
- 정렬 버튼: 낭독 "정렬, 최근 저장순", 시트 행은 `Role.RadioButton` + `selected`.
- 비교: 섹션 제목 `heading()`. 값 칸마다 낭독 "{차 이름} {행 라벨} {값}"(이름 없이 값만 읽히면 열을 구분할 수 없다), 나은 값 칸은 `stateDescription` "가장 높아요/가장 최근이에요/가장 적어요". "—"는 "정보 없음"으로 읽는다. 색만으로 뜻을 전하지 않는다.
- 터치 영역 48dp 이상(정렬 버튼, 상단바 텍스트 버튼, 선택 원 포함 행 전체, 열 머리).
- 큰 글꼴(200%): 목록 행은 높이를 고정하지 않고 줄바꿈 허용(이름·요약은 1줄 말줄임 유지), 비교 값은 2줄까지. 3열에서 넘치는 값은 말줄임되고 낭독은 전체 값을 읽는다.
- 포커스 링: 4.1절 여백으로 목록 행 링이 잘리지 않는다. 비교 열 머리는 열 간격 12dp 안에 링(5dp)이 들어가고, 바깥 열은 화면 여백 20dp 안에 들어간다.

## 9. 상태·예외

| 경우 | 처리 |
|---|---|
| 1번 이전 저장 행 | 컬럼 형식·집계 매핑이 같아 그대로 표시. 열면 결과 화면이 다시 평가(5절). 비교에서 평가 실패하면 대체 열 |
| 같은 차 쌍둥이 행 | 3.4절 묶음. 탭 개수·목록·비교 모두 한 대 |
| `title` 비었음 | "차량 정보 없음" |
| 연식·주행·가격·연료 null | 요약에서 그 칸만 뺌, 비교 값 "—", 나은 값 비교에서 제외. 가격 낮은 순 정렬에서 맨 뒤 |
| `verdict`가 알 수 없는 값 | 확인 필요(Neutral) |
| `score` 범위 밖 | 0~100으로 잘라 표시(저장 시 이미 `coerceIn`) |
| `rawJson` 손상(파싱 불가) | 식별자는 `carId`로 대체, 목록 표시는 컬럼이라 영향 없음. 비교는 대체 열 |
| 비교 중 행 삭제(결과 화면에서 저장 취소 후 복귀) | `observe` Flow로 열이 빠진다. 2대 미만이면 `TooFew` |
| 비교 중 백그라운드 갱신으로 행 변경 | `updatedAt`이 바뀐 행만 다시 평가해 교체 |
| 엔진 초기화 실패 | 모든 열이 대체 열. 화면은 그대로 동작 |
| 삭제 후 되돌리기 전에 다른 삭제 | 새 토스트가 이전 토스트를 대체한다(`AvToastState` 기존 동작). 이전 삭제는 확정 |
| 프로세스 종료·회전 | 목록: 정렬은 prefs, 선택은 `rememberSaveable`. 비교: `Route.Compare(carIds)`가 백스택에 남고 다시 평가 |

## 10. 범위

**포함**: 3~9절 전부. `SavedListScreen`·`CompareScreen` 재작성, 저장 목록·비교 문구의 `strings.xml` 이전, `SavedCarsRepository`·DAO 메서드 추가, 탭바 개수를 항목 수로, `AvToastState.show(long)` 옵션, `AppPreferences` 정렬 키, `DESIGN.md`에 "밀어서 삭제 + 되돌리기" 패턴 한 줄.

**제외**
- DB 스키마 변경, 중복 행의 DB 병합·정리(저장 경로 작업 또는 이후 과제)
- 판정·점수 로직, vendored core, TS 어댑터 변경
- 저장 경로(정규 키로 저장) 자체 — 다른 작업. 3번은 3.3절 계약만 가정
- 목록에서 엔진 재평가, `car.name`(등급 포함 이름)을 목록에 표시(저장 컬럼에 없음. 필요하면 이후 스키마 변경 묶음에서)
- 4대 이상 비교, 비교 결과 공유, 메모·태그·폴더, 검색
- 홈 "최근 본 차"(2번), 설정·앱 아이콘(4번)

## 11. 검증 (필요한 만큼만)

| 대상 | 방법 |
|---|---|
| 식별자·묶음 | JUnit `SavedIdentityTest`: 계약 픽스처 4건의 원본 raw(`webview-bundle/tests/fixtures`를 `app/src/test/resources/raw/`로 복사)에서 `vehicleId == carId`, 더미 광고 합성 raw(다른 `carId`, 같은 `vehicleId`) 두 행이 한 항목, 대표 행 선택(updatedAt·동점 규칙), `vehicleId` 없음·0·문자열·손상 JSON 대체 |
| 목록 표시 규칙 | JUnit `SavedListModelTest`: 4.3절 개수 배지 표 각 행, 이름에서 연식 떼기·빈 이름, 요약 형식(주행 3가지 경계, 가격 만원), 정렬 3종(가격 null 맨 뒤, 동점), 날짜(올해/작년) |
| 비교 표 | JUnit `CompareTableTest`: 유일 1등만 표시(동점·값 1개·null 제외), 가격·통과·평가 제외는 표시 없음, 대체 열(위험·비공개 합침, "—"), 확인할 것 3개 + "외 n개"·"없어요". 보고서는 `app/src/test/resources/report/*.json` 계약 픽스처를 파싱해 쓴다 |
| 화면 | Robolectric Compose 테스트 1개(`SavedListScreenTest`): 선택 모드에서 2대 미만 CTA 비활성·4번째 선택 거부, 삭제 사용자 동작 → 행 사라짐 → "되돌리기" → 복원. 가짜 저장소 주입 |
| 기존 | 전체 Kotlin 단위 테스트, vitest(변경 없음 확인용) 통과 |
| 수동 QA (1회) | 전용 AVD `AV_API_36`(`-no-snapshot`, 사용자 에뮬레이터 사용 금지): 차 3대 저장 → 목록 라벨이 결과 화면 개수 칩과 같은지, 정렬 3종, 밀어서 삭제·되돌리기, 3대 비교·결과 보기 왕복, 결과에서 저장 취소 후 비교 복귀(`TooFew` 또는 열 감소), 큰 글꼴 200%에서 3열. 쌍둥이 행은 더미 광고 URL을 찾을 수 있을 때만 확인(못 찾으면 단위 테스트로 갈음하고 기록) |

- 스크린샷 비교 테스트·성능 측정은 하지 않는다. 시각 확인은 수동 QA 한 번으로 한다.

## 12. 리스크

| 리스크 | 대응 |
|---|---|
| 저장 경로 작업이 `rawJson` 형식을 바꿈 | 3.3절에 의존 필드를 하나(`preloadedState.cars.base.vehicleId`)로 못박고, 없으면 `carId`로 대체해 최악에도 지금과 같은 동작(중복 표시) |
| 목록에서 모든 행의 `rawJson`을 파싱 | 이미 `getAllFlow`가 `rawJson`을 읽고 있다. 파싱은 `Dispatchers.Default`에서 `(carId, updatedAt)`마다 한 번. 수십 대 × 수십 KB 수준 |
| "위험·비공개 n" 라벨이 결과 화면과 다름 | NEVER + 2개 이상일 때만 나오고, 사실과 다른 라벨보다 낫다. 차를 열면 정확한 분리를 본다. 이후 스키마 변경 기회에 `hiddenCount` 컬럼 추가를 검토 |
| 밀어서 삭제를 모름 | 되돌리기로 실수 비용을 낮추고, 접근성 동작으로 대체 경로 제공. QA에서 발견성 문제가 크면 선택 모드에 삭제를 추가하는 안을 다시 결정 |
| 3열에서 값이 잘림 | 값 2줄 + 말줄임, 낭독은 전체. 큰 글꼴 QA |
| 화면 `BackHandler`와 AppRoot `BackHandler` 우선순위 | 화면 것이 나중에 등록되어 우선한다. 구현 시 선택 모드에서 뒤로가기 → 모드만 종료되는지 확인 |

## 13. 작업 분해

파일 소유가 겹치지 않게 나눴다. `strings.xml`은 T2가 한 번에 모두 추가해 충돌을 없앤다.

| # | 작업 | 파일 | 선행 |
|---|---|---|---|
| T1 | 식별자·저장소·DAO | `saved/SavedIdentity.kt`(신규), `saved/SavedCarsRepository.kt`(신규), `db/SavedCarDao.kt`(메서드 추가), `AutoVerdictApp.kt`(lazy 저장소), `MainActivity.kt`(탭 개수 = `itemCount`), `test/.../saved/SavedIdentityTest.kt`, `test/resources/raw/*`(픽스처 복사) | 없음 |
| T2 | 목록 표시 모델·문구 | `saved/SavedListModel.kt`(신규: `SavedCarItem` 표시값, 개수 배지, 이름·요약·날짜 형식, 정렬), `res/values/strings.xml`(7절 전부), `preferences/AppPreferences.kt`(`saved_sort`), `ui/components/AvToast.kt`(`long` 옵션), `test/.../saved/SavedListModelTest.kt` | 없음 |
| T3 | 저장 목록 화면 | `ui/screen/SavedListScreen.kt`(재작성), `ui/saved/SavedCarRow.kt`(신규: 행·밀어서 삭제), `ui/saved/SavedSortSheet.kt`(신규), `ui/navigation/AppScreenContent.kt`(Saved 분기: `onGoHome`, 저장소 주입), `test/.../ui/saved/SavedListScreenTest.kt` | T1, T2 |
| T4 | 비교 표 모델 | `compare/CompareTable.kt`(신규: `buildCompareTable`, 나은 값 규칙, 대체 열), `test/.../compare/CompareTableTest.kt` | T2(형식 함수) |
| T5 | 비교 화면 | `compare/CompareController.kt`(신규), `ui/screen/CompareScreen.kt`(재작성), `ui/navigation/AppScreenContent.kt`(Compare 분기: `onOpenResult`, 저장소·엔진 주입) | T1, T4 |
| T6 | 문서·QA | `DESIGN.md`(밀어서 삭제 + 되돌리기 한 줄), 이 문서 끝에 "구현 기록" 절(QA 결과) 추가, 0번 문서 14절 후속 과제 3번 행에 "3번에서 해소" 표시 | T3, T5 |

**병렬 묶음**
- 1차: **T1 ∥ T2** (서로 다른 파일)
- 2차: **T3 ∥ T4** (T4는 T2만 필요, 먼저 끝나면 T5 바로 시작)
- 3차: **T5** (T3과 `AppScreenContent.kt`의 다른 분기를 수정하므로, T3과 동시에 하면 병합 시 해당 파일만 손으로 합친다)
- 4차: **T6** (빌드 + 전체 테스트 + 에뮬레이터 1회)

각 작업 끝에 `./gradlew :app:testDebugUnitTest`가 통과해야 한다. `proguard-rules.pro`는 바뀌지 않는다(새 `@JavascriptInterface`·엔티티 없음).

## 14. 구현 기록 (2026-10-18)

**만든 것**: `saved/`(`SavedIdentity`·`SavedCarsRepository`·`SavedListModel`), `ui/saved/`(`SavedCarRow`·`SavedSortSheet`), 다시 쓴 `SavedListScreen`, `compare/`(`CompareTable`·`CompareController`), 다시 쓴 `CompareScreen`. DAO는 메서드만 추가했고 스키마는 그대로다. `AvToastState.show(long)`과 `AppPreferences`의 정렬 키를 추가했다.

**결정·변경**
- **묶음 규칙**: 식별자는 `rawJson`의 `preloadedState.cars.base.vehicleId`(숫자 또는 숫자 문자열, 0보다 큼), 없거나 읽을 수 없으면 `carId`. 대표 행은 `updatedAt`이 가장 큰 행 → `carId == 식별자`인 행 → `carId` 사전순. 삭제·되돌리기는 묶음 전체에 적용한다. 탭바 개수와 설정의 "저장한 차 {m}대"도 항목 수를 쓴다(6563ddf).
- **저장 경로(3.3절 계약)**: 새 저장은 `SavedCarKey.of(rawJson, urlId)` = 원래 vehicle id로 쓴다. `savedIdentity`가 같은 함수를 쓰므로 저장과 묶음 규칙이 하나다. 더미 광고 id로 저장된 옛 행은 옮기거나 지우지 않고 그 id로 계속 갱신한다. 더미 광고 URL은 자기 id로 먼저 찾고 원래 id로 한 번 더 찾는다. 캐시 키는 URL id 그대로다(e1ec6d3, 4b0406e).
- **위험·비공개 라벨**: 판정이 NEVER가 아니면 `dangerCount`는 모두 "비공개 n", NEVER이고 1개면 "위험 1", NEVER이고 2개 이상이면 "위험·비공개 n"(4.3절 그대로, `savedCountBadges` 테스트).
- **비교 재평가**: `CompareController`가 고른 행을 구독하고 각 `rawJson`을 엔진으로 다시 평가한다(`carId`+`updatedAt`이 같으면 다시 하지 않음). 실패한 차만 저장 컬럼으로 대체 열을 만든다. 2대 미만이 되면 `TooFew`.
- 되돌리기는 `SavedCarsRepository.restoreDetached`가 앱 범위에서 실행해 목록 화면을 떠난 뒤에도 동작한다.

**후속 과제**
- "위험·비공개 n"을 없애려면 저장 컬럼에 `hiddenCount`가 필요하다. 다음 스키마 변경(실제 마이그레이션 포함) 때 검토한다.
- 더미 광고 쌍둥이 행은 단위 테스트로만 확인했다. DB 쪽 중복 행 정리는 하지 않았다.
- 밀어서 삭제의 발견성은 실제 사용에서 문제가 되면 선택 모드에 삭제를 추가하는 안을 다시 검토한다(12절).
