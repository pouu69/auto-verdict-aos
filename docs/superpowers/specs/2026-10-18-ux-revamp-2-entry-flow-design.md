# UX/UI 개편 2번 — 진입 흐름(홈 + 온보딩) 설계

- 작성일: 2026-10-18
- 브랜치: `worktree-agent-a2868a7720945add9` (`main` f73fdbd 기준)
- 선행: 0번 디자인 언어 & 정보 구조, 1번 결과 화면
- 상태: 설계 확정(사용자 부재 중 자율 결정, 결정마다 근거 기록), 구현 계획 작성 전

## 1. 목표

앱을 처음 열었을 때와 다시 열었을 때의 첫 화면을 토스 스타일로 다시 만든다. 0번에서 껍데기(상단바, 색 치환)만 바꾸고 남겨 둔 홈과 온보딩이 대상이다.

- 홈 하나에서 세 가지가 끝나야 한다. 링크로 분석 시작, 엔카 공유 방법 확인, 최근 본 차 다시 열기.
- 최근 본 차는 1번 이후 캐시에 저장되는 차 이름·판정·점수를 보여준다. 지금은 "매물 #123"과 시각만 나온다.
- 온보딩은 짧게 줄인다. 무엇을 해 주는 앱인지, 엔카 앱이나 브라우저에서 어떻게 공유하는지만 알려준다.
- 홈과 온보딩에 남은 M3 `Button`·`Card`·`OutlinedTextField`·`ModalBottomSheet`, `FontWeight.Medium`, 영문 라벨, 하드코딩 문구를 정리한다(0번 14절 후속 과제).

## 2. 결정 사항

| 항목 | 결정 | 근거 |
|---|---|---|
| 클립보드 자동 읽기 | **없앤다.** 지금은 홈이 다시 보일 때마다 클립보드를 읽어 입력창을 채우는데, 이 동작을 지우고 입력창 안 **[붙여넣기]** 버튼으로 바꾼다. 사용자가 누를 때만 읽는다 | ① Android 12+는 앱이 다른 앱의 클립보드를 읽을 때마다 시스템 토스트("…에서 클립보드에 액세스함")를 띄운다. 자동 읽기에서는 홈으로 돌아올 때마다 이 토스트와 앱 토스트가 같이 뜬다. ② Android 10+는 창에 포커스가 있는 앱만 클립보드를 읽을 수 있다. `onResume` 시점에는 포커스가 아직 없을 수 있어서 자동 채움이 들쭉날쭉하다. ③ 사용자 입장에서 앱이 클립보드를 몰래 읽는 것처럼 느껴진다 |
| `lastAnalyzedUrl` | `ScreenHost`와 `AppRoot`에서 **제거** | 자동 채움이 같은 링크를 다시 채우지 않게 막던 상태다. 자동 채움이 없어지면 쓰는 곳이 없다 |
| 분석 버튼 위치 | 입력창 바로 아래 **인라인** `AvButton` Primary L. 하단 고정 `AvBottomCta`는 쓰지 않음 | 홈은 탭 루트라 하단에 이미 배너(50dp)와 탭바가 있다. CTA까지 고정하면 작은 화면에서 하단 크롬이 130dp를 넘는다. 입력창과 버튼이 한 묶음으로 보이는 편이 낫다 |
| 입력 검증 시점 | 입력 중에는 에러를 띄우지 않는다. 버튼은 공백이 아니면 켜지고, **누를 때 검증**한다. 붙여넣기는 그 자리에서 검증 | 지금은 한 글자만 쳐도 빨간 에러가 뜬다. 대부분 붙여넣기로 들어오므로 손으로 칠 때만 생기는 이 소음은 없앤다 |
| 링크 정규화 | 분석과 붙여넣기 모두 `EncarUrl.extractCarId` → `buildDetailUrl`로 정규화한 URL만 쓴다 | 지금은 "[엔카] 차명 https://…" 같은 원문이 그대로 `Route.Result.url`에 들어간다. 공유 인텐트(0번 7.2절)와 같은 규칙으로 맞춘다 |
| 홈 상단바 | `AvTopBar(onBack = null, title = null, actions = 설정)`. 큰 질문 문장(`title1`, heading)이 화면 제목 역할을 한다 | 화면당 메시지 하나. 상단바 앱 이름(`title2`)과 질문 문장이 위아래로 붙으면 큰 글씨가 두 줄 연달아 나와 위계가 흐려진다. 브랜드는 스플래시와 아이콘이 보여준다 |
| 공유 안내 | 홈의 행 하나(`AvListRow`)를 누르면 `AvBottomSheet`가 열린다. 시트 안에 "엔카 앱에서"·"브라우저에서" 단계와 [엔카 열기] 버튼 | 지금 구조(카드 → 시트)를 유지하고 컴포넌트만 바꾼다. 엔카 앱 경로는 지금 안내에 아예 없어서 새로 넣는다 |
| 최근 본 차 데이터 | `cache`에서 **필요한 열만 고르는 새 DAO 쿼리**(`rawInputJson` 제외, `LIMIT 20`)를 쓴다. 엔티티는 그대로 둔다 | 지금 쿼리는 `SELECT *`라 행마다 수백 KB짜리 원본 JSON까지 메모리에 올린다. 쿼리와 POJO만 추가하면 스키마(해시)는 바뀌지 않는다 |
| 만료 행 숨김 | 화면이 보이는 동안 **1분마다** 현재 시각으로 다시 조회한다(`flatMapLatest`) | 지금은 `now`를 흐름을 만들 때 한 번만 넣어서, 홈을 띄워 둔 채 24시간이 지나도 만료된 행이 남는다. 같은 주기로 "n분 전"도 갱신된다 |
| 최근 본 차 개수 | 최대 20대 조회. 처음에는 5대만 보이고 "{n}대 더 보기"를 누르면 나머지가 펼쳐진다 | 결과 화면 이력 섹션(1번 5.2절 8)과 같은 패턴이다. 24시간 보관이라 20대를 넘기는 일은 드물다 |
| 1번 이전 캐시 행 | `verdict`가 비었으면 이전 형식으로 본다. 판정 배지와 점수를 숨기고, 제목이 비었으면 "엔카 매물 {carId}" | 1번 이전 코드는 `title=""`, `score=0`, `verdict=""`로 기록했다. "0점"이나 "확인 필요"를 잘못 보여주면 안 된다. 이런 행은 업데이트 후 24시간 안에 모두 만료된다 |
| 온보딩 분량 | **2쪽**: ① 무엇을 확인해 주는지 ② 공유하는 법. 3쪽 중 "점수로 보는 등급"은 뺀다 | 첫 화면에서 할 일(공유)까지 가는 거리를 줄인다. 점수와 판정은 결과 화면이 직접 보여준다 |
| 온보딩 모드 | 첫 실행과 "사용법 다시 보기"(설정)를 구분한다. 첫 실행은 [건너뛰기]가 있고 뒤로 버튼이 없다. 다시 보기는 뒤로 버튼이 있고 [건너뛰기]가 없으며 마지막 버튼이 "닫기"다 | 지금은 두 경우가 같아서 설정에서 들어와도 "시작하기"가 나온다 |
| 첫 실행 중 공유 | 첫 실행 온보딩에 보류 중인 공유가 있으면 하단에 "공유한 매물은 시작하면 바로 분석해요"를 보여준다 | 매물을 공유하고 들어왔는데 공유 방법 설명이 나오면 왜 결과가 안 나오는지 알 수 없다. 내비게이션 규칙(0번 7.2절, 완료 시 결과로 이동)은 바꾸지 않는다 |
| 포커스 링 잘림 | `avPressable`에 **안쪽 링** 옵션을 추가하고, 화면 폭을 꽉 채우는 `AvListRow`와 접는 `AvSectionHeader`에 적용한다 | 0번 14절 후속 과제. 홈은 폭 전체를 쓰는 행이 스크롤 컨테이너 안에 있어서 바깥 링이 화면 가장자리에서 잘린다 |
| pop 중 같은 route 재진입 | **그대로 둔다(수용).** 결과에서 뒤로 온 직후 250ms 안에 같은 최근 행을 누르면 나가던 결과 화면이 그대로 돌아온다 | 0번 14절 후속 과제. `Route`가 같으면 같은 차, 같은 데이터라 잘못된 내용이 보이지 않는다. 고치려면 스택 항목마다 고유 id를 붙이고 `NavStateSaver`를 바꿔야 해서 얻는 것에 비해 비용이 크다 |
| 광고 | **현재 위치 유지.** 홈은 탭바 위 배너(`AppRoot`), 온보딩·공유 안내 시트에는 광고 없음 | 0번 10절 |
| DB·판정·core | 스키마 변경 없음, 판정 로직 변경 없음, `webview-bundle/src/core` 수정 없음 | 하드 제약(파괴적 마이그레이션으로 저장한 차가 지워짐) |

## 3. 홈

### 3.1 구성 (위 → 아래, 흰 바탕)

```
AvTopBar(onBack = null, actions = [설정 아이콘])
┌ 질문 문장 (title1, heading)          "어떤 매물을 확인할까요?"
│ 설명 (body2, textSecondary)          "엔카 매물 링크를 넣으면 12가지 기준으로 평가해요"
│ AvTextField                          [(링크 아이콘) 엔카 매물 링크      (붙여넣기)]
│                                      에러 문구 (있을 때)
│ AvButton Primary L, 폭 전체           [분석하기]
│ AvListRow (공유 안내)                 (아이콘) 엔카에서 바로 공유하기 / 링크를 복사하지 않아도 돼요  >
├ AvSectionDivider
│ AvSectionHeader("최근 본 차", count)
│ AvListRow × 최대 5 (펼치면 최대 20)    2015 BMW 5시리즈 / 82점 · 3시간 전        [괜찮아요]
│ AvButton Secondary M "{n}대 더 보기" / "접기"
│ 각주 (caption1, textTertiary)         "분석하고 24시간이 지나면 목록에서 사라져요"
└ (빈 상태면 AvEmptyState)
[배너 320x50]  ← AppRoot
[AvTabBar]     ← AppRoot
```

- 스크롤은 `LazyColumn` 하나다. 좌우 여백은 20dp(행 컴포넌트는 자체 여백). 질문 문장 위 8dp, 설명 아래 24dp, 입력창과 버튼 사이 12dp, 버튼 아래 16dp.
- 질문 문장은 줄바꿈 문자를 넣지 않고 자연스럽게 줄바꿈되게 한다(큰 글꼴 대응).
- 진입할 때 입력창에 자동 포커스를 주지 않는다. 탭을 오갈 때마다 키보드가 뜨는 것을 막기 위해서다.

### 3.2 링크 입력

| 동작 | 결과 |
|---|---|
| 입력창이 비었을 때 | 오른쪽에 [붙여넣기](`AvTextField`의 새 `actionLabel`/`onAction`, Weak S). 입력이 있으면 그 자리에 지우기 아이콘 |
| [붙여넣기], 클립보드에 엔카 매물 링크 있음 | 정규화한 URL로 입력창을 채우고 에러를 지운다. 바로 분석하지는 않는다(오래된 링크가 남아 있을 수 있음) |
| [붙여넣기], 클립보드 비었음 | 입력창은 그대로 두고 에러 "복사한 내용이 없어요. 엔카에서 매물 링크를 복사해 주세요" |
| [붙여넣기], 엔카 매물 링크 아님 | 입력창은 그대로 두고(임의 텍스트를 붓지 않음) 에러 "복사한 내용에 엔카 매물 링크가 없어요" |
| 입력 변경 | 에러를 지운다 |
| [분석하기] 또는 키보드 Go, 유효 | 정규화한 URL로 `Route.Result(url, fromSaved = false)`를 push한다. 입력창과 에러를 비우고 포커스를 해제한다(키보드 닫힘) |
| [분석하기], 무효 | 에러 "엔카 매물 링크가 아니에요. 매물 페이지 주소를 넣어 주세요". 입력은 그대로 둔다 |
| 공백만 입력 | 버튼 비활성(`AvButton` 비활성 = `surfaceMuted` + `textTertiary`, 0번 4.2절 예외) |

- 키보드: `KeyboardType.Uri`, `ImeAction.Go`.
- 판정 로직은 순수 함수로 뺀다: `HomeInput.submit(text): Submit { Go(url) | Invalid | Blank }`, `HomeInput.paste(clipText: String?): Paste { Filled(url) | Empty | NotEncar }`. 둘 다 `EncarUrl`만 쓴다(`EncarUrl` 정규식은 바꾸지 않음).
- `ClipboardUtil`은 텍스트 읽기만 맡는다: `readText(context): String?`(`primaryClip?.getItemAt(0)?.coerceToText(context)`). 기존 `getEncarUrl`은 쓰는 곳이 없어지므로 지운다.
- 입력값과 에러는 `rememberSaveable`로 둔다(회전, 탭 전환 후 복귀).

**클립보드 플랫폼 동작 (권한 아님)**

클립보드 읽기에는 런타임 권한이 없다. 대신 버전별로 아래 동작이 있다.

| 버전 | 동작 | 대응 |
|---|---|---|
| Android 10+ (API 29) | 포커스가 있는 앱만 읽을 수 있다. 아니면 `null` | 사용자가 버튼을 누를 때만 읽으므로 항상 포커스가 있다 |
| Android 12+ (API 31) | 다른 앱이 넣은 클립을 읽으면 시스템 토스트가 뜬다 | 사용자가 [붙여넣기]를 눌렀을 때만 뜨므로 예상 가능한 동작이다. 앱 자체 토스트는 띄우지 않는다(중복 방지) |
| Android 13+ (API 33) | 복사할 때 시스템 클립보드 미리보기가 뜨고, 일정 시간이 지나면 클립보드가 자동으로 비워진다 | 비워진 경우는 "복사한 내용이 없어요"로 안내한다 |

### 3.3 공유 안내 행과 시트

- 행: `AvListRow(title = "엔카에서 바로 공유하기", subtitle = "링크를 복사하지 않아도 돼요", leading = AvIllustrationIcon(Icons.Rounded.IosShare, Accent, 40dp), trailing = Icons.Rounded.ChevronRight(textTertiary), onClick = 시트 열기)`. 시트를 열기 전에 포커스를 해제한다(키보드 닫힘).
- 시트: `AvBottomSheet(title = "엔카에서 공유하는 법")`. 내용이 길어서 `verticalScroll`을 쓰고 높이 상한은 기본값(상태바 아래까지)으로 둔다. 열림 상태는 `rememberSaveable`로 둔다.

```
엔카 앱에서                              (buttonM, textSecondary, heading)
 1  매물 화면에서 공유 버튼을 눌러요
 2  공유 목록에서 AutoVerdict를 골라요
브라우저에서
 1  매물 페이지에서 메뉴를 열고 공유를 눌러요
    [BrowserShareIllustration]
 2  공유 목록에서 AutoVerdict를 골라요
    [ShareSheetIllustration]
목록에 없으면 공유 목록 끝의 더보기를 눌러 보세요   (caption1, textTertiary)
[엔카 열기]  AvButton Primary L, 폭 전체
```

- 단계 번호는 화면 전용 컴포저블(24dp 원, `accentWeak` 배경, `accentText` `caption2`)로 그리고, 토큰만 쓴다. 단계 문구는 `body2` `textPrimary`.
- [엔카 열기]를 누르면 시트를 닫는 애니메이션이 끝난 뒤 `ACTION_VIEW(EncarUrl.HOME_URL)`을 연다. 엔카 앱이 설치돼 있고 앱 링크를 처리하면 앱이 열린다. 열 앱이 없으면(`ActivityNotFoundException`) `LocalAvToast` "엔카를 열지 못했어요".
- **일러스트 2종 정리** (`BrowserShareIllustration`, `ShareSheetIllustration`): `MaterialTheme.*`와 `copy(alpha=…)`를 토큰으로 바꾼다. 바탕은 `surfaceSubtle`, 모양은 `shapes.tile`, 흐린 항목은 `textTertiary`, 문구는 `caption1`/`caption2`. 문구는 모두 `strings.xml`로 옮기고 다음을 고친다. 반말 "← 이것 탭!"·"← 이것만 탭!"은 "여기를 눌러요"로, 화살표 글리프(←, ↓)는 삭제, "⋮"는 Pretendard에 없을 수 있어 "크롬은 오른쪽 위, 삼성 인터넷은 아래쪽 메뉴에 있어요"로. 일러스트 전체는 장식으로 처리한다(`clearAndSetSemantics {}`, 같은 내용을 단계 문구가 읽어 줌).

### 3.4 최근 본 차

**데이터**

```kotlin
// db/RecentViewRow.kt — Room POJO (엔티티 아님, 스키마 영향 없음)
data class RecentViewRow(
    val carId: String, val url: String, val title: String,
    val score: Int, val verdict: String, val cachedAt: Long,
)

// CacheDao — getRecentFlow를 대체
@Query("SELECT carId, url, title, score, verdict, cachedAt FROM cache " +
       "WHERE expiresAt > :now ORDER BY cachedAt DESC LIMIT :limit")
fun recentViewsFlow(now: Long, limit: Int): Flow<List<RecentViewRow>>
```

- `getRecentFlow`는 지운다(앱 코드에서는 홈만 썼다). `AppDatabaseTest`의 해당 테스트 2개는 새 쿼리로 옮긴다.
- `HomeRoute`는 RESUMED 동안 1분마다 `now`를 갱신하는 흐름을 `flatMapLatest { dao.recentViewsFlow(it, 20) }`로 이어서, 하나의 `collectAsStateWithLifecycle(initialValue = null)`로 모은다. `now`가 바뀔 때마다 새로 수집하면 초깃값 `null` 때문에 목록이 1분마다 깜빡이므로, 반드시 한 흐름으로 이어야 한다.
- `null`(첫 조회 전)이면 최근 섹션 자리에 아무것도 그리지 않는다. 빈 상태가 잠깐 깜빡이는 것을 막기 위해서다.

**표시 모델** (순수 함수, `home/RecentViews.kt`)

```kotlin
data class RecentViewItem(
    val carId: String, val url: String,
    val title: String?,        // null → "엔카 매물 {carId}"
    val verdict: String?,      // null → 배지 숨김 (1번 이전 행)
    val score: Int?,           // null → 점수 숨김
    val age: RelativeAge,
)
sealed interface RelativeAge { data object JustNow; data class Minutes(val n: Int); data class Hours(val n: Int) }
fun RecentViewRow.toItem(now: Long): RecentViewItem
```

| 규칙 | 값 |
|---|---|
| 이전 형식 판별 | `verdict.isBlank()` → `verdict = null`, `score = null` (점수 0은 실제 값일 수 있으니 판정으로만 판별) |
| 제목 | `title.isBlank()`면 null |
| 상대 시간 | 1분 미만 또는 `cachedAt > now`(시계 역행) → "방금 전" · 60분 미만 → "{n}분 전" · 그 외 → "{n}시간 전"(최대 23, 내림) |

**행**

- `AvListRow(title = 제목 또는 "엔카 매물 {carId}", subtitle = "82점 · 3시간 전"(이전 형식이면 "3시간 전"), trailing = AvBadge(verdictLabelRes, AvTone.fromVerdict), onClick, onClickLabel = "분석 결과 보기")`.
- 누르면 `host.navigate(Route.Result(row.url, fromSaved = false))`. 결과 화면은 캐시가 살아 있으면 캐시로, 그 사이 만료됐으면 새로 수집해서 보여준다(1번 5.1절 데이터 소스 순서, 변경 없음). 저장한 차면 DB 기준으로 저장 상태가 표시된다.
- 이전 형식 행을 열어도 캐시 행을 새 형식으로 다시 쓰지는 않는다(1번 캐시 기록 규칙 그대로). 24시간 안에 만료되어 자연히 사라진다.
- `key = carId`(PK라 유일).
- 5대 넘으면 "{n}대 더 보기"(n = 남은 수), 펼친 뒤에는 "접기". 펼침 상태는 `rememberSaveable`.

**빈 상태**: `AvEmptyState(icon = Icons.Rounded.History, title = "아직 본 차가 없어요", description = "분석한 매물은 24시간 동안 여기에 남아요")`. 버튼은 없다(바로 위에 입력창이 있음). 빈 상태에서는 각주를 숨긴다.

### 3.5 화면 구조와 상태 소유

- `home/HomeRoute.kt`: 플랫폼 연결 담당. DB 흐름, 클립보드 읽기, 엔카 열기, 토스트, 내비게이션.
- `home/HomeScreen.kt`: 상태 없는 UI. `HomeScreen(input, onInputChange, error: HomeError?, onPaste, onSubmit, recent: List<RecentViewItem>?, onOpenRecent, onOpenSettings, onOpenEncar)`. 시트 열림과 더보기 펼침 같은 화면 내부 UI 상태만 스스로 가진다.
- `home/HomeInput.kt`, `home/RecentViews.kt`: 순수 로직.
- `home/ShareGuideSheet.kt`: 시트 내용.
- `ui/screen/AnalyzeScreen.kt`는 지운다. 패키지는 1번(`result/`)과 같은 방식으로 `com.car.autoverdict.home`에 둔다.

## 4. 온보딩

### 4.1 구성

```
AvTopBar(onBack = 다시 보기면 host.back, 아니면 null,
         actions = 첫 실행이고 마지막 쪽이 아니면 [건너뛰기] (AvButton Secondary S, 48dp 터치 영역))
HorizontalPager (2쪽, 스와이프 가능)
  쪽 1  AvIllustrationIcon(Icons.Rounded.FactCheck, Accent, 96dp)
        "엔카 매물, 사기 전에 확인해 보세요"                        (title1, heading)
        "12가지 기준으로 살펴보고 결론부터 알려 드려요"              (body2, textSecondary)
        AvListRow × 3 (클릭 없음, leading 40dp AvIllustrationIcon)
          사고·침수·전손 이력            (Icons.Rounded.CarCrash, Danger)
          소유자 변경·자차보험 공백       (Icons.Rounded.People, Warning)
          성능점검 교환·수리 부위         (Icons.Rounded.Build, Neutral)
  쪽 2  "엔카에서 공유하면 바로 분석해요"                            (title1, heading)
        1  엔카 앱이나 브라우저에서 매물을 열어요
        2  공유를 누르고 AutoVerdict를 골라요
        [ShareSheetIllustration]
        "링크를 복사해 홈에 붙여 넣어도 돼요"                         (caption1, textTertiary)
페이지 점 (8dp, 현재 쪽은 너비 20dp, accent / 나머지 border)
(첫 실행 + 보류 공유) "공유한 매물은 시작하면 바로 분석해요"        (caption1, accentText)
AvBottomCta: AvButton Primary L 폭 전체
  쪽 1 "다음" → 다음 쪽 애니메이션
  쪽 2 첫 실행 "시작하기" / 다시 보기 "닫기" → host.finishOnboarding
```

- 영문 태그 칩(`AUTO ANALYSIS` 등), 그라데이션 히어로, 합니다체("평가합니다")는 없앤다.
- 쪽 내용은 세로 스크롤을 허용한다. 큰 글꼴이나 작은 화면에서 잘리지 않게 하기 위해서다.
- 인셋: `AvTopBar`가 상태바, `AvBottomCta`가 내비게이션 바를 맡는다. 지금 루트의 `windowInsetsPadding(systemBars)`는 지운다.
- 페이지 상태는 `rememberPagerState`(saveable)로 회전 후에도 유지된다.
- 아이콘 이름은 구현 시 `material-icons-extended`에 있는지 확인하고, 없으면 같은 의미의 `Icons.Rounded.*`로 바꾼다.

### 4.2 첫 실행 / 다시 보기 구분

`OnboardingScreen(mode: OnboardingMode, hasPendingShare: Boolean, onFinish: () -> Unit, onBack: (() -> Unit)?)`, `enum class OnboardingMode { FirstRun, Replay }`.

| | 첫 실행 (`[Onboarding]`) | 다시 보기 (설정에서 push) |
|---|---|---|
| 상단 뒤로 | 없음 (시스템 뒤로 = 백그라운드, 0번 7.2절) | 있음 (`host.back`) |
| [건너뛰기] | 쪽 1에서만 | 없음 |
| 마지막 버튼 | 시작하기 | 닫기 |
| 보류 공유 문구 | `pendingSharedUrl`이 있으면 표시 | 없음 |
| 완료 처리 | 완료 플래그 저장, `[Home]` 또는 `[Home, Result]` (기존) | pop (기존) |

## 5. 내비게이션 변경

새 Route는 없다. 셸에서 바뀌는 것은 아래뿐이다.

| 파일 | 변경 |
|---|---|
| `AppRoot.kt` | `ScreenHost.lastAnalyzedUrl`·`onAnalyzed`와 `lastAnalyzedUrl` saveable을 지운다. `ScreenHost`에 `isFirstRunOnboarding: Boolean`, `hasPendingShare: Boolean`(`navState.pendingSharedUrl != null`)을 추가한다 |
| `AppNavigator.kt` | private `isFirstRunOnboarding(state)`를 `internal`로 열어 `AppRoot`가 쓴다(규칙을 두 곳에 두지 않음) |
| `AppScreenContent.kt` | `Route.Home` → `HomeRoute(host)`, `Route.Onboarding` → `OnboardingScreen(mode = if (host.isFirstRunOnboarding) FirstRun else Replay, hasPendingShare, onFinish = host.finishOnboarding, onBack = host.back.takeIf { mode == Replay })` |

- 이벤트 표(0번 7.2절)는 바뀌지 않는다. `AppNavigatorTest`는 그대로 통과해야 한다.
- `CLAUDE.md`의 "app-level state (back stack, last analyzed URL) lives in AppRoot" 문장을 고친다.

## 6. 컴포넌트 변경 (`ui/components`)

새 컴포넌트는 만들지 않는다. 기존 컴포넌트에 기본값이 있는 파라미터만 추가하므로 호출부 호환이 깨지지 않는다.

| 컴포넌트 | 추가 | 비고 |
|---|---|---|
| `AvTextField` | `actionLabel: String? = null`, `onAction: (() -> Unit)? = null` | 값이 비었을 때만 오른쪽에 `AvButton` Weak S(`minimumInteractiveComponentSize`). 값이 있으면 지금처럼 지우기 아이콘 |
| `AvListRow` | `onClickLabel: String? = null` | `avPressable(onClickLabel)`로 전달. 포커스 링은 안쪽(아래) |
| `Modifier.avPressable` | `focusRingInside: Boolean = false` | true면 링을 요소 안쪽 2dp 들어간 위치에 그린다(간격 없이, 모양은 `focusShape`를 그만큼 줄임). 기본값은 지금 동작 |
| `AvListRow`, `AvSectionHeader`(접기 모드) | 내부에서 `focusRingInside = true` | 화면 폭을 꽉 채워 쓰이므로 바깥 링이 잘린다 |

- 갤러리(`ComponentGalleryScreen`)에 `AvTextField` 액션 상태와 `onClickLabel` 행을 추가한다.
- `DESIGN.md` 컴포넌트 절에 위 파라미터를 한 줄씩 추가한다.

## 7. 문구 (`strings.xml`)

모두 해요체, 이모지·화살표 글리프 없음. 앱 이름 "AutoVerdict"는 그대로 쓴다.

| 키 | 문구 |
|---|---|
| `home_headline` | 어떤 매물을 확인할까요? |
| `home_description` | 엔카 매물 링크를 넣으면 12가지 기준으로 평가해요 |
| `home_input_placeholder` | 엔카 매물 링크 |
| `home_paste` | 붙여넣기 |
| `home_analyze` | 분석하기 |
| `home_error_invalid` | 엔카 매물 링크가 아니에요. 매물 페이지 주소를 넣어 주세요 |
| `home_error_clipboard_empty` | 복사한 내용이 없어요. 엔카에서 매물 링크를 복사해 주세요 |
| `home_error_clipboard_not_encar` | 복사한 내용에 엔카 매물 링크가 없어요 |
| `home_share_row_title` / `_subtitle` | 엔카에서 바로 공유하기 / 링크를 복사하지 않아도 돼요 |
| `home_recent_title` | 최근 본 차 |
| `home_recent_untitled` | 엔카 매물 %1$s |
| `home_recent_meta` | %1$s · %2$s (점수 · 시간) |
| `home_recent_show_more` / `home_recent_fold` | %1$d대 더 보기 / 접기 |
| `home_recent_click_label` | 분석 결과 보기 |
| `home_recent_footnote` | 분석하고 24시간이 지나면 목록에서 사라져요 |
| `home_recent_empty_title` / `_description` | 아직 본 차가 없어요 / 분석한 매물은 24시간 동안 여기에 남아요 |
| `time_just_now` / `time_minutes_ago` / `time_hours_ago` | 방금 전 / %1$d분 전 / %1$d시간 전 |
| `guide_title` | 엔카에서 공유하는 법 |
| `guide_from_app` / `guide_from_browser` | 엔카 앱에서 / 브라우저에서 |
| `guide_app_step_share` | 매물 화면에서 공유 버튼을 눌러요 |
| `guide_browser_step_share` | 매물 페이지에서 메뉴를 열고 공유를 눌러요 |
| `guide_step_pick_app` | 공유 목록에서 AutoVerdict를 골라요 |
| `guide_tip_more` | 목록에 없으면 공유 목록 끝의 더보기를 눌러 보세요 |
| `guide_open_encar` | 엔카 열기 |
| `toast_open_encar_failed` | 엔카를 열지 못했어요 |
| `illus_share_sheet_caption` | 공유 목록에서 이렇게 보여요 |
| `illus_tap_here` | 여기를 눌러요 |
| `illus_menu_caption` | 메뉴를 열면 |
| `illus_menu_favorite` / `illus_menu_share` / `illus_menu_print` | 즐겨찾기 / 공유 / 인쇄 |
| `illus_app_messenger` / `illus_app_message` | 메신저 / 메시지 (기존 더미 앱 라벨) |
| `illus_browser_note` | 크롬은 오른쪽 위, 삼성 인터넷은 아래쪽 메뉴에 있어요 |
| `onboarding_skip` / `onboarding_next` / `onboarding_start` / `onboarding_close` | 건너뛰기 / 다음 / 시작하기 / 닫기 |
| `onboarding_value_title` | 엔카 매물, 사기 전에 확인해 보세요 |
| `onboarding_value_body` | 12가지 기준으로 살펴보고 결론부터 알려 드려요 |
| `onboarding_value_point_history` / `_owner` / `_inspection` | 사고·침수·전손 이력 / 소유자 변경·자차보험 공백 / 성능점검 교환·수리 부위 |
| `onboarding_share_title` | 엔카에서 공유하면 바로 분석해요 |
| `onboarding_share_step_open` / `_step_pick` | 엔카 앱이나 브라우저에서 매물을 열어요 / 공유를 누르고 AutoVerdict를 골라요 |
| `onboarding_share_tip` | 링크를 복사해 홈에 붙여 넣어도 돼요 |
| `onboarding_pending_share` | 공유한 매물은 시작하면 바로 분석해요 |
| `onboarding_page_cd` | %2$d쪽 중 %1$d쪽 |

- 점수는 기존 `result_score`("%1$d점")를 재사용한다. 판정 라벨은 `verdictLabelRes`를 쓴다.
- 0번 4.2절 대비 표에서 "홈 '분석 시작'"으로 적힌 비활성 버튼 예외는 이번에 라벨만 "분석하기"로 바뀐다(규칙 동일).
- 최종 문구 목록은 구현 계획에서 확정하고, 이 표가 기준이다. 구현 중 키 이름은 바뀔 수 있다.

## 8. 상태와 예외

| 상황 | 동작 |
|---|---|
| 엔카가 아닌 링크 입력 후 분석 | 인라인 에러(3.2절), 이동 없음 |
| 엔카 목록·검색 페이지 링크 | `/cars/detail/{id}`가 없으므로 위와 같음 |
| 공유 문구 전체를 붙여 넣음("[엔카] … https://…/cars/detail/123?…") | 차 ID만 뽑아 정규화(유효) |
| 클립보드 비었음 / 엔카 아님 / 앱 포커스 없음 | 3.2절 표. 포커스 없음은 사용자 탭에서만 읽으므로 생기지 않음 |
| 최근 본 차 20대 초과 | 최신 20대만 조회 |
| 홈을 띄워 둔 채 시간이 지남 | 1분마다 재조회해 만료 행이 빠지고 상대 시간이 갱신됨. 백그라운드에서는 멈춤 |
| 1번 이전 캐시 행 | 제목 대체, 배지·점수 숨김(3.4절) |
| 캐시 비우기(설정) 후 홈 복귀 | Room 흐름이 다시 내보내 빈 상태로 바뀜 |
| 같은 차 최근 행을 빠르게 두 번 탭 | 기존 규칙(같은 `carId` Result 중복 push 방지, 0번 7.2절) |
| 결과에서 뒤로 온 직후 같은 행 재진입(250ms 이내) | 나가던 화면 재사용, 수용(2절) |
| 첫 실행 온보딩 중 공유 | 보류 공유 문구 표시, 완료 시 결과로(기존 규칙) |
| 온보딩 중 무효 공유 | 토스트 "엔카 매물 링크만 확인할 수 있어요"(기존) |
| 회전·프로세스 복원 | 입력값·에러·시트 열림·더보기 펼침·페이지 위치가 유지된다. 최근 목록은 다시 조회한다 |
| 키보드 열림 | 홈 루트 `imePadding()`(0번 6절 인셋 표 유지). 입력창이 화면 위쪽에 있어 가리지 않는다 |
| 엔카 열기 실패 | 토스트 "엔카를 열지 못했어요" |

## 9. 접근성

- 질문 문장, 온보딩 쪽 제목, 시트의 "엔카 앱에서"·"브라우저에서"는 `heading()`.
- 최근 행은 하나로 합쳐 읽힌다: "2015 BMW 5시리즈, 82점 · 3시간 전, 괜찮아요". 동작 이름은 "분석 결과 보기".
- 입력 에러는 `AvTextField`의 기존 `error` 시맨틱과 polite live region으로 읽힌다.
- [붙여넣기]·[건너뛰기]는 S 크기 버튼이라 `minimumInteractiveComponentSize()`로 터치 영역 48dp를 확보한다.
- 페이지 점은 `clearAndSetSemantics { contentDescription = "2쪽 중 1쪽" }`로 처리한다. 일러스트는 장식(시맨틱 없음).
- 폭 전체 행의 포커스 링은 안쪽에 그린다(6절).
- 큰 글꼴(200%): 줄바꿈 문자를 넣지 않고 행 제목·부제는 한 줄 말줄임, 온보딩 쪽은 스크롤.
- 대비: 새 조합은 없다. 모두 0번 4.2절의 통과 조합(`textTertiary`·`accentText` on `background`, 배지 톤 쌍)이다.

## 10. 범위

**포함**: 2~9절 전부. 홈·온보딩 재작성, 공유 안내 시트, 일러스트 2종 토큰 정리, 최근 본 차 쿼리, 컴포넌트 파라미터 추가, 셸의 `lastAnalyzedUrl` 제거와 온보딩 모드, 문서(`DESIGN.md`, `CLAUDE.md`) 갱신.

**제외**
- 저장한 차·비교(3번), 설정 화면 정리·캐시 비우기 다이얼로그·앱 아이콘·스토어 자산(4번). `SavedListScreen`·`SettingsScreen`·`PrivacyPolicyScreen`의 `FontWeight.Medium`도 각 하위 프로젝트에서 처리한다.
- 최근 행 개별 삭제·스와이프, 최근 행의 "저장됨" 표시(저장 테이블 조인).
- 차 번호(carId)만 입력하는 방식, 엔카 앱 공유 문구가 다른 URL 형식일 때 `EncarUrl` 확장(11절 리스크, QA에서 확인 후 별도 결정).
- 클립보드 내용 감지 후 붙여넣기 버튼 강조(API 31+ `ClipDescription` 분류). 단순함을 위해 버튼을 항상 보여준다.
- 판정·점수 로직, vendored core, DB 스키마, 스플래시.

## 11. 리스크

| 리스크 | 대응 |
|---|---|
| 엔카 **앱**의 공유 텍스트가 `/cars/detail/{id}`가 아닌 형식일 수 있다(지금 파서는 이 형식만 앎) | 에뮬레이터에는 엔카 앱이 없을 수 있다. 실제 기기 공유 문구를 확인하기 전까지 안내 문구는 유지하고, 다르면 사용자에게 보고한 뒤 `EncarUrl` 확장을 별도로 결정한다. 그 전까지는 무효 공유 토스트로 안전하게 실패한다 |
| 자동 채움 제거로 탭이 한 번 늘어남 | 공유 경로(탭 0번)를 홈과 온보딩에서 더 잘 보이게 한다. 붙여넣기는 버튼 하나 |
| 브라우저마다 메뉴 위치가 다름 | 일러스트 아래 크롬·삼성 인터넷 위치를 문구로 안내(기존 유지) |
| `focusRingInside`가 다른 화면의 행 모양을 바꿈 | 링은 키보드·D-pad 포커스에서만 보인다. 갤러리에서 확인 |
| DB 쿼리 추가가 스키마를 바꾸는지 | 엔티티는 그대로. `app/schemas/.../1.json` diff가 비어 있어야 한다(리뷰 체크) |
| 1분 주기 재조회 비용 | 원본 JSON을 빼고 20행만 조회하며, RESUMED에서만 돈다 |

## 12. 검증 (적정 수준)

| 대상 | 방법 |
|---|---|
| 입력 로직 | JUnit `HomeInputTest`: 정규 URL, 앞뒤 문구가 붙은 공유 텍스트 → 정규화, 엔카 목록 URL·다른 사이트 → Invalid/NotEncar, 공백, `null` 클립 |
| 최근 표시 모델 | JUnit `RecentViewsTest`: 이전 형식 행(빈 verdict·title, score 0) → null 처리, 상대 시간 경계(59초·1분·59분·60분·23시간 59분), `cachedAt > now` |
| DAO | 기존 `AppDatabaseTest`에서 `getRecentFlow` 테스트 2개를 `recentViewsFlow`로 옮기고 `LIMIT`을 한 단언 추가 |
| 셸 | `AppRootTest`: `lastAnalyzedUrl` 관련 단언 제거, 첫 실행 온보딩과 보류 공유에서 `ScreenHost` 플래그가 맞는지 한 케이스. `AppNavigatorTest`는 변경 없이 통과 |
| 화면 | Robolectric Compose 테스트 파일 2개. `HomeScreenTest`: 무효 제출 시 에러 노출, 이전 형식 행 대체 제목·배지 없음, 빈 상태, 행 탭 콜백, 더보기 펼침. `OnboardingScreenTest`: 첫 실행 건너뛰기 노출, 다시 보기는 뒤로만 있고 마지막 버튼 "닫기", 마지막 쪽 버튼이 `onFinish` 호출 |
| 컴포넌트 | 기존 `AvTextFieldTest`에 액션 버튼이 빈 값일 때만 보이는지 한 케이스 추가 |
| 전체 | `./gradlew :app:testDebugUnitTest` 통과, vitest 변화 없음(TS 미변경) |
| 수동 QA (1회) | 전용 AVD `AV_API_36`(`-no-snapshot`)에서 한 번: 첫 실행 온보딩 → 건너뛰기 / 브라우저 공유 → 결과 / 붙여넣기(시스템 토스트 1회 확인)·무효 링크·빈 클립보드 / 두 매물 분석 후 최근 목록(배지·점수·시간) → 행 탭 → 결과 → 뒤로 / 공유 안내 시트·엔카 열기 / 설정 → 사용법 다시 보기 / 첫 실행 중 공유 → 문구 → 시작하기 → 결과 / 글꼴 200%에서 홈과 온보딩 1회. 스크린샷은 로컬에만 둔다 |

## 13. 작업 분할 (병렬 구현용)

`strings.xml` 충돌을 피하려고 문자열을 먼저 한 번에 넣고, 그다음 독립 작업을 병렬로 돌린다.

| 웨이브 | 작업 | 파일 | 의존 |
|---|---|---|---|
| 0 | **T0 문자열** | `res/values/strings.xml`(7절 키 전부) | 없음 |
| 1 (병렬) | **T1 컴포넌트** | `ui/components/AvTextField.kt`, `AvListRow.kt`, `AvPressable.kt`, `AvSection.kt`, `ui/gallery/ComponentGalleryScreen.kt`, `test/.../AvTextFieldTest.kt` | T0 |
| 1 (병렬) | **T2 데이터·순수 로직** | `db/RecentViewRow.kt`(신규), `db/CacheDao.kt`, `home/HomeInput.kt`(신규), `home/RecentViews.kt`(신규), `util/ClipboardUtil.kt`, `test/.../AppDatabaseTest.kt`, `HomeInputTest.kt`, `RecentViewsTest.kt` | T0 |
| 1 (병렬) | **T3 공유 안내** | `ui/components/BrowserShareIllustration.kt`, `ShareSheetIllustration.kt`, `home/ShareGuideSheet.kt`(신규) | T0 |
| 1 (병렬) | **T4 셸** | `ui/navigation/AppRoot.kt`, `AppNavigator.kt`, `AppScreenContent.kt`(홈 호출에서 `lastAnalyzedUrl`만 제거, 임시로 기존 `AnalyzeScreen` 유지), `test/.../AppRootTest.kt`, `test/.../ResultRouteTest.kt`(`ScreenHost` 생성자) | 없음 |
| 2 (병렬) | **T5 홈** | `home/HomeScreen.kt`, `home/HomeRoute.kt`(신규), `ui/screen/AnalyzeScreen.kt` 삭제, `AppScreenContent.kt`의 `Route.Home` 한 줄, `test/.../HomeScreenTest.kt` | T1, T2, T3, T4 |
| 2 (병렬) | **T6 온보딩** | `onboarding/OnboardingScreen.kt`(신규), `ui/screen/OnboardingScreen.kt` 삭제, `AppScreenContent.kt`의 `Route.Onboarding` 한 줄, `test/.../OnboardingScreenTest.kt` | T1, T3(일러스트), T4 |
| 3 | **T7 문서·QA** | `DESIGN.md`(컴포넌트 파라미터), `CLAUDE.md`(AppRoot 상태 문장), 이 문서에 완료 기록, 12절 수동 QA 1회 | T5, T6 |

- T5와 T6은 `AppScreenContent.kt`에서 서로 다른 한 줄씩만 고친다. 충돌이 나면 단순 병합하면 된다.
- 각 웨이브가 끝날 때 `./gradlew :app:testDebugUnitTest`가 통과해야 다음 웨이브로 넘어간다.
