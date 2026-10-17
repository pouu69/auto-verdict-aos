# UX/UI 개편 0번 — 디자인 언어 & 정보 구조 설계

- 작성일: 2026-10-17
- 브랜치: `feat/ux-revamp` (worktree `auto-verdict-aos-ux`)
- 상태: 설계 확정, 구현 계획 작성 전

## 1. 배경

앱 전체 UX/UI 개편에 앞서 코드와 화면을 감사한 결과, 다음 문제가 반복적으로 나타났다.

- **디자인 시스템 부재.** Apple 웹사이트 분석 문서(`DESIGN.md`)를 차용했지만 토큰화되지 않았다. 카드 반경 14/16/20/28dp 혼재, 버튼 52dp 알약형과 56dp/16dp 둥근형 혼재, 서브화면 상단바 3종, 공용 컴포넌트 없음(일러스트 2개뿐).
- **색 정의 이중화.** Compose `Color.kt`(`#0066CC`), `res/values/colors.xml`(`#0064FF`), WebView `tokens.ts`가 각각 따로 있다. 스플래시에서 다음 화면으로 넘어갈 때 색이 튄다.
- **렌더러 이중화.** 결과 화면만 React(WebView)로 그린다. 로딩 화면 2종, 바텀시트 동작 차이, 폰트 차이(Pretendard 지정만 있고 미번들)가 여기서 생긴다.
- **내비게이션 결함.** 메인 탭에서 뒤로가기를 누르면 즉시 종료된다. 서브화면 상태가 `remember`라 회전하면 사라진다. 이중 스플래시가 있다. 엔카가 아닌 링크를 공유하면 아무 반응이 없다.
- **문구 혼재.** 해요체, 합니다체, 영문 대문자 라벨(`CAUTION`, `AUTO ANALYSIS`)이 섞여 있다. 개발자 용어("DOM 텍스트 비어있음")가 노출된다. 문자열이 하드코딩되어 있다.

## 2. 전체 개편 로드맵

전체 리디자인은 독립적인 하위 프로젝트로 나눠, 각각 설계 → 계획 → 구현 사이클을 거친다. **이 문서는 0번만 다룬다.**

| # | 하위 프로젝트 | 내용 | 의존 |
|---|---|---|---|
| **0** | **디자인 언어 & 정보 구조** | 토큰, 폰트·아이콘, 공용 컴포넌트, 내비게이션 백스택, 문구 가이드 | 없음 |
| 1 | 결과 화면 | 결과 UI를 Compose로 이전, 헤드리스 엔진 브릿지, 판정 히어로·확인할 것·이력·판매자 질문, 로딩·에러·저장 흐름. 판정 로직 정합성(`fail` 미반영 등)을 함께 결정 | 0 |
| 2 | 진입 흐름 | 온보딩, 홈(링크 입력 + 최근 본 차), 공유 안내 | 0 |
| 3 | 저장 & 비교 | 저장한 차 목록(정렬·스와이프 삭제), 비교 모드, 비교 화면 | 0, 1 |
| 4 | 설정·정리 | 설정, 개인정보처리방침, 앱 아이콘·스토어 자산 교체, 접근성 마감 | 0 |

출시는 최소 1번까지 끝난 뒤에 한다. 0번만 적용된 상태는 새 내비게이션에 기존 화면 내용이 섞인 과도기 상태다.

## 3. 결정 사항

| 항목 | 결정 | 근거 |
|---|---|---|
| 비주얼 언어 | **토스 컨셉**: 큰 결론 문장, 흰 바탕과 그레이 스케일, 강조색 하나, 리스트 행 중심, 하단 고정 CTA | 주 사용자가 중고차 초보라 결론을 문장으로 말해 주는 방식이 적합 |
| 강조색 | 토스 파랑 `#3182F6` (기존 `#0066CC` 대체) | 사용자 결정. 앱 아이콘·스토어 자산 교체는 4번 |
| 테마 | **라이트만**. 다크 토큰은 정의하지 않음 | 사용자 결정 |
| 렌더링 | **결과 UI를 Compose로 이전**. WebView는 규칙 엔진만 헤드리스로 실행 (구현은 1번) | 디자인 시스템을 하나로, 네이티브 시트·제스처·접근성 |
| 컴포넌트 | **직접 구현** (TDS 사용 불가) | TDS·토스 아이콘은 앱인토스 전용 라이선스. 독립 Play 앱 + 광고 수익에는 사용 불가 |
| 그림 아이콘 | **토스페이스**(Tossface) 원본 폰트 번들, 출처 표기 | 무료 사용 허용. 변형·판매 금지, 출처 표기 필수 |
| UI 아이콘 | `Icons.Rounded` (기존 의존성 `material-icons-extended`) | 추가 의존성 없음, 둥근 형태 |
| 폰트 | **Pretendard** 번들 (OFL) | 토스 느낌에 가장 가까운 무료 한글 폰트 |
| 내비게이션 | **2탭(홈, 저장한 차) + 우상단 설정 아이콘**, 자체 `Route` 백스택 | 설정은 사용 빈도 낮음. 저장·비교는 구매 과정에서 자주 감 |
| 광고 | **현재 위치 유지**: 탭바 위 배너, 설정 화면 하단 배너, 결과 로딩 중 MREC, 결과 화면 하단 배너 | 사용자 결정. 스타일만 맞춤 |
| 전문 용어 | 중고차 업계 용어(전손, 침수, 성능점검, 프레임, 자차보험 공백)는 **그대로** 사용 | 사용자 결정. 설명은 상세 시트에서만 |

## 4. 디자인 토큰

### 4.1 구조

- 위치: `app/src/main/kotlin/com/car/autoverdict/ui/theme/`
- 의미 기반 토큰 객체 4개(`AvColors`, `AvTypography`, `AvShapes`, `AvSpacing`)를 `CompositionLocal`로 제공한다. 진입점은 `AvTheme { }`이고, 접근은 `AvTheme.colors.textPrimary` 형태다.
- `AvTheme`는 내부에서 M3 `MaterialTheme`도 감싼다. 컬러스킴의 모든 슬롯(`primary`, `surface`, `surfaceVariant`, `onSurfaceVariant`, `secondaryContainer`, `outline`, `error` 등)을 아래 값으로 채워 M3 기본 보라색이 새어 나오지 않게 한다.
- 화면 코드는 원시 색 상수(`Primary`, `TextPrimary` 등)를 직접 import하지 않는다. 기존 `Color.kt` 상수는 0번에서 삭제하고, 기존 화면의 참조는 4.5절 매핑표대로 기계적으로 치환한다(시각 재설계는 1~4번).
- `res/values/colors.xml`도 같은 값으로 맞춘다. `enableEdgeToEdge` 환경이라 `themes.xml`의 `statusBarColor`/`navigationBarColor`는 사실상 효과가 없다. 실질적인 대상은 스플래시 배경(7.2절)이다.

### 4.2 색

**원시 팔레트 (Grey)**

| 단계 | 값 | | 단계 | 값 |
|---|---|---|---|---|
| grey900 | `#191F28` | | grey400 | `#B0B8C1` |
| grey800 | `#333D4B` | | grey300 | `#D1D6DB` |
| grey700 | `#4E5968` | | grey200 | `#E5E8EB` |
| grey600 | `#6B7684` | | grey100 | `#F2F4F6` |
| grey500 | `#8B95A1` | | grey50 | `#F9FAFB` |

**의미 토큰**

| 토큰 | 값 | 용도 |
|---|---|---|
| `background` | `#FFFFFF` | 화면 바탕 |
| `surfaceMuted` | grey100 | 입력 필드, 사실 타일, secondary 버튼, 섹션 구분 띠 |
| `surfaceSubtle` | grey50 | 보조 박스 |
| `textPrimary` | grey900 | 제목, 본문 |
| `textSecondary` | grey700 | 보조 본문 |
| `textTertiary` | grey600 | 캡션, 메타 정보 |
| `textDisabled` | grey400 | 비활성 요소 |
| `border` | grey200 | 최소한의 구분선 |
| `accent` / `accentPressed` / `accentWeak` | `#3182F6` / `#1B64DA` / `#E8F3FF` | 주요 행동의 **채움색**, 눌림 상태, weak 버튼 배경 |
| `accentText` | `#1B64DA` | 강조색을 **글자·아이콘**으로 쓸 때 (링크, 텍스트 버튼, weak 버튼 라벨). `accent`는 글자색으로 쓰지 않음 |
| `onAccent` | `#FFFFFF` | accent 위 텍스트 |
| `dangerFill` / `dangerText` / `dangerBg` | `#F04452` / `#D22030` / `#FFEEEE` | 위험 |
| `warningFill` / `warningText` / `warningBg` | `#FE9800` / `#9A5000` / `#FFF4E5` | 주의 |
| `successFill` / `successText` / `successBg` | `#03B26C` / `#03804D` / `#E5F8EF` | 양호 |
| `neutralFill` / `neutralText` / `neutralBg` | grey400 / grey700 / grey100 | 확인 필요 |
| `toastBg` / `toastAction` | grey800 / `#90C2FF` | 토스트 배경 / 토스트 액션 글자 (5.94:1) |
| `scrim` | `#191F28` @ 50% | 시트·다이얼로그 뒤 딤 |

**명도 대비 정책 (WCAG AA 4.5:1)**

| 조합 | 대비 | 판정 |
|---|---|---|
| `dangerText` on `dangerBg` | 4.69 | 통과 |
| `warningText` on `warningBg` | 5.49 | 통과 |
| `successText` on `successBg` | 4.53 | 통과 |
| `neutralText` on `neutralBg` | 6.45 | 통과 |
| `textSecondary` on `background` / `surfaceMuted` | 7.11 / 6.45 | 통과 |
| `textTertiary` on `background` | 4.62 | 통과 |
| `accentText` on `background` / `surfaceMuted` / `accentWeak` | 5.41 / 4.91 / 4.82 | 통과 (링크, weak 버튼 라벨) |
| `toastAction` on `toastBg` | 5.94 | 통과 |
| `textTertiary` on `surfaceMuted` | 4.19 | **플레이스홀더에만 예외 허용** (`AvTextField`, 앞에 아이콘이 함께 있음). 그 외 `surfaceMuted` 위 보조 텍스트는 `textSecondary` |
| `onAccent` on `accent` | 3.71 | **예외 허용**. 토스 파랑 유지 결정에 따름. Primary 버튼 라벨(`AvButton` Primary, SemiBold 15sp 이상)에만 사용. 0번 과도기에는 M3 `primary` 슬롯을 쓰는 기존 화면의 M3 컴포넌트(FilledButton, Switch 등)도 이 조합이 되며, 1~4번에서 화면을 재설계할 때 `AvButton`으로 교체해 해소 |
| 흰 글자 on `dangerFill` | 3.71 | **사용 금지**. Danger 버튼은 `dangerBg` 배경 + `dangerText` 라벨(4.69) |
| `textDisabled` | 2.01 | **예외 허용**. 비활성 요소에만 사용(WCAG 비활성 예외) |

위 표의 "통과" 조합은 단위 테스트로 4.5:1 이상을 고정한다. 테스트는 hex 값이 아니라 토큰을 참조해, 토큰 값을 바꾸면 테스트도 함께 검증되게 한다. "예외"는 테스트하지 않고, 사용처를 이 표에 적은 범위로 제한한다.

**톤 매핑** — `enum class AvTone { Accent, Danger, Warning, Success, Neutral }`

| 판정(Verdict 문자열) | 톤 | 기본 라벨 |
|---|---|---|
| `NEVER` | Danger | 피하세요 |
| `CAUTION` | Warning | 주의 |
| `OK` | Success | 괜찮아요 |
| `UNKNOWN` | Neutral | 확인 필요 |

`AvTone.fromVerdict(verdict: String?)`를 제공한다. 알 수 없는 값과 null은 Neutral이다. 라벨 문구는 1번에서 최종 확정하며, 0번에서는 위 값을 `strings.xml`에 둔다.

### 4.3 타이포그래피 (Pretendard)

| 토큰 | 크기 / 굵기 | 행간 | 자간 | 용도 |
|---|---|---|---|---|
| `title1` | 26sp / Bold | 1.35 | -0.6sp | 화면 결론 문장 |
| `title2` | 22sp / Bold | 1.35 | -0.5sp | 화면 제목 |
| `title3` | 19sp / SemiBold | 1.4 | -0.4sp | 시트 제목, 섹션 강조 |
| `body1` | 17sp / Medium | 1.5 | -0.3sp | 리스트 행 제목 |
| `buttonL` | 17sp / SemiBold | 1.3 | -0.3sp | 버튼 L 라벨 |
| `body2` | 15sp / Regular | 1.5 | -0.2sp | 본문, 리스트 부제 |
| `buttonM` | 15sp / SemiBold | 1.3 | -0.2sp | 버튼 M 라벨 |
| `buttonS` | 13sp / SemiBold | 1.3 | 0 | 버튼 S 라벨 (Primary 스타일로는 쓰지 않음) |
| `caption1` | 13sp / Regular | 1.45 | 0 | 메타 정보 |
| `caption2` | 11sp / Medium | 1.3 | 0 | 배지·칩 라벨 전용 (최소 크기) |

- 11sp 미만은 쓰지 않는다.
- M3 `Typography`의 모든 슬롯에 위 토큰 중 가장 가까운 값을 연결한다. 정의되지 않은 슬롯이 기본값으로 노출되지 않게 하기 위해서다.
- 모든 크기는 `sp`이고, 높이를 고정하지 않아 시스템 글꼴 크기 확대를 따른다.

### 4.4 모양, 간격, 모션

| 구분 | 값 |
|---|---|
| 반경 | `chip` 999dp · `tile` 12dp · `buttonS` 10dp · `buttonM` 14dp · `buttonL`/`input` 16dp · `sheetTop` 24dp |
| 간격 | 4 · 8 · 12 · 16 · 20 · 24 · 32dp (`AvSpacing.xs`~`xxxl`) |
| 화면 좌우 여백 | 20dp |
| 섹션 구분 | 8dp 높이 `surfaceMuted` 띠 |
| 그림자 | 사용하지 않음. 구분은 색 띠와 여백으로 한다 |
| 눌림 반응 | scale 0.96, 100ms, 손을 떼면 복귀. `Modifier.avPressable()`로 공통화 |
| 시트 | M3 `ModalBottomSheet` 기반, 드래그로 닫기 |

### 4.5 기존 상수 → 새 토큰 매핑 (0번 기계적 치환용)

현재 화면은 회색 페이지 위에 흰 카드를 얹어 구분한다. 1~4번에서 재설계하기 전까지 이 구조를 유지하도록, 기존 `Background`는 흰색이 아니라 `surfaceMuted`로 옮긴다.

| 기존 상수 | 새 토큰 |
|---|---|
| `Primary` | 채움·배경·테두리로 쓰면 `accent`, 글자·아이콘 색으로 쓰면 `accentText` |
| `PrimaryFocus` | `accentPressed` |
| `Background` (`#F5F5F7`) | `surfaceMuted` |
| `Surface` | `background` |
| `TextPrimary` | `textPrimary` |
| `TextSecondary` | `textSecondary` |
| `Danger` / `Warning` / `Success` | 글자·아이콘 색이면 `dangerText` / `warningText` / `successText`, 채움·배경이면 `dangerFill` / `warningFill` / `successFill` |
| `DangerBg` / `WarningBg` / `SuccessBg` | `dangerBg` / `warningBg` / `successBg` |
| `Border`, `Hairline` | `border` |
| `Primary.copy(alpha = …)` 배경 틴트 | `accentWeak` |
| 그 외 `*.copy(alpha = …)` | 토큰에 같은 alpha를 유지 (0번에서는 재해석하지 않음) |
| SavedList·Compare의 점수 그라디언트 하드코딩 RGB | 그대로 둠 (3번에서 재설계) |

### 4.6 M3 슬롯 매핑

| ColorScheme 슬롯 | 토큰 | | Typography 슬롯 | 토큰 |
|---|---|---|---|---|
| `primary` / `onPrimary` | `accent` / `onAccent` | | `display*`, `headlineLarge`, `headlineMedium` | `title1` |
| `primaryContainer` / `onPrimaryContainer` | `accentWeak` / `accentText` | | `headlineSmall`, `titleLarge` | `title2` |
| `secondary` / `onSecondary` | grey700 / `#FFFFFF` | | `titleMedium` | `body1` |
| `secondaryContainer` / `onSecondaryContainer` | `surfaceMuted` / `textPrimary` | | `titleSmall` | `buttonM` |
| `background` / `onBackground` | `background` / `textPrimary` | | `bodyLarge`, `bodyMedium` | `body2` |
| `surface` / `onSurface` | `background` / `textPrimary` | | `bodySmall`, `labelMedium` | `caption1` |
| `surfaceVariant` / `onSurfaceVariant` | `surfaceMuted` / `textSecondary` | | `labelSmall` | `caption2` |
| `surfaceContainerLowest` ~ `surfaceContainerHighest` | `background`, `background`, `surfaceSubtle`, `surfaceMuted`, grey200 순 | | `labelLarge` | `buttonL` |
| `outline` / `outlineVariant` | grey300 / `border` | | | |
| `error` / `onError` / `errorContainer` | `dangerText` / `#FFFFFF` / `dangerBg` | | | |
| `inverseSurface` / `inverseOnSurface` | `toastBg` / `#FFFFFF` | | | |
| `scrim` | `scrim` | | | |

## 5. 폰트·아이콘·라이선스

| 자산 | 방식 | 라이선스 처리 |
|---|---|---|
| Pretendard | Regular·Medium·SemiBold·Bold 정적 4종을 `res/font/`에 번들. `FontFamily`로 묶어 `AvTypography`에서 사용 | SIL OFL 1.1. 라이선스 전문을 앱 내 "오픈소스 라이선스" 화면에 표시 |
| 토스페이스 | 공식 배포 TTF **원본 그대로** `res/font/`에 번들. 서브셋·형식 변환 금지 | 변형·판매 금지, 출처 표기 필수. 라이선스 화면에 "이 앱에는 토스팀에서 제공한 토스페이스가 적용되어 있습니다." 표기 |
| UI 아이콘 | `Icons.Rounded.*` | Apache 2.0 (Compose 의존성에 포함). 라이선스 화면에 표기 |

**크기 예산:** 폰트 추가로 release AAB가 10MB 넘게 커지면 구현을 멈추고 사용자와 대안(예: 토스페이스 제외)을 다시 정한다. 실제 증가량은 구현 시 측정해 이 문서에 기록한다.

**폰트 출처:** 공식 배포처에서만 받는다. Pretendard는 `github.com/orioncactus/pretendard` 릴리스, 토스페이스는 `github.com/toss/tossface` 릴리스다. 다운로드 전 사용자 확인을 받는다.

**파일명:** `res/font`는 소문자·숫자·밑줄 파일명만 허용한다. 예를 들어 `pretendard_semibold.otf`, `tossface.ttf`처럼 파일명만 바꾼다. 파일 내용은 바이트 단위로 원본과 같으므로 라이선스상 변형에 해당하지 않는다. 구현 시 원본과 SHA-256이 같은지 확인해 기록한다.

**토스페이스 렌더링 검증 (구현 첫 단계, 통과해야 진행):**
- 배포 TTF의 컬러 글리프 형식을 Android가 그리는지 확인한다. 검증 기기는 API 26(최소), 29, 33, 36 에뮬레이터다.
- 검증 조건은 Compose `Text`에 `FontFamily(tossface)`를 지정하고 `PlatformTextStyle(emojiSupportMatch = EmojiSupportMatch.None)`으로 EmojiCompat 대체를 끈 상태다.
- 판정 기준: 네 API 모두에서 컬러 토스페이스 글리프가 보이면 통과다. 하나라도 시스템 이모지나 빈 글리프(□)로 나오면 실패다.
- **실패 시:** 토스페이스를 번들에서 빼고, `AvEmoji`를 `AvIllustrationIcon(icon: ImageVector, tone: AvTone)`으로 바꾼다. 이 컴포넌트는 톤 배경의 원 안에 `Icons.Rounded` 아이콘을 그린다. 형식 변환이나 이미지 추출 같은 우회는 하지 않는다. 결과는 사용자에게 보고하고 이 문서에 기록한다.

**라이선스 화면 범위 (0번):** 이번에 새로 들어오는 자산만 다룬다. Pretendard(OFL 전문), 토스페이스(출처 문구), Material Symbols(Apache 2.0)다. AndroidX, Play services 등 기존 의존성 전체를 고지하는 작업은 4번에서 한다.

## 6. 공용 컴포넌트

위치: `app/src/main/kotlin/com/car/autoverdict/ui/components/`. 모든 컴포넌트는 상태를 갖지 않는다(stateless). 텍스트, 상태, 콜백만 파라미터로 받고 색·크기는 토큰에서만 가져온다.

| 컴포넌트 | 시그니처(요지) | 비고 |
|---|---|---|
| `AvTopBar` | `(onBack: (() -> Unit)?, title: String? = null, subtitle: String? = null, actions: @Composable RowScope.() -> Unit = {})` | 탭 루트(`onBack = null`)와 서브화면 모두 사용. 뒤로 아이콘은 `Icons.Rounded.ArrowBackIosNew` 하나로 통일. 상태바 인셋을 직접 처리 |
| `AvButton` | `(text, onClick, style = Primary, size = Large, enabled = true, loading = false, modifier)` | `AvButtonStyle { Primary, Weak, Secondary, Danger }`, `AvButtonSize { Large(56dp), Medium(48dp), Small(36dp) }`. 높이는 최소 높이(`heightIn(min=)`)로 지정. 라벨은 L=`buttonL`, M=`buttonM`, S=`buttonS`. Primary는 L/M만 허용. Weak=`accentWeak`+`accentText`, Secondary=`surfaceMuted`+`textSecondary`, Danger=`dangerBg`+`dangerText` |
| `AvBottomCta` | `(content: @Composable RowScope.() -> Unit)` | 하단 고정 영역. 시스템바·키보드 인셋 처리, 상단 1dp `border` |
| `AvListRow` | `(title, onClick: (() -> Unit)? = null, subtitle: String? = null, leading: (@Composable () -> Unit)? = null, trailing: (@Composable () -> Unit)? = null)` | 최소 높이 56dp. 클릭 가능하면 `Role.Button`과 눌림 반응 |
| `AvTextField` | `(value, onValueChange, placeholder, leadingIcon: ImageVector? = null, errorText: String? = null, onClear: (() -> Unit)? = null)` | `surfaceMuted` 채움형. 에러 문구는 필드 아래 `dangerText` |
| `AvBadge` | `(text, tone: AvTone)` | 알약형, `caption2`. 시맨틱 설명은 `"$text 판정"` 등 호출부에서 지정 가능 |
| `AvFactTile` | `(label, value, tone: AvTone = Neutral)` | Neutral은 `surfaceMuted`, 그 외는 톤의 `*Bg` 배경 |
| `AvSectionHeader` | `(title, count: Int? = null, actionLabel: String? = null, onAction: (() -> Unit)? = null)` | |
| `AvSectionDivider` | `()` | 8dp 띠 |
| `AvEmptyState` | `(emoji: String, title, description: String? = null, actionLabel: String? = null, onAction: (() -> Unit)? = null)` | 그림은 `AvEmoji` 사용 |
| `AvEmoji` | `(emoji: String, size: Dp)` | 토스페이스 `FontFamily` + `emojiSupportMatch = None`. 장식용이면 `contentDescription` 없음. 렌더링 검증 실패 시 `AvIllustrationIcon`으로 대체(5절) |
| `AvBottomSheet` | `(onDismiss, title: String? = null, content: @Composable ColumnScope.() -> Unit)` | 상단 24dp 반경, 핸들, 드래그 닫기 |
| `AvToastHost` / `AvToastState` | `state.show(message, actionLabel: String? = null, onAction: (() -> Unit)? = null)` | `SnackbarHost` 기반. `toastBg` 배경, 액션 글자는 `toastAction`. `AvToastState`는 앱 루트(`AppRoot`)가 소유하고 `CompositionLocal`로 내려준다 |
| `AvDialog` | `(title, message, confirmLabel, onConfirm, dismissLabel, onDismiss, destructive = false)` | `destructive`면 확인 버튼이 Danger 스타일 |
| `AvTabBar` | `(selected: MainTab, onSelect: (MainTab) -> Unit, savedCount: Int)` | `enum class MainTab { Home, Saved }`가 `Route.Home`/`Route.Saved`와 1:1로 대응한다. 선택 탭은 `textPrimary`, 비선택 탭은 `textTertiary`. `savedCount`는 `AppRoot`가 `savedCarDao().getAllFlow()`의 크기로 수집한다. 내비바 인셋을 직접 처리 |
| `AvLoadingSteps` | `(steps: List<LoadingStep>)` — `LoadingStep(label, state: Done/Active/Pending)` | 0번은 컴포넌트만 만든다. 수집 단계 연결은 1번 |

**접근성 기본값**
- 터치 영역은 최소 48dp다.
- 클릭 요소에는 `Role`을 붙인다.
- 장식 아이콘은 `contentDescription = null`, 의미 있는 아이콘은 설명을 필수로 받는다.
- 행 단위 요소는 `Modifier.semantics(mergeDescendants = true)`로 묶어 한 번에 읽히게 한다.

**시스템 인셋 책임 (중복 처리 금지)**

| 층 | 처리하는 인셋 |
|---|---|
| `AppRoot` | 없음. 루트에 일괄 패딩을 주지 않는다 |
| `AvTopBar` | `statusBars` |
| `AvTabBar` | `navigationBars` (탭 루트 화면의 하단) |
| `AvBottomCta` | `navigationBars` + `ime` |
| 결과 화면 하단 영역 | `ResultScreen`의 하단 컨테이너가 `navigationBars`를 처리. 배너가 있을 때는 배너 아래, 로딩 중(배너 없음)에는 빈 여백 |
| 설정 화면 하단 배너 | 배너 컨테이너가 `navigationBars` |
| 하단 고정 요소가 없는 서브화면 | 스크롤 콘텐츠 끝에 `navigationBars` 여백 |
| 텍스트 입력이 있는 탭 루트(홈) | 콘텐츠 영역에 `consumeWindowInsets`(탭바 + 배너 높이) 후 `imePadding()`. 탭바·배너 높이만큼 여백이 이중으로 생기지 않게 한다. 키보드가 열리면 입력창이 키보드 위로 올라오고, 탭바와 배너는 키보드에 가려진다 |

**컴포넌트 갤러리**
- `ui/gallery/ComponentGalleryScreen.kt`에 모든 컴포넌트를 상태별(기본, 비활성, 로딩, 에러, 톤 4종)로 나열한다.
- `BuildConfig.DEBUG`일 때만 설정 화면에 "컴포넌트 갤러리" 행이 보인다. release에서는 진입 경로가 없다.

## 7. 정보 구조 & 내비게이션

### 7.1 화면 구성

```
OS 스플래시 (1회)
 ├─ 첫 실행 → 온보딩 → 홈 탭
 └─ 그 외   → 홈 탭

메인 (2탭, 탭바 위 배너 광고)
 ├─ 홈 탭          (우상단 설정 아이콘)
 └─ 저장한 차 탭   (우상단 비교 아이콘, 기존과 동일)

서브화면 (스택에 push)
 ├─ 결과   ← 홈 링크 입력 / 최근 본 차 / 저장한 차 / 엔카 공유
 │          (비교 → 결과 경로는 3번에서 추가, 그때 fromSaved = true)
 ├─ 비교   ← 저장한 차 (2대 선택)
 └─ 설정   ← 홈 우상단 (하단 배너 광고 유지)
     ├─ 개인정보처리방침
     ├─ 오픈소스 라이선스 (신규)
     ├─ 사용법 다시 보기 → 온보딩
     └─ 컴포넌트 갤러리 (debug 전용)
```

### 7.2 Route 백스택

CLAUDE.md의 원칙(내비게이션 라이브러리 미사용)을 유지한다. 대신 흩어진 `remember` 변수와 early `return`을 백스택 하나로 바꾼다.

```kotlin
sealed interface Route {
    data object Onboarding : Route
    data object Home : Route          // 탭 루트
    data object Saved : Route         // 탭 루트
    data object Settings : Route
    data object PrivacyPolicy : Route
    data object Licenses : Route
    data object Gallery : Route       // debug 전용
    data class Result(val url: String, val fromSaved: Boolean) : Route
    data class Compare(val carIds: List<String>) : Route
}
```

**상태 보존**
- 스택은 `rememberSaveable(saver = RouteStackSaver)`로 보존한다. `RouteStackSaver`는 각 Route를 문자열 리스트로 직렬화하는 커스텀 `listSaver`다. `kotlin-parcelize` 플러그인은 추가하지 않는다.
- 회전이나 프로세스 복원 뒤에는 스택 전체가 복원된다. 화면 내부 상태(저장 목록 정렬, 비교 모드 선택 등)는 각 화면이 지금 하는 만큼만 유지되고, 0번에서 범위를 넓히지 않는다.
- 결과 화면은 회전하면 WebView가 다시 만들어져 수집이 재실행된다. 24시간 캐시가 있으면 캐시를 쓴다. 0번에서는 이 동작을 허용한다(1번에서 재설계).
- 지금 `MainScreen`의 `rememberSaveable`에 있는 `lastAnalyzedUrl`은 `AppRoot` 수준으로 끌어올린다. 결과에서 돌아올 때 클립보드의 같은 링크로 입력창이 다시 채워지는 것을 막는 상태라서, 기존 동작을 유지해야 한다.

**`AppNavigator`**
- 순수 Kotlin 클래스로 만들고 Compose와 Android 의존성을 두지 않는다. 입력은 현재 스택과 이벤트, 출력은 새 스택과 전환 종류(`Push`, `Pop`, `Replace`)다. JUnit으로 단위 테스트한다.
- `ResultScreen`의 `NativeBridge(onClose = onBack)`처럼 콜백을 `remember`로 한 번만 캡처하는 곳은 `rememberUpdatedState`로 감싼다. 그래야 항상 현재 스택을 기준으로 뒤로 간다.

| 이벤트 | 규칙 | 전환 |
|---|---|---|
| 앱 시작, 온보딩 미완료 | `[Onboarding]` | 없음 |
| 앱 시작, 온보딩 완료 | `[Home]` | 없음 |
| 탭 선택(다른 탭) | `[선택한 탭 루트]`로 교체 | 없음(즉시) |
| 탭 재선택(같은 탭) | 변화 없음 | 없음 |
| 화면 열기 | `push(route)` | Push |
| 뒤로가기, 스택 크기 2 이상 | `pop` | Pop |
| 뒤로가기, `[Saved]` | `[Home]`으로 교체 | 없음 |
| 뒤로가기, `[Home]` 또는 `[Onboarding]` | 처리하지 않음(`BackHandler` 비활성). 시스템 기본 동작(predictive back, 백그라운드 이동)에 맡김 | 시스템 |
| 엔카 공유(유효), 온보딩 완료 | `[Home, Result(정규화 url, false)]`로 교체 | Replace |
| 엔카 공유(유효), 온보딩 미완료 | `[Onboarding]` 유지, 정규화 url을 `pendingSharedUrl`(saveable)에 보관 | 없음 |
| 엔카 공유(무효) | 스택 그대로 두고 토스트 "엔카 매물 링크만 확인할 수 있어요" | 없음 |
| 온보딩 완료, 스택이 `[Onboarding]` | 완료 플래그 저장. `pendingSharedUrl`이 있으면 `[Home, Result(url, false)]`, 없으면 `[Home]` | Replace |
| 온보딩 완료, 스택에 다른 화면이 있음(설정에서 다시 보기) | `pop` | Pop |
| 같은 차의 Result push | 스택 top이 같은 `carId`의 Result면 push하지 않음 | 없음 |

**공유 인텐트 처리**
- 판정은 순수 함수 `ShareIntentParser.parse(action: String?, type: String?, text: String?, isRestored: Boolean): NavEvent?`로 분리한다. 결과는 `NavEvent.OpenShared(url)`, `NavEvent.InvalidShare`, `null`(무시) 중 하나다. Activity는 이 함수를 호출만 한다.
- `EXTRA_TEXT`에서 `EncarUrl.extractCarId`로 차 ID를 뽑고 `EncarUrl.buildDetailUrl(carId)`로 정규화한 url만 Route에 담는다. 공유 문구의 앞뒤 텍스트("[엔카] 차명 …")는 버린다.
- `onCreate`에서는 `savedInstanceState == null`일 때만 인텐트를 처리한다. 회전이나 프로세스 복원 때 같은 인텐트로 복원된 스택을 덮어쓰거나 토스트를 반복하지 않게 하기 위해서다. 그 뒤로 들어오는 공유는 `onNewIntent`에서 처리한다.
- 인텐트 처리는 composition 전에 일어날 수 있다. 결과는 Activity의 일회성 상태(`pendingNavEvent`)에 담고, `AppRoot`의 `LaunchedEffect`가 소비해 내비게이터와 토스트에 반영한다.

**렌더링**
- `AppRoot`는 다음 순서로 쌓는다: `AnimatedContent(스택 top)` → (탭 루트일 때) 배너 광고 → `AvTabBar`.
- 배너 광고와 탭바는 `AnimatedContent` **바깥**에 두어 탭을 바꿔도 `AdView`가 다시 만들어지지 않게 한다.
- 전환 애니메이션: Push는 오른쪽에서 들어오는 슬라이드(250ms), Pop은 오른쪽으로 나가는 슬라이드, Replace와 탭 전환은 애니메이션 없음.

**스플래시**
- Compose `BrandSplashScreen`을 제거한다.
- OS `SplashScreen`의 `setKeepOnScreenCondition`으로 온보딩 완료 여부를 읽을 때까지만 유지한다.
- 스플래시 배경은 `accent`(`#3182F6`)다. 기존 런처 전경 아이콘이 흰색이라 흰 배경에서는 보이지 않기 때문이다. 아이콘 자체는 그대로 쓴다(교체는 4번).
- 파랑 스플래시에서 흰 홈 화면으로 넘어가는 색 전환은 남는다. 이중 스플래시와 두 파랑 사이의 색 차이는 없어진다. 스플래시를 흰색으로 바꾸는 일은 4번에서 아이콘을 교체할 때 한다.

### 7.3 0번에서 기존 화면에 적용하는 범위

원칙은 **내용은 그대로, 연결과 껍데기만 교체**다. 화면별 변경은 아래로 한정한다.

| 화면 | 변경 |
|---|---|
| 공통 | 색 상수를 4.5절대로 치환. 화면 상태(`remember`)와 콜백을 `AppNavigator`에 연결 |
| `AnalyzeScreen` (홈 탭) | M3 `TopAppBar`를 `AvTopBar(onBack = null, title = "AutoVerdict", actions = 설정 아이콘)`으로 교체. `lastAnalyzedUrl`은 파라미터로 받음 |
| `SavedListScreen` (저장한 차 탭) | 상단바를 `AvTopBar(onBack = null, title = "저장한 차", actions = 비교 아이콘)`으로 교체. 나머지 동작 그대로 |
| `ResultScreen` | 직접 만든 상단바를 `AvTopBar(onBack, title = "분석 결과", subtitle = 저장된 매물일 때 "저장된 매물", actions = "엔카에서 보기")`로 교체. WebView, 로딩, 광고 그대로 |
| `CompareScreen` | 직접 만든 상단바를 `AvTopBar(onBack, title = "비교 분석", subtitle = "2대 매물을 한눈에")`로 교체. 닫기(X) 대신 뒤로 아이콘 |
| `SettingsScreen` | 탭에서 서브화면으로 바뀜. **예외 변경:** `onBack` 파라미터 추가, `AvTopBar` 적용, "오픈소스 라이선스" 행 추가, debug 빌드에서만 "컴포넌트 갤러리" 행 추가, 하단 배너 광고 유지 |
| `PrivacyPolicyScreen` | `LargeTopAppBar`(스크롤 접힘)를 `AvTopBar(onBack, title = "개인정보 처리방침")`로 교체. 접힘 동작은 없어짐 |
| `OnboardingScreen` | 색 치환만. 완료와 건너뛰기는 `AppNavigator` 이벤트로 연결 |
| `LicensesScreen` | 신규 (5절 범위) |
| `ComponentGalleryScreen` | 신규 (debug 전용) |

## 8. 렌더링 구조 결정 (구현은 1번)

```
엔카 URL
  → CollectorWebView (기존, 수집 단계 이벤트 추가)
  → raw JSON
  → EngineWebView (화면에 보이지 않음, 규칙 엔진 번들만 로드)
       window.evaluate(json) → Android.onReport(reportJson)
  → Kotlin EvaluationReport (kotlinx.serialization)
  → Compose 결과 화면
```

- TS 규칙 엔진(`webview-bundle/src/core/`, `daksin-car`와 공유하는 vendored core)은 유지한다. React 오버레이(`overlay/*`, `App.tsx`)는 1번에서 제거한다.
- 엔진이 넘기는 JSON(`EvaluationReport`)에는 판정, 점수, 룰 결과에 더해 지금 버려지는 데이터(사고 상세, 보험 공백 구간, 차량 기본정보)를 담는다. 스키마는 1번 설계의 첫 항목이다.
- QuickJS 같은 JS 엔진 라이브러리는 채택하지 않는다. 의존성이 늘고, 수집을 위해 WebView가 어차피 필요하다.
- **0번에서는 이 구조를 구현하지 않는다.** 기존 `EvalWebView` 결과 화면은 1번 완료 전까지 그대로 동작한다.

## 9. 문구 가이드

| 원칙 | 예 |
|---|---|
| 모두 해요체, 결론 먼저 | "사기 전에 두 가지는 꼭 확인하세요" |
| 앱이 한 일보다 사용자가 할 일을 말한다 | "DOM 텍스트 비어있음" 대신 "엔카에 로그인하면 확인할 수 있어요" |
| 영문 라벨·개발자 용어 금지 | `CAUTION`, `AUTO ANALYSIS`, "추출 실패", "데이터 미수집" 금지 |
| 중고차 전문 용어는 그대로 쓴다. 설명은 상세 시트에서만 | 전손, 침수, 성능점검, 프레임, 자차보험 공백 |
| 에러는 무슨 일 + 다음 행동 | "엔카 페이지를 열지 못했어요. 네트워크를 확인하고 다시 시도해 주세요" |
| 버튼은 동사형, 2~3단어 이내 | "저장하기", "비교하기", "다시 시도" |
| 숫자는 읽기 쉬운 단위 | "8.2만km", "2,180만원", "2022년 2월~9월" |
| 본문에 이모지 금지 | 그림이 필요하면 `AvEmoji` 자리에만 |

- 새로 만들거나 손대는 화면의 문구는 `res/values/strings.xml`로 옮긴다. 0번에서 옮기는 대상은 새 컴포넌트·내비게이션·라이선스 화면·토스트 문구다.
- 이 가이드는 새 `DESIGN.md`에 포함한다.

## 10. 광고 배치

현재 위치를 유지한다. `AdBanner` 컴포넌트의 래퍼 스타일(배경, 여백)만 토큰에 맞춘다.

| 위치 | 형식 | 노출 조건 |
|---|---|---|
| 탭바 바로 위 | 320x50 배너 | 스택 top이 탭 루트(홈, 저장한 차)일 때. `AnimatedContent` 바깥에 두어 탭 전환 시 재요청하지 않음 |
| 설정 화면 하단 | 320x50 배너 | 설정이 탭에서 서브화면이 되어도 기존 노출을 유지 |
| 결과 로딩 중 | 300x250 MREC | 기존과 동일 |
| 결과 화면 하단 | 320x50 배너 | 로딩 완료 후 (기존과 동일) |

## 11. 범위

**0번에 포함**
1. `AvTheme`와 토큰 4종, M3 컬러스킴·타이포 슬롯 연결(4.6절), `colors.xml` 정렬, 기존 `Color.kt` 상수 제거
2. 토스페이스 렌더링 검증(5절), Pretendard·토스페이스 번들, `LicensesScreen`
3. 공용 컴포넌트 16종과 `Modifier.avPressable()`, 컴포넌트 갤러리(debug)
4. `Route` 백스택, `RouteStackSaver`, `AppNavigator`, `AppRoot`, 2탭 + 설정 아이콘, 뒤로가기 규칙, 공유 인텐트 처리, OS 스플래시 단일화, 화면 전환 애니메이션
5. 기존 화면 적용(7.3절 표 범위로 한정)
6. `DESIGN.md`를 토스 기준 문서로 교체, `CLAUDE.md`의 내비게이션 규칙 설명 갱신

**0번에서 제외**
- 각 화면의 내용·레이아웃 재설계 (1~4번)
- 헤드리스 엔진 브릿지와 결과 화면 Compose 이전 (1번)
- 판정·점수 로직 수정 (1번과 함께 결정)
- 비교 화면에서 결과로 가는 경로 (3번)
- 기존 의존성 전체의 오픈소스 고지, 앱 아이콘, 피처 그래픽, 스토어 스크린샷 교체 (4번)
- 다크 테마

**구현 계획 단계 분할 권장:** ① 토큰·폰트(토스페이스 검증 포함) → ② 공용 컴포넌트·갤러리 → ③ 내비게이션·기존 화면 연결 → ④ 문서. 각 단계가 끝날 때 빌드와 전체 테스트가 통과해야 한다.

## 12. 검증

| 대상 | 방법 |
|---|---|
| 기존 동작 | 기존 Kotlin 테스트 42개, vitest 56개가 모두 통과 |
| `AppNavigator` | JUnit, 7.2절 이벤트 표의 모든 행을 한 케이스 이상: 탭 전환·재선택, push/pop, `[Saved]`·`[Home]`·`[Onboarding]` 뒤로가기, 공유 유효·무효·온보딩 중 공유, 온보딩 완료 두 경로(pending url 유무 포함), 같은 `carId` 중복 push 방지, 전환 종류 반환 |
| 공유 url 정규화 | JUnit: 앞뒤 텍스트가 붙은 공유 문구에서 `buildDetailUrl(carId)` 결과가 나오는지 |
| 상태 보존 | Compose `StateRestorationTester`: `AppRoot`의 스택과 `lastAnalyzedUrl`, `pendingSharedUrl` 복원. `AppRoot`는 광고 슬롯(`adBanner: @Composable () -> Unit`)과 화면 슬롯을 파라미터로 받아 테스트에서 빈 구현을 주입한다(AdMob·WebView 초기화 회피) |
| 인텐트 처리 | JUnit으로 `ShareIntentParser.parse` 검증: SEND+text/plain+유효 링크, 무효 텍스트, 다른 action/type, `isRestored = true`면 `null`. `launchMode`가 standard라 공유는 대부분 새 인스턴스의 `onCreate`로 들어오고 `onNewIntent`는 드물다. 두 경로 모두 같은 파서를 거치는지 수동 시나리오로 각각 확인 |
| 토큰 | JUnit: 4.2절 대비 표의 "통과" 조합이 모두 4.5:1 이상, `AvTone.fromVerdict` 매핑(null·알 수 없는 값 포함) |
| 컴포넌트 | Robolectric + Compose UI 테스트(`testImplementation` `ui-test-junit4`, `debugImplementation` `ui-test-manifest`): `AvButton`(클릭, 비활성, 로딩 중 클릭 무시), `AvListRow`(Role, 병합 시맨틱), `AvBottomCta`(렌더링), `AvTextField`(에러 문구 노출), `AvTabBar`(선택 콜백) |
| 토스페이스 | 5절 렌더링 검증 결과(API 26/29/33/36 스크린샷)를 이 문서에 기록 |
| 시각 | debug 빌드 컴포넌트 갤러리와 기존 화면 6종을 에뮬레이터에서 확인, 스크린샷을 PR에 첨부 |
| 크기 | release AAB 크기 변화를 측정해 5절에 기록 |

## 13. 리스크

| 리스크 | 대응 |
|---|---|
| 0번만 머지된 과도기에 새 내비게이션 + 기존 화면이 섞임 | 0~1번은 `feat/ux-revamp`에서 이어서 진행. 1번 완료 전에는 출시하지 않음 |
| 폰트 번들로 앱 크기 증가 | 5절 크기 예산(10MB) 초과 시 중단하고 재결정 |
| 토스페이스 컬러 글리프를 일부 Android 버전이 그리지 못함 | 5절 검증 단계에서 먼저 확인. 실패하면 `AvIllustrationIcon`으로 대체 |
| 토스페이스 라이선스 해석(서브셋·변환이 "변형"인지 불명) | 원본 파일을 그대로 번들(파일명만 변경, 해시로 동일성 확인). 서브셋·변환하지 않음 |
| `onAccent` on `accent` 대비 3.71:1 | 토스 파랑 유지 결정에 따른 의도된 예외. Primary 버튼 라벨에만 사용하고 SemiBold 15sp 이상 |
| `Route` 직렬화 크기(Compare carIds, Result url) | 값이 작아 `Bundle` 한도 문제 없음. 대용량 데이터는 Route에 넣지 않는다 |
| 기존 화면 색 치환으로 미세한 시각 변화 | 의도된 변화. 갤러리와 주요 화면 스크린샷으로 확인 |
| 인셋 처리 변경으로 키보드·시스템바 겹침 | 6절 인셋 책임표를 따르고, 홈 입력창 + 키보드, 결과 하단 배너를 에뮬레이터에서 확인 |
