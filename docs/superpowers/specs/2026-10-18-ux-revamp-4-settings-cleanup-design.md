# UX/UI 개편 4번 — 설정·정리 설계 (앱 아이콘, 스토어 자산, 오픈소스 고지)

- 작성일: 2026-10-18
- 기준: `main` f73fdbd (1번 결과 화면 병합 직후)
- 선행: 0번 디자인 언어(`2026-10-17-ux-revamp-0-design-language-design.md`), 1번 결과 화면(`2026-10-17-ux-revamp-1-result-screen-design.md`)
- 상태: 설계 확정(사용자 부재 중 자율 결정, 근거는 각 절의 결정 표), 구현 계획 작성 전
- 병행: 2번(진입 흐름)·3번(저장 & 비교)이 같은 시기에 진행된다. 4번은 그 화면들(`AnalyzeScreen`·`OnboardingScreen`·`SavedListScreen`·`CompareScreen`)을 건드리지 않는다

## 1. 목표

1. 설정 화면을 토스 스타일로 다시 만든다. 0번 14절의 후속 과제(시스템 `Toast`·M3 `AlertDialog` 교체, 토스트와 하단 배너 겹침, 행 눌림 반응 통일)를 해결한다.
2. 개인정보 처리방침 화면을 새 디자인으로 바꾸고, 공개 방침과 어긋난 내용을 바로잡는다.
3. 앱 아이콘을 토스 파랑 디자인 언어에 맞는 새 적응형 아이콘으로 바꾼다. 테마 아이콘(API 33+)도 지원하고, 스플래시를 흰색으로 바꾼다.
4. 스토어 자산(설명 문구, 스크린샷 목록·캡션, 피처 그래픽, Play 아이콘)을 저장소 안에서 만든다. 업로드는 하지 않는다.
5. 릴리스에 들어가는 모든 런타임 의존성의 오픈소스 고지를 오프라인에서 만들고, 라이선스별로 묶은 목록에서 전문을 볼 수 있게 한다.
6. 지금 하는 게 맞는 정리 작업(죽은 리소스, 남은 M3 사용, 문구)을 함께 한다.

**하드 제약**: DB 스키마 변경 금지(DAO 쿼리 추가는 허용), 판정 로직 변경 금지, `webview-bundle/src/core` 수정 금지, 다운로드 금지(새 Gradle 플러그인·의존성, npm 설치, 인터넷에서 라이선스 전문 받기 모두 금지).

## 2. 결정 사항

| 항목 | 결정 | 근거 |
|---|---|---|
| 설정 구조 | 흰 바탕 + `AvSectionHeader` + `AvListRow` + `AvSectionDivider`. 카드·그림자·행 앞 아이콘 상자를 없앤다 | DESIGN.md 원칙(리스트 행과 8dp 띠). 행 앞 아이콘 상자는 정보 없이 시선만 끈다 |
| "캐시" 용어 | 사용자에게는 **"최근 본 차"**로 부른다. 캐시 행이 곧 홈의 최근 목록(`getRecentFlow`)이라 사실과도 맞다 | 0번 9절: 개발자 용어 금지. 2번 홈 IA의 "최근 본 차"와 같은 말 |
| 데이터 지우기 | "최근 본 차 지우기", "저장한 차 모두 지우기" 두 행. 둘 다 `AvDialog(destructive)` 확인 후 실행하고 `LocalAvToast`로 알린다. 되돌리기는 없다 | 확인 다이얼로그로 충분. 되돌리기는 메모리 보관·재삽입 로직이 필요해 비례하지 않음 |
| 저장한 차 전체 삭제 | `SavedCarDao.deleteAll()`(`DELETE FROM saved_cars`) 추가 | 쿼리 추가는 스키마 변경이 아님(엔티티·버전 그대로) |
| 문의하기 | `mailto:` 작성 화면만 연다(`ACTION_SENDTO`). 보내기는 사용자가 메일 앱에서 한다 | 공개 방침·스토어에 이미 있는 연락처. 앱이 직접 보내지 않음 |
| 토스트 위치 | `AppRoot`가 스택 top 기준으로 하단 여백을 정한다. 설정은 배너(50dp) 위로 띄운다 | 경로→높이가 고정이라 순수 함수로 테스트 가능. 화면별 등록 방식은 전환 중 두 화면이 함께 컴포즈되어 순서 문제가 생김 |
| 개인정보 처리방침 | 앱 안은 **해요체 요약**, 정식 전문은 웹(`https://pouu69.github.io/auto-verdict-aos/`). 요약 맨 아래 "전체 방침 보기" 행 | 문구 가이드(해요체)와 법적 문서(합니다체)를 분리. 요약은 전문과 모순되면 안 됨(지금 화면은 "외부 서버로 전송하지 않아요"라고 해 AdMob 광고 ID 전송과 모순) |
| 오픈소스 목록 생성 | 저장소에 둔 Python 표준 라이브러리 스크립트가 Gradle 의존성 보고서와 `~/.gradle` 캐시의 POM·AAR·JAR을 읽어 `res/raw`에 JSON을 만든다. 결과물은 커밋한다 | 다운로드 없음, 새 Gradle 플러그인 없음(oss-licenses-plugin 금지). zip 읽기에 Python `zipfile`이 가장 간단. 빌드 시 자동 생성은 Gradle 설정만 복잡해짐 |
| 라이선스 전문 위치 | 생성된 JSON 안에 전문을 넣는다. 화면은 `R.raw.oss_notices`, `R.raw.oss_google_notices` 두 리소스만 정적으로 참조 | `isShrinkResources = true`라 `getIdentifier`로 찾는 raw 파일은 잘려 나갈 수 있음 |
| Google SDK 약관 | Play 서비스·AdMob·UMP는 "Android 소프트웨어 개발 키트 라이선스"를 이름과 URL로 표기한다. 약관 전문은 넣지 않고, AAR에 들어 있는 `third_party_licenses.txt`(포함된 오픈소스 고지)는 전부 넣는다 | 개발자용 약관이라 최종 사용자에게 재배포할 의무가 없음. Google이 고지 대상으로 제공하는 것은 `third_party_licenses`이며 캐시에 오프라인으로 있음 |
| 라이선스 상세 | 새 `Route.LicenseDetail(id)` 화면 | 전문이 길어(Kotlin 고지 약 110KB) 시트로는 부적합. 스택에 넣어야 뒤로가기·회전 복원이 자연스럽다 |
| 앱 아이콘 | 토스 파랑 단색 배경 + 흰색 **정면 자동차** 한 덩어리. 그릴 자리에 체크 표시를 **구멍**으로 뚫는다 | 굵은 면 도형이라 작은 크기에서도 읽힘(지금 아이콘은 2.4dp 선이라 약함). 구멍 방식이라 같은 도형을 단색(테마) 레이어로 그대로 쓸 수 있음 |
| 레거시 PNG | 만들지 않는다 | minSdk 26이라 모든 기기가 적응형 아이콘을 쓴다 |
| 스플래시 | 배경 흰색, 아이콘은 런처 전경 + 파랑 아이콘 배경 원 | 0번 7.2절 예고대로. 흰 스플래시 → 흰 홈이라 색이 튀지 않음 |
| 스토어 자산 위치 | 기존 위치를 그대로 쓴다: 문구 `docs/store-listing.md`, 원본 SVG `docs/assets/source/`, 결과 PNG `docs/assets/`. `docs/store/`를 새로 만들지 않는다 | 이미 체크리스트·가이드가 이 경로를 가리킨다. 중복 위치를 만들지 않기 위해 |
| PNG 렌더링 | `rsvg-convert`(이미 설치, 오프라인) + 저장소의 Pretendard를 가리키는 임시 fontconfig | 새 도구 설치 없음 |
| 문자열 파일 | 화면별 파일 `res/values/strings_settings.xml`, `strings_licenses.xml`, `strings_privacy.xml` | 2·3번과 병행 작업 중 `strings.xml` 끝부분 충돌을 피함 |

## 3. 설정 화면

### 3.1 구성 (위→아래)

```
AvTopBar(onBack, "설정")
─ 내 데이터 ───────────────────────────
  최근 본 차 지우기                3대 >
  저장한 차 모두 지우기            5대 >
▒▒▒▒▒▒▒▒ (AvSectionDivider)
─ 앱 정보 ─────────────────────────────
  사용법 다시 보기                     >
  개인정보 처리방침                    >
  오픈소스 라이선스                    >
  문의하기       pouu69@gmail.com      >
  버전                             1.0.2
▒▒▒▒▒▒▒▒ (debug 빌드만)
─ 개발 ────────────────────────────────
  컴포넌트 갤러리                      >
(여백 24dp)
  엔카(encar.com)의 공식 서비스가 아니에요.        ← caption1, textTertiary, 좌우 20dp
  점수와 판정은 참고용이에요.
(LazyColumn 끝)
[ 320x50 배너 + navigationBars ]
```

- 바탕은 `background`(흰색). 지금의 `surfaceMuted` 바탕 + 흰 카드 구조를 없앤다.
- 섹션 제목은 `AvSectionHeader(title)`(heading 시맨틱 포함). 섹션 사이는 `AvSectionDivider`.
- 모든 행은 `AvListRow`. 누를 수 있는 행은 `avPressable`이 자동 적용된다(0번 14절 "행 눌림 반응 통일" 해결). 행 앞 아이콘(`leading`)은 쓰지 않는다.
- 이동 행의 trailing은 `Icons.Rounded.ChevronRight`(20dp, `textTertiary`, 장식이라 `contentDescription = null`). 값 표시는 `body2` `textTertiary`. 값과 화살표가 함께 있으면 값 다음 4dp 간격으로 화살표.
- 하단 배너는 지금처럼 `Box(navigationBarsPadding) { AdBanner() }`(0번 10절 유지).

### 3.2 행별 동작

| 행 | trailing | 누르면 | 비활성 조건 |
|---|---|---|---|
| 최근 본 차 지우기 | "{n}대" | 확인 다이얼로그 | n = 0이면 trailing "없음", `onClick = null`(누를 수 없음, 화살표 없음) |
| 저장한 차 모두 지우기 | "{m}대" | 확인 다이얼로그 | m = 0이면 같은 방식 |
| 사용법 다시 보기 | › | `Route.Onboarding` push(기존) | — |
| 개인정보 처리방침 | › | `Route.PrivacyPolicy` | — |
| 오픈소스 라이선스 | › | `Route.Licenses` | — |
| 문의하기 | 부제 "pouu69@gmail.com", › | `ACTION_SENDTO mailto:` (제목 "[AutoVerdict] 문의", 본문 첫 줄 "앱 버전 {versionName} · Android {release}") | — |
| 버전 | "{versionName}" | 없음 | 항상 누를 수 없음 |
| 컴포넌트 갤러리 | › | `Route.Gallery` | debug 빌드에서만 섹션 노출(기존) |

- n은 `cacheDao().getRecentFlow()`의 크기(만료 제외 = 홈에 보이는 수), m은 `savedCarDao().countFlow()`. 둘 다 Flow로 수집해 지우면 바로 0이 된다(지금의 일회성 `count()` 대체).
- 메일 앱이 없으면(`ActivityNotFoundException`) 토스트 "메일 앱을 찾지 못했어요".
- 다이얼로그 상태(`pendingClear: ClearTarget?`)는 `rememberSaveable`. 회전해도 열린 다이얼로그가 유지된다.
- 지우기는 화면의 코루틴 스코프에서 `withContext(Dispatchers.IO)`로 실행한다. 실패하면 토스트 "지우지 못했어요. 다시 시도해 주세요".

### 3.3 다이얼로그·토스트 문구

| 대상 | 제목 | 본문 | 확인(Danger) | 취소 | 완료 토스트 |
|---|---|---|---|---|---|
| 최근 본 차 | 최근 본 차 기록을 지울까요? | 홈에 보이는 최근 본 차 {n}대가 사라져요. 저장한 차는 그대로예요. | 지우기 | 취소 | 최근 본 차 기록을 지웠어요 |
| 저장한 차 | 저장한 차 {m}대를 모두 지울까요? | 저장 당시 결과도 함께 사라지고 되돌릴 수 없어요. | 모두 지우기 | 취소 | 저장한 차를 모두 지웠어요 |

- 다이얼로그는 `AvDialog(destructive = true)`. M3 `AlertDialog` 직접 사용과 `android.widget.Toast`를 없앤다(0번 14절 해결).
- 저장한 차를 지운 뒤 탭바 배지(`savedCount`)는 `AppRoot`가 같은 Flow를 수집하므로 자동 갱신된다.

### 3.4 토스트와 하단 배너 겹침

지금 `AppRoot.toastOffset`은 탭 루트가 아니면 `navigationBarsPadding + 16dp`라서 설정 화면의 배너(50dp)를 덮는다.

- `toastOffset`을 순수 함수 `toastBottomPadding(top: Route, navBar: Dp, bottomChromeHeight: Dp): Dp`로 바꾼다.
  - 탭 루트: `bottomChromeHeight`(지금과 같음)
  - `Route.Settings`: `navBar + BannerSlotHeight(50dp) + 8dp`
  - 그 외: `navBar + 16dp`(지금과 같음)
- 결과 화면(하단 CTA + 배너)도 같은 문제가 있는지 에뮬레이터에서 확인한다. 겹치면 CTA 높이가 글꼴 크기에 따라 바뀌므로 이 함수에 고정값을 넣지 않고 후속 과제로 기록한다(1번 소관).

### 3.5 코드 구조

- `ui/screen/SettingsScreen.kt` → 상태 없는 `SettingsScreen(state: SettingsUiState, onClearRecent, onClearSaved, onPrivacyPolicy, onLicenses, onReplayGuide, onContact, onGallery: (() -> Unit)?, onBack)`. `SettingsUiState(recentCount: Int, savedCount: Int, versionName: String)`.
- `ui/screen/SettingsRoute.kt`(신규): DB Flow 수집, 다이얼로그 상태, 지우기 실행, 토스트, 메일 인텐트. 1번의 `ResultRoute`/`ResultScreen` 분리와 같은 방식.
- `AppScreenContent`의 `Route.Settings` 분기는 `SettingsRoute(host)`를 호출한다.
- 남은 `MaterialTheme.typography`, `FontWeight.Medium`, 원시 `RoundedCornerShape`, `Icons.Outlined`를 모두 없앤다. 문구는 `strings_settings.xml`.

## 4. 개인정보 처리방침 화면

- 구성: `AvTopBar(onBack, "개인정보 처리방침")` → 결론 문장(`title2`) "분석한 차 정보는 내 기기에만 저장돼요" → `caption1` "최종 업데이트 {날짜}" → 섹션들 → `AvSectionDivider` → `AvListRow("전체 방침 보기", 부제 "웹에서 열려요", ›)` → navigationBars 여백.
- 섹션은 제목(`body1` SemiBold 대신 `title3`, heading 시맨틱) + 본문(`body2`, `textSecondary`) 단락. 번호 배지·아이콘 상자·카드는 없앤다.
- 섹션과 내용(공개 방침 `docs/privacy-policy.md`와 같은 사실, 해요체 요약):

| 섹션 | 요약 |
|---|---|
| 처리하는 정보 | 입력·공유한 엔카 매물 링크, 그 매물의 공개 정보(차량 상태·이력·가격). 이름·연락처·위치 등은 받지 않아요 |
| 저장 위치와 기간 | 기기 안 데이터베이스에만 저장. 최근 본 차는 24시간 뒤 자동 삭제, 저장한 차는 지울 때까지 보관. 자체 서버가 없어요 |
| 지우는 방법 | 설정 → 최근 본 차 지우기 / 저장한 차 모두 지우기, 저장한 차 목록에서 하나씩, 앱 삭제 시 전부 |
| 광고 | Google AdMob이 광고를 보여주기 위해 광고 ID 등 기기 정보를 Google에 보내요. Android 설정 → Google → 광고에서 광고 ID를 삭제하거나 재설정할 수 있어요 |
| 네트워크 | 엔카(encar.com)에서 공개 매물 정보를 가져오고, 광고를 불러와요. 모두 HTTPS |
| 권한 | 인터넷, 광고 ID(`AD_ID`) 두 가지만 써요 |
| 문의 | pouu69@gmail.com |

- "전체 방침 보기"는 `ACTION_VIEW`로 공개 URL을 연다(브라우저가 없으면 토스트 "브라우저 앱을 찾지 못했어요").
- 공개 방침 갱신: `docs/privacy-policy.md`와 `docs/legal/index.html`의 4.3 삭제 항목을 새 설정 문구("최근 본 차 지우기", "저장한 차 모두 지우기")로 고치고 "최종 업데이트"를 구현일로 바꾼다. **gh-pages 게시(공개 콘텐츠 수정)는 사용자가 직접 한다**(후속 과제).
- 날짜 문자열은 `strings_privacy.xml`의 `privacy_updated_at` 하나로 두고, 공개 문서와 같은 날짜를 쓴다.

## 5. 앱 아이콘

### 5.1 구성

| 레이어 | 리소스 | 내용 |
|---|---|---|
| background | `@color/av_accent` (`#3182F6`) | 단색. `ic_launcher_background.xml`(`#3B82F6`, 토큰과 다른 값)은 삭제 |
| foreground | `@drawable/ic_launcher_foreground` (108×108dp 벡터) | 흰색 정면 자동차, 구멍(evenOdd)으로 앞유리·전조등·체크 |
| monochrome | `@drawable/ic_launcher_foreground` (같은 파일) | 구멍이 진짜 투명이라 알파만 쓰는 테마 아이콘에서도 그대로 읽힌다 |

`mipmap-anydpi-v26/ic_launcher.xml`과 `ic_launcher_round.xml`은 둘 다:

```xml
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/av_accent" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

`<monochrome>`는 API 33 미만에서 무시된다(compileSdk 36이라 aapt가 받아들임).

### 5.2 전경 도형 (viewport 108×108, 중심 (54,54), 안전 영역 지름 66 = 반지름 33)

모든 path는 `android:fillColor="#FFFFFF"`. 내용 경계 상자는 x 26~82, y 34~75(가로 56, 세로 41, 세로 중심 54.5). 가장 바깥 점(바퀴 아래 모서리)까지 중심에서 약 30.2dp라 안전 영역(33) 안이다.

| 부분 | path (`fillType`) | 설명 |
|---|---|---|
| 지붕(캐빈) | `M35,51 L39.6,37.2 Q40.5,34 43.8,34 H64.2 Q67.5,34 68.4,37.2 L73,51 Z` + 앞유리 구멍 `M42.5,37.5 H65.5 L69.25,47.5 H38.75 Z` (`evenOdd`) | 위 26dp·아래 38dp 사다리꼴, 윗모서리만 둥글게. 아래 끝(y 51)은 차체와 1dp 겹쳐 이음매가 보이지 않게 한다. 앞유리 위 띠 3.5dp, 아래 기둥 2.5dp |
| 차체 + 전조등 + 체크 | 차체 `M32,50 H76 A6,6 0 0 1 82,56 V64 A6,6 0 0 1 76,70 H32 A6,6 0 0 1 26,64 V56 A6,6 0 0 1 32,50 Z` / 왼쪽 전조등 `M32.5,55 H35.5 A2.5,2.5 0 0 1 35.5,60 H32.5 A2.5,2.5 0 0 1 32.5,55 Z` / 오른쪽 전조등 `M72.5,55 H75.5 A2.5,2.5 0 0 1 75.5,60 H72.5 A2.5,2.5 0 0 1 72.5,55 Z` / 체크 `M47.56,57.94 L51.50,61.89 L60.94,52.44 A2.2,2.2 0 0 1 64.06,55.56 L51.50,68.11 L44.44,61.06 A2.2,2.2 0 0 1 47.56,57.94 Z` — 한 path에 이어 쓰고 `evenOdd` | 56×20dp 둥근 사각형(반경 6). 전조등은 8×5dp 알약 구멍. 체크는 (46,59.5)→(51.5,65)→(62.5,54) 꺾은선을 두께 4.4dp, 둥근 끝, 마이터 이음으로 바꾼 다각형 구멍(이미 계산된 좌표). 체크와 차체 위·아래 가장자리 간격은 약 1.8dp |
| 왼쪽 바퀴 | `M33.5,68 H38.5 A2.5,2.5 0 0 1 41,70.5 V72.5 A2.5,2.5 0 0 1 38.5,75 H33.5 A2.5,2.5 0 0 1 31,72.5 V70.5 A2.5,2.5 0 0 1 33.5,68 Z` | 10×7dp, 차체와 2dp 겹침 |
| 오른쪽 바퀴 | 위 path를 x +36 (`M69.5,68 H74.5 … 67,72.5 V70.5 … Z`) | 대칭 |

- 체크는 판정("이 차 괜찮은지 확인했다")을, 정면 자동차는 대상을 뜻한다. 색은 흰색 하나, 배경은 토스 파랑 하나뿐이라 0번의 "강조색 하나" 원칙과 맞는다.
- 설계 시 위 좌표를 SVG로 옮겨 `rsvg-convert`로 렌더링해 정면 자동차 + 그릴 위치 체크로 읽히는지, 안전 영역 원 안에 들어가는지 확인했다(저장소 밖 임시 파일).
- 구현자는 같은 path를 `docs/assets/source/app-icon.svg`(viewBox 0 0 108 108, 배경 rect 포함)로 옮겨 `rsvg-convert -w 48`, `-w 192`로 렌더링해 확인한다. 작은 크기에서 체크가 뭉치면 체크 두께를 4.0~4.8 사이에서, 전조등을 ±1dp 범위에서 조정해도 된다. 조정 값은 이 절에 기록한다.
- 런처 마스크(원·둥근 사각형·물방울)와 테마 아이콘은 에뮬레이터에서 확인한다(10절).

### 5.3 스플래시

`themes.xml`의 `Theme.AutoVerdict.Splash`:
- 부모를 `Theme.SplashScreen.IconBackground`로 바꾸고
- `windowSplashScreenBackground` = `@color/av_background`(흰색)
- `windowSplashScreenAnimatedIcon` = `@drawable/ic_launcher_foreground`
- `windowSplashScreenIconBackgroundColor` = `@color/av_accent`

흰 화면 가운데 파란 원 안에 흰 자동차가 뜬다. 주석("existing launcher foreground is white …")을 새 이유로 바꾼다.

## 6. 스토어 자산 (저장소 안에서만 만든다, 업로드 없음)

### 6.1 산출물

| 산출물 | 원본 | 결과 | 만드는 법 |
|---|---|---|---|
| 앱 아이콘 원본 | `docs/assets/source/app-icon.svg` (108 viewBox) | — | 5.2절 path 그대로 |
| Play 아이콘 512 | `docs/assets/source/play-icon-512.svg` (교체) | `docs/assets/play-icon-512.png` | 512 정사각형 꽉 찬 `#3182F6`(모서리 둥글림 없음 — Play가 마스크를 씌움) + 전경을 `scale(4.74074)`. `rsvg-convert -w 512 -h 512` |
| 피처 그래픽 | `docs/assets/source/feature-graphic-1024x500.svg` (교체) | `docs/assets/feature-graphic-1024x500.png` | 6.3절 |
| 스크린샷 틀 | `docs/assets/source/screenshot-frame.svg` (신규, 캡션·이미지 경로만 바꿔 8장) | `docs/assets/screenshots/play/01~08.png` | 6.4절 |
| 원본 캡처 | — | `docs/assets/screenshots/raw/01~08.png` | 에뮬레이터 캡처(10절 QA와 같은 회차) |
| 문구 | `docs/store-listing.md` (갱신) | — | 6.2절 |

- 기존 `docs/assets/screenshots/01~08.png`(옛 디자인)는 `raw/`로 대체하며 삭제한다. git 이력에 남는다.
- 렌더링 글꼴: `FONTCONFIG_FILE`로 저장소 `app/src/main/res/font`만 가리키는 임시 설정을 쓴다. SVG의 `font-family="Pretendard"`, 굵기 400/600/700만 쓴다. 명령은 `docs/assets-guide.md`에 적는다.

```bash
cat > /tmp/av-fonts.conf <<EOF
<?xml version="1.0"?><!DOCTYPE fontconfig SYSTEM "fonts.dtd">
<fontconfig><dir>$(pwd)/app/src/main/res/font</dir><cachedir>/tmp/av-fontcache</cachedir></fontconfig>
EOF
FONTCONFIG_FILE=/tmp/av-fonts.conf rsvg-convert -w 1024 -h 500 \
  docs/assets/source/feature-graphic-1024x500.svg -o docs/assets/feature-graphic-1024x500.png
```

### 6.2 스토어 문구 (`docs/store-listing.md` 갱신)

- 앱 이름은 그대로("AutoVerdict — 중고차 매물 평가", '엔카' 상표 제외 원칙 유지).
- **짧은 설명(80자 이내, 52자)**: `엔카 매물 링크 하나로 사고·보험·성능점검 이력을 12가지 기준으로 점검하고 결론부터 알려줘요`
  - 기존 문구의 '엔카'는 호환 대상 설명이라 유지한다.
- **긴 설명**: 아래 골격을 해요체로 쓴다. 기존 설명의 사실과 다른 항목("주행거리 대비 연식", "정비 기록", "가중치", "빨강·주황·초록")은 뺀다.

```
중고차, 사기 전에 결론부터 확인하세요.

AutoVerdict는 엔카(encar.com) 매물 링크를 넣으면 보험이력·성능점검·엔카진단을
모아 12가지 기준으로 점검하고, "사기 전에 2가지는 꼭 확인하세요"처럼
결론을 한 문장으로 알려줘요.

◆ 이렇게 써요
· 브라우저로 연 엔카 매물에서 [공유]를 누르고 AutoVerdict 선택
· 또는 앱에 매물 링크를 붙여넣기

◆ 12가지 기준
보험이력 공개 · 성능점검 공개 · 엔카진단 · 프레임 손상 · 렌트·택시·영업용 이력 ·
전손·침수·도난 · 소유자 변경 · 자차보험 공백 · 수리비 미확정 · 보험 처리 금액 ·
가격 적정성 · 오일 누유

◆ 결과 화면
· 판정(피하세요·주의·괜찮아요·확인 필요)과 점수
· 사기 전에 확인할 것과 그 이유
· 사고·소유자 변경·보험 공백 이력 타임라인
· 교환·수리 부위와 주요 장치 상태
· 판매자에게 물어볼 질문(복사·공유)
· 항목마다 판단 근거 원본

◆ 저장하고 비교하기
마음에 드는 차를 저장하고, 두 대를 나란히 비교할 수 있어요.

◆ 내 정보는 내 기기에만
· 분석한 차 정보는 기기 안에만 저장돼요. 자체 서버가 없어요
· 위치·연락처·사진 권한을 쓰지 않아요
· 광고(Google AdMob)를 위해 광고 ID가 Google에 전달돼요

◆ 알아두세요
엔카(encar.com)의 공식 서비스가 아니며 SK엔카닷컴과 제휴 관계가 없어요.
점수와 판정은 공개 정보를 바탕으로 한 참고용이고, 차량 상태를 보증하지 않아요.
```

- **출시 노트(다음 버전)**: "새 디자인으로 바뀌었어요 / 결과를 결론 한 문장과 확인할 것 중심으로 정리했어요 / 이력 타임라인, 교환·수리 부위, 판매자 질문을 추가했어요 / 새 앱 아이콘". 버전 번호 올리기는 릴리스 작업이라 범위 밖.
- 스크린샷 가이드·피처 그래픽 설명 절은 6.3·6.4절 내용으로 바꾼다. 데이터 안전성·광고·연락처 절은 그대로 둔다.

### 6.3 피처 그래픽 (1024×500) 콘셉트

```
┌──────────────────────────────────────────────────────────────┐
│ #3182F6 단색                                                  │
│  [아이콘 72px]                       ┌──────────────────────┐ │
│                                      │ (주의) 72점          │ │
│  중고차, 사기 전에                    │ 사기 전에 2가지는    │ │
│  결론부터 확인하세요                  │ 꼭 확인하세요        │ │
│                                      │ ─────────────────── │ │
│  매물 링크 하나로 12가지 기준 점검     │ ● 소유자 변경 5회    │ │
│                                      │ ● 자차보험 공백 7개월│ │
│                                      └──────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

- 배경 `#3182F6` 단색(그라데이션·장식 원·그림자 없음).
- 왼쪽(x 72부터): 아이콘(흰 둥근 사각형 72×72, 반경 18 안에 파란 전경 — 파란 바탕 위라 색을 뒤집는다), 제목 2줄 Pretendard Bold 52px 흰색 자간 -1.5, 행간 66px(y 230, 296), 부제 Pretendard Regular 24px 흰색 85% 불투명(y 352).
- 오른쪽 카드: x 604, y 96, 348×308, 흰색, 반경 24. 안쪽 여백 28. `AvBadge` 모양 "주의"(`#FFF4E5` 배경·`#9A5000` 글자, 15px SemiBold, 알약) + "72점"(28px Bold `#9A5000`), 결론 2줄(26px Bold `#191F28`), 1px `#E5E8EB` 선, 행 2개(점 8px `#FE9800` + 17px Regular `#333D4B`).
- 텍스트는 가장자리 40px 안쪽. 이미지 안에 '엔카' 글자는 넣지 않는다.

### 6.4 스크린샷 (휴대전화 8장)

- 틀: 1080×1920(9:16). 바탕 `#F2F4F6`(`surfaceMuted`). 위 캡션 영역 높이 360: 캡션 2줄 이내 Pretendard Bold 60px `#191F28` 가운데 정렬(y 170, 246). 아래에 원본 캡처(1080×2400)를 폭 900으로 축소(높이 2000)해 x 90, y 400에 반경 48 모서리로 놓고 틀 밖은 잘린다(캡처 상단 약 1520px가 보임 — 상태바와 상단 내용이 핵심이 되게 촬영).
- 촬영: `AV_API_36`(1080×2400), 상태바 데모 모드(`adb shell settings put global sysui_demo_allowed 1` 후 시계 9:00·배터리 100%), 광고는 debug 빌드의 테스트 광고가 보이지 않게 광고 슬롯이 비어 있는 상태로 촬영(광고 로드 전 캡처 또는 비행기 모드로 저장한 차 화면).

| # | 화면·상태 | 캡션 |
|---|---|---|
| 1 | 결과 Ready 상단(차 이름·판정 배지·점수·결론 문장·개수 칩), CAUTION 매물 | 결론부터 한 문장으로 알려줘요 |
| 2 | 결과 "확인할 것" 카드 2~3개 | 사기 전에 꼭 확인할 것만 골라요 |
| 3 | 상세 시트(보험 공백 또는 소유자 변경 근거) | 판단 근거는 원본 그대로 보여줘요 |
| 4 | 이력 타임라인 + 교환·수리 부위 | 사고·소유자 변경·보험 공백을 한눈에 |
| 5 | 판매자에게 물어볼 것 | 판매자에게 물어볼 질문까지 챙겨요 |
| 6 | 홈(링크 입력 + 최근 본 차) — 2번 디자인 | 링크 하나면 바로 분석해요 |
| 7 | 브라우저 공유 시트에서 AutoVerdict 선택(또는 온보딩 공유 안내) | 엔카에서 공유하면 바로 열려요 |
| 8 | 비교 화면 — 3번 디자인 | 저장하고 두 대를 나란히 비교해요 |

- 6·8은 2·3번 병합 뒤에만 찍을 수 있다. **스크린샷 작업(T8)은 2·3번이 `main`에 들어온 뒤로 미룬다.** 그 전에 4번을 병합해도 되며, 그때 스토어 PNG는 기존 파일을 유지한다.
- 공개 매물을 찍으므로 차량번호·VIN·딜러 연락처가 보이는 화면(엔카 웹 페이지)은 쓰지 않는다. 결과 화면에는 원래 없다.

## 7. 오픈소스 고지

### 7.1 대상 (2026-10-18, f73fdbd 기준 실측)

`./gradlew --offline -q :app:dependencies --configuration releaseRuntimeClasspath`(캐시만으로 성공 확인)로 해석된 아티팩트는 BOM을 빼고 **129개, 43개 그룹**이다. 캐시 POM의 `<license>` 기준:

| 라이선스(POM 표기) | 아티팩트 | 비고 |
|---|---|---|
| Apache 2.0 (표기 4종: "The Apache Software License, Version 2.0", "Apache-2.0", "The Apache License, Version 2.0", "Apache 2.0") | 114 | AndroidX 전부, Kotlin·kotlinx, jsr305, error_prone, j2objc, jetbrains annotations, jspecify |
| 없음 → 부모 POM `guava-parent` = Apache 2.0 | 3 | guava, failureaccess, listenablefuture |
| MIT | 1 | `org.checkerframework:checker-qual:3.12.0` (전문은 JAR의 `META-INF/LICENSE.txt`) |
| Android Software Development Kit License | 11 | `com.google.android.gms:*` 10개 + `com.google.android.ump:user-messaging-platform` |

그 밖의 대상:

| 대상 | 라이선스 | 전문 출처(오프라인) |
|---|---|---|
| Pretendard (번들 폰트) | SIL OFL 1.1 | 기존 `res/raw/license_pretendard_ofl.txt` |
| Play 서비스·UMP AAR에 포함된 구성요소 | 구성요소별(Apache 2.0, MIT, BSD 등) | 각 AAR의 `third_party_licenses.json` + `.txt`. 10개 AAR 합집합은 이름 71개, 서로 다른 전문 31개(약 450KB, 압축 약 74KB) |
| WebView 엔진 번들(`engine.js`) | 고지 대상 없음 | `package.json`에 런타임 의존성이 없다(devDependencies만). vendored core는 같은 저자의 코드. Vite/esbuild가 넣는 짧은 변환 도우미는 라이브러리 포함이 아니라 고지하지 않는다 |
| Material 아이콘 | Apache 2.0 | `material-icons-extended`가 위 Apache 그룹에 포함 |

Apache 2.0 전문은 기존 `res/raw/license_apache_2_0.txt`(AndroidX JAR의 `LICENSE.txt`와 같은 내용)를 쓴다.

### 7.2 생성 스크립트

위치: `scripts/oss-notices/`

| 파일 | 내용 |
|---|---|
| `generate.py` | 진입점. `python3 -I scripts/oss-notices/generate.py --deps <의존성 보고서 경로>` |
| `groups.json` | 그룹 묶기·표시 이름 표(아래) |
| `extra.json` | Maven 밖 항목(Pretendard) |
| `texts/apache-2.0.txt`, `texts/ofl-1.1-pretendard.txt` | 원본 전문(지금 `res/raw`에서 옮김) |
| `README.md` | 재생성 절차 3줄(이 스펙 7.2절 요약) — 스크립트 사용법이라 문서 금지 원칙의 예외 |

절차:
1. `./gradlew --offline -q :app:dependencies --configuration releaseRuntimeClasspath > /tmp/deps.txt`
2. `generate.py`가 트리에서 `g:a:v`(`->` 뒤 해석 버전 우선, `(c)` 제약 줄과 BOM 제외)를 모은다.
3. 아티팩트마다 `~/.gradle/caches/modules-2/files-2.1/{g}/{a}/{v}/*/{a}-{v}.pom`을 읽어 `<licenses>`를 얻고, 없으면 `<parent>` POM을 따라 올라간다(최대 5단계).
4. 라이선스 이름을 정규화한다: "Apache" 포함 → `apache-2.0`, "MIT" → `mit`, "Android Software Development Kit License" → `google-android-sdk`. **알 수 없는 이름이나 POM이 캐시에 없는 아티팩트가 하나라도 있으면 목록을 출력하고 종료 코드 1**(조용히 빠뜨리지 않음). 처리는 `groups.json`에 수동 지정을 추가하는 것.
5. `mit` 전문은 해당 JAR/AAR 안의 `LICENSE*` 파일에서 읽는다(저작권 줄이 들어 있어 아티팩트별로 보관).
6. `com.google.android.gms`·`com.google.android.ump` AAR의 `third_party_licenses.json`(이름 → `start`/`length`)과 `.txt`를 읽어 이름별 전문을 모으고, 같은 이름이 여러 AAR에 있으면 가장 긴 전문을 쓴다. 전문은 SHA-1로 중복 제거한다.
7. 출력(정렬·들여쓰기 고정, 재실행 시 diff 없음):
   - `app/src/main/res/raw/oss_notices.json`
   - `app/src/main/res/raw/oss_google_notices.json`
8. 기존 `res/raw/license_apache_2_0.txt`, `license_pretendard_ofl.txt`는 삭제한다(전문이 JSON 안으로 들어감).

`groups.json`의 묶음(그룹 ID 접두사 → 표시 이름). 목록 행 하나가 묶음 하나다.

| 표시 이름 | 그룹 |
|---|---|
| Jetpack Compose | `androidx.compose.*` |
| AndroidX Lifecycle | `androidx.lifecycle` |
| AndroidX Room · SQLite | `androidx.room`, `androidx.sqlite` |
| AndroidX Activity · Fragment · SavedState | `androidx.activity`, `androidx.fragment`, `androidx.savedstate` |
| AndroidX Core | `androidx.core`, `androidx.annotation`, `androidx.collection`, `androidx.arch.core`, `androidx.concurrent`, `androidx.versionedparcelable`, `androidx.tracing`, `androidx.startup`, `androidx.profileinstaller`, `androidx.emoji2`, `androidx.graphics`, `androidx.autofill`, `androidx.customview`, `androidx.interpolator`, `androidx.loader`, `androidx.viewpager` |
| AndroidX WebKit · Browser | `androidx.webkit`, `androidx.browser` |
| AndroidX WorkManager | `androidx.work` |
| AndroidX Privacy Sandbox Ads | `androidx.privacysandbox.ads` |
| Kotlin 표준 라이브러리 | `org.jetbrains.kotlin` |
| kotlinx.coroutines · kotlinx.serialization | `org.jetbrains.kotlinx` |
| Guava | `com.google.guava`, `com.google.j2objc`, `com.google.errorprone`, `com.google.code.findbugs` |
| JetBrains Annotations · JSpecify | `org.jetbrains`, `org.jspecify` |
| Checker Framework | `org.checkerframework` (MIT) |
| Google 모바일 광고 SDK · Play 서비스 | `com.google.android.gms`, `com.google.android.ump` (`google-android-sdk`) |
| Pretendard | `extra.json` (OFL 1.1) |

묶음 하나 안에 라이선스가 둘 이상 섞이면 스크립트가 실패한다(묶음을 나누도록). 새 그룹이 표에 없으면 그룹 ID를 이름으로 쓰고 경고를 출력한다(실패는 아님).

### 7.3 데이터 형식

`oss_notices.json`:

```json
{
  "schemaVersion": 1,
  "source": "releaseRuntimeClasspath",
  "licenses": [
    { "id": "apache-2.0", "name": "Apache License 2.0", "text": "…전문…", "url": null },
    { "id": "mit-checker-qual", "name": "MIT License", "text": "…저작권 줄 포함 전문…", "url": null },
    { "id": "ofl-1.1", "name": "SIL Open Font License 1.1", "text": "…", "url": null },
    { "id": "google-android-sdk", "name": "Android 소프트웨어 개발 키트 라이선스", "text": null,
      "url": "https://developer.android.com/studio/terms.html" }
  ],
  "libraries": [
    { "id": "jetpack-compose", "name": "Jetpack Compose", "licenseId": "apache-2.0",
      "artifacts": ["androidx.compose.ui:ui:1.7.6", "…"] }
  ]
}
```

`oss_google_notices.json`: `{ "schemaVersion": 1, "texts": { "<sha1 앞 10자>": "…" }, "components": [ { "name": "Animal Sniffer", "textId": "…" } ] }`.

- `text`와 `url` 중 하나는 반드시 있다. `id`에는 `|`를 쓰지 않는다(라우트 인코딩).
- Kotlin 모델은 `licenses/OssNotices.kt`(`@Serializable`, `Json { ignoreUnknownKeys = true }`), 로더는 `licenses/OssNoticeRepository.kt`(IO 디스패처에서 raw 읽기, 앱 범위 lazy 캐시). `proguard-rules.pro`에 `com.car.autoverdict.licenses.**`에 대한 serializer 유지 규칙을 `report.**`와 같은 형태로 추가한다.

### 7.4 화면

**목록 `LicensesScreen`**

```
AvTopBar(onBack, "오픈소스 라이선스")
  AutoVerdict는 아래 오픈소스 소프트웨어를 사용해요.      ← body2, textSecondary, 20dp 여백
─ Apache License 2.0  12 ──────────────────
  Jetpack Compose               34개 모듈 >
  AndroidX Lifecycle            20개 모듈 >
  …
▒▒▒▒
─ MIT License  1 ──────────────────────────
  Checker Framework              1개 모듈 >
▒▒▒▒
─ SIL Open Font License 1.1  1 ────────────
  Pretendard                       폰트   >
▒▒▒▒
─ Google 약관  1 ───────────────────────────
  Google 모바일 광고 SDK · Play 서비스 11개 모듈 >
▒▒▒▒
─ Google Play 서비스에 포함된 오픈소스  71 ∨ ─   ← AvSectionHeader(onToggle), 기본 접힘
  Animal Sniffer                          >
  …
```

- 섹션 = 라이선스(라이브러리 수 내림차순, Google 두 섹션은 맨 아래). 행 = 묶음(이름 가나다·알파벳순). trailing은 "{n}개 모듈" + ›.
- Google 포함 구성요소 섹션은 71행이라 기본 접힘(`AvSectionHeader(expanded, onToggle)` + `AvCollapsibleBody`, 상태는 `rememberSaveable`).
- 로딩 중(JSON 파싱, 수십 ms)은 빈 화면, 실패하면 `AvEmptyState`("라이선스 정보를 불러오지 못했어요"). 실패는 리소스가 깨진 경우뿐이라 재시도 버튼은 두지 않는다.
- 라이선스 이름·라이브러리 이름은 고유명사라 영문 그대로 쓴다(문구 가이드의 영문 금지 예외, 이 화면 한정).

**상세 `LicenseDetailScreen`** — `Route.LicenseDetail(id)`. `id`는 묶음이면 `lib:<library id>`, Google 포함 구성요소면 `google:<이름>`.

```
AvTopBar(onBack, title = 묶음 이름)
  Apache License 2.0                         ← title3
  androidx.compose.ui:ui:1.7.6               ← caption1, textTertiary, 아티팩트 목록(최대 전부)
  …
  [웹에서 보기]                              ← url만 있는 라이선스일 때 AvButton(Weak, S), ACTION_VIEW
▒▒▒▒
  (전문)                                      ← caption1, textSecondary
```

- 전문은 빈 줄 기준 문단으로 나눠 `LazyColumn` 항목으로 그린다(Kotlin 고지 약 110KB를 `Text` 하나로 그리면 첫 프레임이 느림). `SelectionContainer`로 감싸 복사할 수 있게 한다.
- 알 수 없는 `id`(데이터가 바뀐 뒤 복원 등)는 `AvEmptyState` + 뒤로.
- `Route`·`RouteCodec`에 `LicenseDetail` 추가(`"license|<id>"`), `AppScreenContent`에 분기 추가. `AppNavigator` 규칙은 바뀌지 않는다(일반 push/pop).

### 7.5 의존성이 바뀔 때

- `docs/release-guide.md`와 `docs/play-store-checklist.md`의 릴리스 전 단계에 "의존성을 바꿨으면 7.2절 절차로 고지를 다시 생성" 한 줄을 추가한다.
- 자동 감시는 하지 않는다. 대신 Kotlin 테스트가 `build.gradle.kts`의 `implementation(...)` 좌표(정규식으로 읽음, BOM 관리 좌표는 그룹:이름만)가 모두 `oss_notices.json` 아티팩트에 들어 있는지 확인한다. 직접 의존성 누락만 잡는 가벼운 장치다.

## 8. 함께 하는 정리

| 항목 | 내용 |
|---|---|
| 설정·방침·라이선스 화면 | M3 `AlertDialog`·`android.widget.Toast`·`MaterialTheme.typography`·`FontWeight.Medium`·원시 `RoundedCornerShape`/`sp`·`Icons.Outlined` 제거, 문구를 화면별 strings 파일로 |
| 문자열 | `settings_licenses_subtitle`("폰트·아이콘 라이선스 고지"), `settings_gallery_subtitle`("debug 빌드 전용"), `license_pretendard_notice`, `license_material_notice`, `license_show_full`, `license_hide_full` 삭제(새 화면에서 안 씀). `strings.xml`의 M3 시트 문구 재정의(`close_sheet`, `m3c_bottom_sheet_*`)는 코드 참조가 없지만 **의도된 라이브러리 덮어쓰기라 유지** |
| 리소스 | `drawable/ic_launcher_background.xml`, `raw/license_apache_2_0.txt`, `raw/license_pretendard_ofl.txt` 삭제(각각 5.1·7.2절) |
| 주석 | `LicensesScreen`의 "Full OSS list is sub-project 4", `themes.xml` 스플래시 주석, `colors.xml` 주석 갱신 |
| 문서 | `DESIGN.md`: 앱 아이콘 절(색·도형·monochrome), 문자열 파일 규칙("`res/values/strings*.xml`"), 라이선스 화면은 영문 고유명사 허용. `CLAUDE.md` Key Conventions: OSS 고지 재생성 한 줄. `docs/assets-guide.md`: Image Asset Studio·레거시 PNG 안내를 6.1절 rsvg 절차로 교체. 0번 스펙 14절의 4번 항목에 "해결(4번)" 표시 |

**범위 밖(남긴다)**

- `AnalyzeScreen`·`OnboardingScreen`·`SavedListScreen`·`CompareScreen`의 M3 컴포넌트·`FontWeight.Medium`·하드코딩 문구 — 2·3번 소관, 병행 작업과 충돌 방지.
- 앱 전체 접근성 감사 — 4번은 손대는 화면(설정·방침·라이선스·아이콘)만 마감한다. 다른 화면은 각 하위 프로젝트에서.
- 결과 화면 토스트 위치(3.4절에서 겹치면 후속 과제로 기록).
- gh-pages의 공개 방침 게시, Play Console 업로드, 버전 번호 올리기 — 사용자 작업.
- Play 서비스 SDK 약관 전문 표시(7.1절 결정).
- 다크 테마, 앱 평가하기(스토어 링크) 행.

## 9. 문구 목록 (strings_settings / strings_licenses / strings_privacy)

| 키 | 값 |
|---|---|
| `settings_title` | 설정 |
| `settings_section_data` | 내 데이터 |
| `settings_clear_recent` | 최근 본 차 지우기 |
| `settings_clear_saved` | 저장한 차 모두 지우기 |
| `settings_count_cars` | %1$d대 |
| `settings_count_none` | 없음 |
| `settings_section_app` | 앱 정보 |
| `settings_replay_guide` | 사용법 다시 보기 |
| `settings_privacy` | 개인정보 처리방침 |
| `settings_licenses` | 오픈소스 라이선스 |
| `settings_contact` | 문의하기 |
| `settings_contact_email` | pouu69@gmail.com |
| `settings_contact_subject` | [AutoVerdict] 문의 |
| `settings_contact_body` | 앱 버전 %1$s · Android %2$s |
| `settings_version` | 버전 |
| `settings_section_dev` | 개발 (기존) |
| `settings_gallery_title` | 컴포넌트 갤러리 (기존) |
| `settings_disclaimer` | 엔카(encar.com)의 공식 서비스가 아니에요. 점수와 판정은 참고용이에요. |
| `settings_clear_recent_title` / `_message` / `_confirm` / `_done` | 3.3절 표 |
| `settings_clear_saved_title` / `_message` / `_confirm` / `_done` | 3.3절 표 |
| `settings_cancel` | 취소 |
| `settings_clear_failed` | 지우지 못했어요. 다시 시도해 주세요 |
| `settings_no_mail_app` | 메일 앱을 찾지 못했어요 |
| `licenses_title` | 오픈소스 라이선스 (기존 키 이동) |
| `licenses_intro` | AutoVerdict는 아래 오픈소스 소프트웨어를 사용해요. |
| `licenses_module_count` | %1$d개 모듈 |
| `licenses_font` | 폰트 |
| `licenses_section_google_terms` | Google 약관 |
| `licenses_section_google_bundled` | Google Play 서비스에 포함된 오픈소스 |
| `licenses_open_web` | 웹에서 보기 |
| `licenses_load_failed` | 라이선스 정보를 불러오지 못했어요 |
| `privacy_title` | 개인정보 처리방침 |
| `privacy_headline` | 분석한 차 정보는 내 기기에만 저장돼요 |
| `privacy_updated_at` | 최종 업데이트 {구현일, 공개 문서와 같은 날짜} |
| `privacy_section_*` / `privacy_body_*` | 4절 표 7개 섹션 |
| `privacy_open_full` / `privacy_open_full_sub` | 전체 방침 보기 / 웹에서 열려요 |
| `common_no_browser` | 브라우저 앱을 찾지 못했어요 |

## 10. 검증 (비례적으로)

| 대상 | 방법 |
|---|---|
| 설정 화면 | Compose UI 테스트(Robolectric) 1개 파일: 개수 표시와 0일 때 누를 수 없음, 갤러리 행은 콜백이 있을 때만, 다이얼로그 확인 시 콜백 호출(상태 없는 `SettingsScreen`에 가짜 상태 주입) |
| 토스트 위치 | `toastBottomPadding` 순수 함수 JUnit 3케이스(탭 루트·설정·그 외) |
| DAO | `AppDatabaseTest`에 `deleteAll` 1케이스 |
| 라우트 | `RouteCodecTest` 왕복 목록에 `LicenseDetail` 추가, `|`가 든 id는 쓰지 않음을 생성 데이터 테스트에서 확인 |
| 고지 데이터 | Robolectric로 실제 `R.raw.oss_notices`·`oss_google_notices` 파싱: 모든 `licenseId`가 존재, 각 라이선스에 `text` 또는 `url`, id 중복 없음, 대표 항목(Compose, `play-services-ads`, Pretendard, checker-qual MIT) 포함, 7.5절 직접 의존성 포함 확인 |
| 생성 스크립트 | 실행해서 결과를 커밋하고, 한 번 더 실행해 `git diff`가 없음을 확인(결정적 출력). 별도 테스트 코드는 두지 않는다 |
| 기존 | `./gradlew :app:testDebugUnitTest`, `npx vitest run` 전부 통과(vitest는 바뀌는 게 없어 회귀 확인용) |
| 에뮬레이터 1회 | `AV_API_36`, `-no-snapshot`(사용자 에뮬레이터는 건드리지 않음). 확인: ① 설정 두 지우기 흐름(다이얼로그·토스트가 배너 위에 뜨는지·탭바 배지 갱신) ② 문의하기 메일 인텐트 ③ 방침 화면과 "전체 방침 보기" ④ 라이선스 목록·접힘 섹션·Kotlin 고지 상세 스크롤 ⑤ 런처 아이콘(원·둥근 사각형 마스크) ⑥ 테마 아이콘 켜기(설정 → 배경화면 및 스타일) ⑦ 흰 스플래시 ⑧ 글꼴 크기 최대에서 설정 행 ⑨ 결과 화면 저장 토스트 위치(3.4절) |
| 크기 | release AAB 크기 변화 기록(고지 JSON 약 0.5MB, 압축 후 약 0.1MB 증가 예상) |
| 스크린샷 | 2·3번 병합 뒤 별도 회차(T8) |

## 11. 리스크

| 리스크 | 대응 |
|---|---|
| 캐시에 없는 POM(새 의존성을 아직 받지 않은 환경) | 스크립트가 실패하고 목록을 출력. 그 환경에서 한 번 빌드(의존성 해석)한 뒤 다시 실행 |
| 고지가 의존성 변경을 따라가지 못함 | 7.5절 체크리스트 + 직접 의존성 테스트 |
| 아이콘 체크가 작은 크기에서 뭉침 | 5.2절 조정 범위 안에서 수정, 값 기록 |
| `windowSplashScreenIconBackgroundColor`가 API 31 미만에서 다르게 보임 | core-splashscreen이 아이콘 배경 원을 직접 그림. API 26 에뮬레이터가 없으면 API 36에서만 확인하고 기록 |
| 2·3번과 `AppScreenContent`·`Route`·`strings` 충돌 | 화면별 strings 파일, `Route`에는 한 줄 추가뿐. 병합 시 수동 해결 |
| 앱 안 방침 요약과 공개 방침 불일치 | 같은 커밋에서 두 문서를 함께 고치고 4절 표를 기준으로 대조 |

## 12. 작업 분해

| 작업 | 파일 | 의존 |
|---|---|---|
| **T1 고지 생성** | `scripts/oss-notices/{generate.py, groups.json, extra.json, texts/*, README.md}`, `res/raw/oss_notices.json`·`oss_google_notices.json`(생성), `res/raw/license_*.txt` 삭제 | 없음 |
| **T2 라이선스 화면** | `licenses/OssNotices.kt`, `licenses/OssNoticeRepository.kt`, `ui/screen/LicensesScreen.kt`(교체), `ui/screen/LicenseDetailScreen.kt`(신규), `ui/navigation/{Route, RouteCodec, AppScreenContent}.kt`, `proguard-rules.pro`, `res/values/strings_licenses.xml`, `strings.xml`(옛 라이선스 키 삭제), 테스트(`OssNoticesTest`, `RouteCodecTest`) | T1(형식은 7.3절로 고정이라 T1과 동시 착수 가능, 테스트는 T1 결과 필요) |
| **T3 설정** | `ui/screen/SettingsScreen.kt`(교체), `ui/screen/SettingsRoute.kt`(신규), `db/SavedCarDao.kt`(`deleteAll`), `ui/navigation/{AppRoot, AppScreenContent}.kt`, `res/values/strings_settings.xml`, `strings.xml`(옛 설정 키 삭제), 테스트(`SettingsScreenTest`, `AppRootTest` 또는 신규 `ToastPaddingTest`, `AppDatabaseTest`) | 없음 |
| **T4 개인정보 처리방침** | `ui/screen/PrivacyPolicyScreen.kt`(교체), `res/values/strings_privacy.xml`, `docs/privacy-policy.md`, `docs/legal/index.html` | 없음 |
| **T5 앱 아이콘·스플래시** | `res/drawable/ic_launcher_foreground.xml`(교체), `res/drawable/ic_launcher_background.xml`(삭제), `res/mipmap-anydpi-v26/ic_launcher{,_round}.xml`, `res/values/{themes, colors}.xml`, `docs/assets/source/app-icon.svg`(신규), `docs/assets/source/play-icon-512.svg`, `docs/assets/play-icon-512.png` | 없음 |
| **T6 스토어 문구·그래픽** | `docs/store-listing.md`, `docs/assets/source/feature-graphic-1024x500.svg`, `docs/assets/feature-graphic-1024x500.png`, `docs/assets/source/screenshot-frame.svg`(신규), `docs/assets-guide.md` | T5(아이콘 도형) |
| **T7 문서 마감** | `DESIGN.md`, `CLAUDE.md`, `docs/release-guide.md`, `docs/play-store-checklist.md`, 0번 스펙 14절 표시, 이 문서의 구현 기록(아이콘 조정 값, AAB 크기) | T1~T6 |
| **T8 QA·스크린샷** | 10절 에뮬레이터 1회(T1~T7 뒤). 스크린샷 `docs/assets/screenshots/{raw,play}/01~08.png`, 옛 `screenshots/0x.png` 삭제는 2·3번 병합 뒤 | T1~T7, (스크린샷은) 2·3번 |

**병렬 묶음**

- 1차(동시): T1, T3, T4, T5 — 서로 겹치는 파일 없음(T3만 `AppScreenContent`·`AppRoot`를 건드림).
- 2차(동시): T2(T1 뒤, `AppScreenContent`는 T3와 같은 파일이라 T3 병합 뒤 리베이스), T6(T5 뒤).
- 3차: T7 → T8(에뮬레이터 QA). 스크린샷 부분은 2·3번 병합 뒤 별도 회차.

각 작업이 끝날 때 `./gradlew :app:testDebugUnitTest`가 통과해야 한다(T4·T6은 문서·리소스 위주라 빌드만 확인).

## 13. 구현 기록 (2026-10-18)

**만든 것**: 상태 없는 `SettingsScreen` + `SettingsRoute`(최근 본 차 지우기·저장한 차 모두 지우기, `AvDialog` 확인, `LocalAvToast`, mailto 문의), 순수 함수 `toastBottomPadding`(설정에서는 배너 위), 해요체 요약 개인정보 처리방침 화면, 새 적응형 아이콘·테마 아이콘·흰 스플래시, 오픈소스 고지 생성 스크립트(`scripts/oss-notices/`)와 목록·상세(`Route.LicenseDetail`) 화면, 스토어 문구·피처 그래픽·스크린샷 틀.

**결정·변경**
- **아이콘**: 5.2절 path를 조정 없이 그대로 썼다(체크 두께 4.4, 전조등 8×5). `docs/assets/source/app-icon.svg`와 `play-icon-512` 원본·PNG를 새로 만들었다.
- **고지 수량**: Maven 아티팩트 127개(설계 시 129개 추정) + Pretendard, 묶음 15개. 라이선스는 Apache 2.0·MIT(checker-qual)·OFL 1.1·Android SDK 라이선스(URL만). Play 서비스·UMP AAR에 포함된 구성요소는 71개, 서로 다른 전문 23개다.
- **개인정보 내용 바로잡기**: "외부 서버로 전송하지 않아요" 같은 문구를 지웠다. AdMob이 광고 ID·앱 세트 ID·IP·기기/앱 정보·광고 상호작용을 Google에 보내고, 엔카는 기기에서 직접 요청(매물 ID·차량번호·접속 정보)을 받는다는 사실을 앱 요약·`docs/privacy-policy.md`·`docs/legal/index.html`·스토어 데이터 안전성 답변에 맞췄다(7df1ffc, 3759fb5). 최종 업데이트는 2026년 10월 18일.
- **IP 주소 = 대략적인 위치(사용자 결정)**: Play 데이터 안전성에서 IP 주소를 "위치 > 대략적인 위치"로 신고한다. 수집·공유(Google, AdMob SDK) 예, 일시적 처리 아니오, 필수, 용도는 광고 또는 마케팅·분석·사기 예방/보안/규정 준수(Google Mobile Ads SDK Play 데이터 공개 안내 기준). `docs/store-listing.md` 표, 체크리스트, Play Console 안내, 방침 문서, 앱 요약에 반영했다.
- **사기 예방 용도를 네 행 모두에(사용자 결정)**: 데이터 안전성의 나머지 세 행(기기 또는 기타 ID, 앱 활동 > 앱 상호작용, 앱 정보 및 성능 > 진단)에도 "사기 예방·보안·규정 준수"를 더해, 네 행 모두 용도가 광고 또는 마케팅·분석·사기 예방/보안/규정 준수로 같다. AdMob 안내가 SDK가 보내는 모든 데이터의 목적을 "광고, 분석, 사기 예방"으로 적기 때문이다. `docs/store-listing.md` 표(제출 전 재확인 메모 삭제), 체크리스트, Play Console 안내에 반영했다. 방침 문서는 Play 데이터 유형별 용도를 나열하지 않아 바꾸지 않았다.
- **렌더링**: macOS에서는 `PANGOCAIRO_BACKEND=fc`를 함께 줘야 `FONTCONFIG_FILE`이 적용된다. 빠지면 Pretendard 대신 대체 글꼴로 한글이 렌더링된다. 6.1절 명령에도 이 변수가 필요하다(`docs/assets-guide.md`에 반영).
- **문자열**: 브라우저 없음 토스트는 `strings.xml`의 `common_no_browser` 하나를 방침 화면과 라이선스 상세("웹에서 보기")가 함께 쓴다. 라이선스 상세는 처음에 로그만 남겼으나 문서 마감 때 토스트로 고쳤다.
- 최근 본 차 개수는 `getRecentFlow` 대신 2번의 `recentViewItems()`를, 저장한 차 개수는 3번의 `SavedCarsRepository.itemCount`를 쓴다(홈 목록·저장 탭과 같은 수).
- 앱 이름은 `docs/store-listing.md`의 "AutoVerdict — 중고차 매물 평가"로 체크리스트·Play Console 안내를 맞췄다. 릴리스 문서에 고지 재생성 단계를 넣었다(7.5절).

**후속 과제**
- **광고 동의(UMP) 흐름 미구현.** UMP SDK는 `play-services-ads`를 통해 함께 들어오지만 동의 양식을 띄우는 코드가 없다. EEA·영국에 배포하기 전에 구현해야 한다.
- 스토어 스크린샷(T8): 2·3번이 들어온 새 디자인으로 다시 찍어야 한다. 지금 `docs/assets/screenshots/`는 옛 화면이다.
- gh-pages 공개 방침 게시, Play Console 업로드, 버전 올리기는 사용자가 직접 한다.
- 결과 화면 토스트와 하단 CTA·배너 겹침 여부(3.4절), release AAB 크기 변화는 아직 기록하지 않았다.
