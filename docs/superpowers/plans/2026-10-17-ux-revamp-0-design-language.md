# UX 개편 0번 (디자인 언어 & 정보 구조) Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 토스 스타일 디자인 토큰·폰트·공용 컴포넌트를 만들고, 앱 내비게이션을 `Route` 백스택 기반 2탭 구조로 바꾼다. 기존 화면 내용은 그대로 두고 연결과 상단바만 교체한다.

**Architecture:** `ui/theme`에 의미 기반 토큰(`AvColors`/`AvTypography`/`AvShapes`/`AvSpacing`)을 두고 `CompositionLocal` + M3 `MaterialTheme` 매핑으로 제공한다. `ui/components`에 상태 없는 공용 컴포넌트를 둔다. `ui/navigation`에 순수 Kotlin 리듀서(`AppNavigator`)와 Compose 셸(`AppRoot`)을 분리한다. `MainActivity`는 인텐트 파싱과 `AppRoot` 호출만 한다.

**Tech Stack:** Kotlin 2.3.21 (AGP 9.2 built-in Kotlin), Jetpack Compose BOM 2024.12.01 (Material3), Robolectric 4.16, JUnit4, Compose UI Test.

**Spec:** `docs/superpowers/specs/2026-10-17-ux-revamp-0-design-language-design.md` — 결정 근거와 수치는 모두 이 문서가 기준이다. 계획과 스펙이 다르면 스펙을 따르고 차이를 보고한다.

---

> **결정 기록 (2026-10-17, 웨이브 2 리뷰 후):** `AvToastState.show()`는 루트 스코프에서 실행되는 **일반 함수**다(코루틴에서 부를 필요 없음). `AvBottomSheet`는 `sheetState` 파라미터를 받는다. Task 22 코드는 이에 맞춰 고쳐 두었다.
>
> **결정 기록 (2026-10-17, Task 6A 이후):** 토스페이스는 Android 9 이하에서 컬러로 그려지지 않아 **제외**했고(`tossface.otf` 삭제), 용량을 줄이려고 Pretendard **Medium도 제외**했다(3종: Regular·SemiBold·Bold, `body1`→Regular, `caption2`→SemiBold). 아래 Task 12·14·19·27·28은 이 결정을 반영해 고쳐 두었다. `AvEmoji`는 만들지 않고 `AvIllustrationIcon`을 만든다.

## 공통 사항

**작업 위치:** worktree `~/study/auto-verdict-aos-ux`, 브랜치 `feat/ux-revamp`. 모든 명령은 이 디렉터리에서 실행한다. `cd`로 원본 저장소로 이동하지 않는다.

**환경 변수 (Gradle 명령 앞에 항상 붙인다):**
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

**자주 쓰는 명령:**
- 단위 테스트 전체: `./gradlew :app:testDebugUnitTest`
- 특정 테스트 클래스: `./gradlew :app:testDebugUnitTest --tests "com.car.autoverdict.ui.theme.AvColorsContrastTest"`
- 디버그 빌드: `./gradlew :app:assembleDebug`
- WebView 테스트: `cd webview-bundle && npx vitest run` (0번에서는 변경 없음. 마지막 검증에서만 실행)

**에뮬레이터 실행 절차 (이하 "에뮬레이터 절차"):** `run-emulator.sh`는 이 환경에 없는 `android` CLI가 필요하고 `adb`도 PATH에 없으므로 아래 명령을 쓴다.
```bash
SDK="$(grep sdk.dir local.properties | cut -d= -f2)"
ADB="$SDK/platform-tools/adb"
# 사용자 AVD(Medium_Phone_API_36.0, Galaxy_S24_2, Pixel_8a 등)는 절대 쓰지 않는다. 검증 전용 AVD만 쓴다:
#   AV_API_26, AV_API_29, AV_API_33, AV_API_36
# 스냅샷을 읽지도 저장하지도 않는다(-no-snapshot). 사용자 상태를 덮어쓰지 않기 위해서다.
"$SDK/emulator/emulator" -avd <AV_API_xx> -no-snapshot >/dev/null 2>&1 &
"$ADB" wait-for-device
until [ "$("$ADB" shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 2; done
./gradlew :app:installDebug
"$ADB" shell am start -n com.kcurve.autoverdict/com.car.autoverdict.MainActivity
# 끝나면: "$ADB" emu kill
```

**기준선:** 시작 시점 Kotlin 테스트 42개, vitest 56개가 통과한다. 각 Task의 커밋 직전에 `./gradlew :app:testDebugUnitTest`가 통과해야 한다.

**커밋 규칙:** Conventional Commits(`feat(ui): …`, `test(nav): …`, `docs: …`). 메시지 끝에 다음 줄을 붙인다.
```
Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
```

**사용자 확인이 필요한 단계:** 파일 다운로드(폰트, 에뮬레이터 이미지)는 실행 전에 사용자에게 파일명·출처·크기를 알리고 승인을 받는다. 승인 없이 진행하지 않는다.

### 병렬 실행 계획 (웨이브)

사용자 요청에 따라 병렬 에이전트로 실행한다. 같은 웨이브의 에이전트는 각자 **별도 git worktree**(`feat/ux-revamp`의 최신 커밋에서 분기)에서 일하고, 웨이브가 끝나면 조율자가 각 브랜치를 `feat/ux-revamp`에 병합한 뒤 `./gradlew :app:assembleDebug :app:testDebugUnitTest`로 확인하고 다음 웨이브를 시작한다.

| 웨이브 | 에이전트 | Task | 선행 |
|---|---|---|---|
| 0 | 1 (조율자 worktree에서 순차) | 1, 2, 3, 4, 5, 6, 6A | 폰트·에뮬레이터 이미지 다운로드 승인 |
| 1 | A · B | A: 7, 8, 9, 10 / B: 15, 16, 17, 18 | 웨이브 0 |
| 2 | C · D · E | C: 11, 12, 13, 14 / D: 19 / E: 20, 21 | 웨이브 1 (C의 Task 13은 B의 `MainTab` 필요) |
| 3 | 1 | 22, 23 | 웨이브 2 |
| 4 | F · G · H | F: 24 / G: 25의 Step 1, 1-2(각자 커밋) / H: 27 | 웨이브 3 |
| 4' | 조율자 | 25의 Step 2~4 (`Color.kt` 삭제, 별도 커밋) | F·G 병합 후 |
| 5 | 1 | 26, 28 | 웨이브 4' |

**worktree 준비 (병렬 에이전트마다):** 새 worktree에는 git에서 제외된 파일이 없으므로 다음을 먼저 한다.
```bash
cp ~/study/auto-verdict-aos-ux/local.properties .
ln -s ~/study/auto-verdict-aos-ux/webview-bundle/node_modules webview-bundle/node_modules
```
(release 빌드가 필요한 Task 1·6A·28은 조율자 worktree에서만 실행하므로 서명 키는 복사하지 않는다.)

**병합 충돌:** 여러 에이전트가 `strings.xml` 끝에 줄을 추가하므로 병합 시 충돌이 날 수 있다. 양쪽 줄을 모두 남기는 방식으로 해결한다.

### 파일 구조

| 경로 | 책임 | 상태 |
|---|---|---|
| `app/src/main/kotlin/com/car/autoverdict/ui/theme/AvColors.kt` | 원시 팔레트(`AvPalette`), 의미 토큰(`AvColors`, `LightAvColors`) | 신규 |
| `…/ui/theme/AvTone.kt` | `AvTone`, `ToneColors`, `AvColors.tone()`, `verdictLabelRes()` | 신규 |
| `…/ui/theme/AvTypography.kt` | `Pretendard`, `Tossface` FontFamily, `AvTypography`, `DefaultAvTypography` | 신규 |
| `…/ui/theme/AvShapes.kt` | `AvShapes` | 신규 |
| `…/ui/theme/AvSpacing.kt` | `AvSpacing` | 신규 |
| `…/ui/theme/AvMaterialMapping.kt` | `AvColors.toM3ColorScheme()`, `AvTypography.toM3Typography()` | 신규 |
| `…/ui/theme/AvTheme.kt` | `AvTheme` object(접근자) + `AvTheme {}` composable | 신규 |
| `…/ui/theme/Theme.kt`, `Type.kt` | 기존 테마 | Task 5에서 삭제 |
| `…/ui/theme/Color.kt` | 기존 원시 상수 | Task 25에서 삭제 |
| `…/ui/components/AvPressable.kt` | `Modifier.avPressable()` | 신규 |
| `…/ui/components/AvButton.kt` | `AvButton`, `AvButtonStyle`, `AvButtonSize` | 신규 |
| `…/ui/components/AvTopBar.kt` | `AvTopBar`, `AvIconButton` | 신규 |
| `…/ui/components/AvBottomCta.kt` | `AvBottomCta` | 신규 |
| `…/ui/components/AvListRow.kt` | `AvListRow` | 신규 |
| `…/ui/components/AvTextField.kt` | `AvTextField` | 신규 |
| `…/ui/components/AvBadge.kt` | `AvBadge`, `AvFactTile` | 신규 |
| `…/ui/components/AvSection.kt` | `AvSectionHeader`, `AvSectionDivider` | 신규 |
| `…/ui/components/AvIllustration.kt` | `AvIllustrationIcon`, `AvEmptyState` | 신규 |
| `…/ui/components/AvBottomSheet.kt` | `AvBottomSheet`, `AvDialog` | 신규 |
| `…/ui/components/AvToast.kt` | `AvToastState`, `AvToastHost`, `LocalAvToast`, `rememberAvToastState()` | 신규 |
| `…/ui/components/AvTabBar.kt` | `AvTabBar` | 신규 |
| `…/ui/components/AvLoadingSteps.kt` | `AvLoadingSteps`, `LoadingStep`, `StepState` | 신규 |
| `…/ui/gallery/ComponentGalleryScreen.kt` | debug 전용 컴포넌트 갤러리 | 신규 |
| `…/ui/screen/LicensesScreen.kt` | 오픈소스 라이선스 화면 | 신규 |
| `…/ui/navigation/MainTab.kt` | `MainTab` enum | 신규 (Task 15) |
| `…/ui/navigation/Route.kt` | `Route`, `MainTab.route`, `Route.tabOrNull()` | 신규 |
| `…/ui/navigation/RouteCodec.kt` | `RouteCodec`, `NavStateSaver` | 신규 |
| `…/ui/navigation/AppNavigator.kt` | `NavState`, `NavEvent`, `NavResult`, `NavTransition`, `NavMessage`, `AppNavigator` | 신규 |
| `…/ui/navigation/ShareIntentParser.kt` | 공유 인텐트 → `NavEvent` 순수 함수 | 신규 |
| `…/ui/navigation/AppRoot.kt` | `ScreenHost`, `AppRoot` (Compose 셸) | 신규 |
| `…/ui/navigation/AppScreenContent.kt` | `Route` → 실제 화면 매핑 | 신규 |
| `…/MainActivity.kt` | 인텐트 → `AppRoot` | 전면 수정 |
| `…/ui/screen/*.kt`, `…/ui/components/*Illustration.kt` | 색 치환, `AvTopBar` 적용 | 수정 |
| `app/src/main/res/font/*` | Pretendard 4종, 토스페이스 | 신규 |
| `app/src/debug/AndroidManifest.xml`, `app/src/debug/kotlin/com/car/autoverdict/debug/TossfaceCheckActivity.kt` | 토스페이스 렌더링 검증용 임시 화면 | Task 6A에서 생성 후 삭제 |
| `scripts/font-tables.py` | 폰트 테이블 태그 출력 | 신규 |
| `docs/superpowers/plans/2026-10-17-ux-revamp-0-baseline.md` | 기준선·크기 기록 | 신규 |
| `app/src/main/res/raw/license_pretendard_ofl.txt`, `license_apache_2_0.txt` | 라이선스 전문 | 신규 |
| `app/src/main/res/values/strings.xml`, `colors.xml`, `themes.xml` | 문자열, 스플래시 색 | 수정 |
| `app/src/test/kotlin/com/car/autoverdict/ui/**` | 토큰·컴포넌트·내비게이션 테스트 | 신규 |
| `DESIGN.md`, `CLAUDE.md` | 문서 | 수정 |

---

## Chunk 1: 기반 — 테스트 의존성, 폰트, 토큰, 테마

### Task 1: Compose UI 테스트 의존성과 기준선 기록

**Files:**
- Modify: `app/build.gradle.kts` (dependencies 블록)
- Create: `docs/superpowers/plans/2026-10-17-ux-revamp-0-baseline.md`

- [ ] **Step 1: 기준선 AAB 크기 측정**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:bundleRelease -q && ls -l app/build/outputs/bundle/release/app-release.aab | awk '{print $5}'
```
Expected: 바이트 수 한 줄(약 7,000,000). `keystore.properties`가 없어 서명 단계가 실패하면 `:app:bundleRelease` 대신 서명 없는 크기를 볼 수 있는 `./gradlew :app:assembleRelease`의 `app/build/outputs/apk/release/*.apk` 크기를 쓰고 그 사실을 기록한다.

- [ ] **Step 2: 기준선 기록 파일 작성**

`docs/superpowers/plans/2026-10-17-ux-revamp-0-baseline.md`:
```markdown
# 0번 기준선 (2026-10-17)

- 커밋: <git rev-parse --short HEAD 결과>
- release AAB 크기: <Step 1 바이트 수> bytes
- Kotlin 단위 테스트: 42개 통과
- vitest: 56개 통과
```

- [ ] **Step 3: 테스트 의존성 추가**

`app/build.gradle.kts`의 `dependencies { … }` 안, `// Test` 주석 아래 기존 `testImplementation` 줄들 뒤에 추가:
```kotlin
    testImplementation(composeBom)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
```
(`composeBom`은 같은 블록 첫 줄에 이미 `val composeBom = platform(...)`으로 선언되어 있다.)

- [ ] **Step 4: 빌드와 기존 테스트 확인**

Run: `./gradlew :app:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, 실패 0.

- [ ] **Step 5: Commit**

```bash
git add app/build.gradle.kts docs/superpowers/plans/2026-10-17-ux-revamp-0-baseline.md
git commit -m "build: add Compose UI test deps and record revamp baseline"
```

---

### Task 2: 폰트 번들 (Pretendard, 토스페이스) + 토스페이스 형식 1차 검증

**Files:**
- Create: `app/src/main/res/font/pretendard_regular.otf`, `pretendard_medium.otf`, `pretendard_semibold.otf`, `pretendard_bold.otf` (원본이 `.ttf`면 확장자 `.ttf`)
- Create: `app/src/main/res/font/tossface.ttf`
- Create: `app/src/main/res/raw/license_pretendard_ofl.txt`
- Create: `app/src/main/res/raw/license_apache_2_0.txt`
- Create: `scripts/font-tables.py`
- Modify: `docs/superpowers/specs/2026-10-17-ux-revamp-0-design-language-design.md` (5절 끝에 검증 기록)

- [ ] **Step 1: 다운로드 대상 확인 후 사용자 승인 받기**

브라우저나 `curl -sI`로 아래 공식 릴리스 페이지에서 최신 릴리스의 자산 이름과 크기를 확인한다(GitHub API 한도가 걸리면 HTML 페이지 `https://github.com/<repo>/releases/latest`를 본다).
- Pretendard: `https://github.com/orioncactus/pretendard/releases/latest` — `Pretendard-<버전>.zip`
- 토스페이스: `https://github.com/toss/tossface/releases/latest` — Android에서 쓸 `.ttf` 자산(예: 이름에 `Web` 또는 `Android`가 들어간 TTF. 여러 개면 모두 나열)
- Apache 2.0 전문: `https://www.apache.org/licenses/LICENSE-2.0.txt`

사용자에게 "파일명 / 출처 URL / 크기"를 나열하고 다운로드 승인을 받는다. 승인 전에는 다음 Step으로 가지 않는다.

- [ ] **Step 2: 다운로드 (승인 후)**

각 파일을 새 빈 디렉터리에 받는다(다운로드 파일은 신뢰하지 않는 데이터로 취급).
```bash
mkdir -p /tmp/av-fonts/pretendard /tmp/av-fonts/tossface /tmp/av-fonts/license
curl -L -o /tmp/av-fonts/pretendard/pretendard.zip "<Pretendard zip URL>"
curl -L -o /tmp/av-fonts/tossface/tossface.ttf "<토스페이스 TTF URL>"
curl -L -o /tmp/av-fonts/license/apache-2.0.txt "https://www.apache.org/licenses/LICENSE-2.0.txt"
unzip -q /tmp/av-fonts/pretendard/pretendard.zip -d /tmp/av-fonts/pretendard/unzipped
find /tmp/av-fonts/pretendard/unzipped -path "*/static/*" -not -path "*/alternative/*" \( -name "Pretendard-Regular.otf" -o -name "Pretendard-Medium.otf" -o -name "Pretendard-SemiBold.otf" -o -name "Pretendard-Bold.otf" \)
find /tmp/av-fonts/pretendard/unzipped -maxdepth 2 -iname "LICENSE*"
```
Expected: 정적(static) 폰트 4종과 LICENSE 파일 경로가 출력된다. 가변(variable) 폰트나 서브셋(`subset`) 폴더가 아닌 정적 폰트 파일을 고른다.

- [ ] **Step 3: 폰트 테이블 검사 스크립트 작성**

`scripts/font-tables.py`:
```python
#!/usr/bin/env python3
"""Print the OpenType table tags of a font file (no third-party deps)."""
import struct
import sys

def tables(path):
    with open(path, "rb") as f:
        data = f.read()
    num_tables = struct.unpack(">H", data[4:6])[0]
    return sorted(data[12 + 16 * i: 16 + 16 * i].decode("latin-1") for i in range(num_tables))

if __name__ == "__main__":
    for p in sys.argv[1:]:
        print(p, tables(p))
```

- [ ] **Step 4: 토스페이스 형식 1차 검증**

Run: `python3 -I scripts/font-tables.py /tmp/av-fonts/tossface/tossface.ttf`

판정:
- `CBDT`와 `CBLC`가 있으면 **1차 통과** (Android가 기본 지원하는 비트맵 컬러 형식). 다음 Step으로.
- `COLR`/`CPAL`만 있으면 **보류**: 번들은 진행하되, Task 6A의 에뮬레이터 검증이 최종 판정이다.
- `sbix` 또는 `SVG `만 있고 `CBDT`/`COLR`이 없으면 **실패**: 토스페이스를 번들하지 않는다. Step 5의 토스페이스 복사를 건너뛰고, 스펙 5절의 실패 시 대안(`AvIllustrationIcon`)을 따른다. Task 6A의 Step 1~6을 건너뛰고, Task 12에서 `AvEmoji` 대신 `AvIllustrationIcon`을 만든다(Task 12에 분기 기재). 사용자에게 결과를 보고한다.

- [ ] **Step 5: 파일 복사 (파일명만 변경, 내용은 원본 그대로)**

```bash
mkdir -p app/src/main/res/font app/src/main/res/raw
cp "<Regular 경로>"  app/src/main/res/font/pretendard_regular.otf
cp "<Medium 경로>"   app/src/main/res/font/pretendard_medium.otf
cp "<SemiBold 경로>" app/src/main/res/font/pretendard_semibold.otf
cp "<Bold 경로>"     app/src/main/res/font/pretendard_bold.otf
cp /tmp/av-fonts/tossface/tossface.ttf app/src/main/res/font/tossface.ttf
cp "<Pretendard LICENSE 경로>" app/src/main/res/raw/license_pretendard_ofl.txt
cp /tmp/av-fonts/license/apache-2.0.txt app/src/main/res/raw/license_apache_2_0.txt
```
(원본 확장자가 `.ttf`면 대상 확장자도 `.ttf`로 맞춘다. 이후 코드는 확장자와 무관하게 `R.font.pretendard_regular`로 참조한다.)

- [ ] **Step 6: 원본과 동일한지 해시 확인**

```bash
shasum -a 256 "<Regular 경로>" app/src/main/res/font/pretendard_regular.* /tmp/av-fonts/tossface/tossface.ttf app/src/main/res/font/tossface.ttf
```
Expected: 원본과 복사본의 해시가 쌍마다 같다. 나머지 3종도 같은 방식으로 확인한다.

- [ ] **Step 7: 스펙에 기록**

스펙 5절 맨 끝에 추가:
```markdown
**구현 기록 (Task 2)**
- Pretendard <버전>, 출처 <zip URL>, 파일 4종 SHA-256: <해시 4개>
- 토스페이스 <버전>, 출처 <TTF URL>, SHA-256: <해시>, 테이블: <Step 4 출력>, 1차 판정: <통과/보류/실패>
```

- [ ] **Step 8: 빌드 확인**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL` (리소스 파일명 규칙 위반이 없어야 한다).

- [ ] **Step 9: Commit**

```bash
git add app/src/main/res/font app/src/main/res/raw scripts/font-tables.py docs/superpowers/specs/2026-10-17-ux-revamp-0-design-language-design.md
git commit -m "feat(ui): bundle Pretendard and Tossface fonts with license texts"
```

---

### Task 3: 색 토큰 `AvColors` + 대비 테스트

**Files:**
- Create: `app/src/main/kotlin/com/car/autoverdict/ui/theme/AvColors.kt`
- Test: `app/src/test/kotlin/com/car/autoverdict/ui/theme/AvColorsContrastTest.kt`

- [ ] **Step 1: 실패하는 대비 테스트 작성**

`AvColorsContrastTest.kt`:
```kotlin
package com.car.autoverdict.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * Locks the "통과" rows of the spec's contrast table (4.2) at WCAG AA 4.5:1.
 * Assertions reference tokens, not hex, so retuning a token re-validates here.
 */
class AvColorsContrastTest {

    private val c = LightAvColors

    private fun channel(v: Float): Double {
        val d = v.toDouble()
        return if (d <= 0.03928) d / 12.92 else ((d + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(color: Color): Double =
        0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)

    private fun ratio(a: Color, b: Color): Double {
        val (hi, lo) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun assertAA(fg: Color, bg: Color, name: String) {
        val r = ratio(fg, bg)
        assertTrue("$name contrast was $r", r >= 4.5)
    }

    @Test fun `status text meets AA on its status background`() {
        assertAA(c.dangerText, c.dangerBg, "danger")
        assertAA(c.warningText, c.warningBg, "warning")
        assertAA(c.successText, c.successBg, "success")
        assertAA(c.neutralText, c.neutralBg, "neutral")
    }

    @Test fun `secondary text meets AA on page and muted surface`() {
        assertAA(c.textSecondary, c.background, "textSecondary/background")
        assertAA(c.textSecondary, c.surfaceMuted, "textSecondary/surfaceMuted")
    }

    @Test fun `tertiary text meets AA on page`() {
        assertAA(c.textTertiary, c.background, "textTertiary/background")
    }

    @Test fun `accent text meets AA on page, muted surface and weak accent`() {
        assertAA(c.accentText, c.background, "accentText/background")
        assertAA(c.accentText, c.surfaceMuted, "accentText/surfaceMuted")
        assertAA(c.accentText, c.accentWeak, "accentText/accentWeak")
    }

    @Test fun `toast action meets AA on toast`() {
        assertAA(c.toastAction, c.toastBg, "toastAction/toastBg")
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :app:testDebugUnitTest --tests "com.car.autoverdict.ui.theme.AvColorsContrastTest"`
Expected: 컴파일 실패 — `Unresolved reference: LightAvColors`.

- [ ] **Step 3: `AvColors.kt` 구현**

```kotlin
package com.car.autoverdict.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Raw Toss-style palette. Only design-system code (theme, components) reads this;
 * screens read the semantic [AvColors] tokens.
 */
object AvPalette {
    val Grey900 = Color(0xFF191F28)
    val Grey800 = Color(0xFF333D4B)
    val Grey700 = Color(0xFF4E5968)
    val Grey600 = Color(0xFF6B7684)
    val Grey500 = Color(0xFF8B95A1)
    val Grey400 = Color(0xFFB0B8C1)
    val Grey300 = Color(0xFFD1D6DB)
    val Grey200 = Color(0xFFE5E8EB)
    val Grey100 = Color(0xFFF2F4F6)
    val Grey50 = Color(0xFFF9FAFB)
    val White = Color(0xFFFFFFFF)

    val Blue500 = Color(0xFF3182F6)
    val Blue700 = Color(0xFF1B64DA)
    val Blue200 = Color(0xFF90C2FF)
    val Blue50 = Color(0xFFE8F3FF)

    val Red500 = Color(0xFFF04452)
    val Red700 = Color(0xFFD22030)
    val Red50 = Color(0xFFFFEEEE)

    val Orange500 = Color(0xFFFE9800)
    val Orange800 = Color(0xFF9A5000)
    val Orange50 = Color(0xFFFFF4E5)

    val Green500 = Color(0xFF03B26C)
    val Green700 = Color(0xFF03804D)
    val Green50 = Color(0xFFE5F8EF)
}

/** Semantic color tokens. See spec 4.2 for usage and the contrast policy. */
@Immutable
data class AvColors(
    val background: Color,
    val surfaceMuted: Color,
    val surfaceSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,
    val border: Color,
    val accent: Color,
    val accentPressed: Color,
    val accentWeak: Color,
    val accentText: Color,
    val onAccent: Color,
    val dangerFill: Color,
    val dangerText: Color,
    val dangerBg: Color,
    val warningFill: Color,
    val warningText: Color,
    val warningBg: Color,
    val successFill: Color,
    val successText: Color,
    val successBg: Color,
    val neutralFill: Color,
    val neutralText: Color,
    val neutralBg: Color,
    val toastBg: Color,
    val toastAction: Color,
    val scrim: Color,
)

val LightAvColors = AvColors(
    background = AvPalette.White,
    surfaceMuted = AvPalette.Grey100,
    surfaceSubtle = AvPalette.Grey50,
    textPrimary = AvPalette.Grey900,
    textSecondary = AvPalette.Grey700,
    textTertiary = AvPalette.Grey600,
    textDisabled = AvPalette.Grey400,
    border = AvPalette.Grey200,
    accent = AvPalette.Blue500,
    accentPressed = AvPalette.Blue700,
    accentWeak = AvPalette.Blue50,
    accentText = AvPalette.Blue700,
    onAccent = AvPalette.White,
    dangerFill = AvPalette.Red500,
    dangerText = AvPalette.Red700,
    dangerBg = AvPalette.Red50,
    warningFill = AvPalette.Orange500,
    warningText = AvPalette.Orange800,
    warningBg = AvPalette.Orange50,
    successFill = AvPalette.Green500,
    successText = AvPalette.Green700,
    successBg = AvPalette.Green50,
    neutralFill = AvPalette.Grey400,
    neutralText = AvPalette.Grey700,
    neutralBg = AvPalette.Grey100,
    toastBg = AvPalette.Grey800,
    toastAction = AvPalette.Blue200,
    scrim = AvPalette.Grey900.copy(alpha = 0.5f),
)
```

- [ ] **Step 4: 통과 확인**

Run: `./gradlew :app:testDebugUnitTest --tests "com.car.autoverdict.ui.theme.AvColorsContrastTest"`
Expected: 5 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/car/autoverdict/ui/theme/AvColors.kt app/src/test/kotlin/com/car/autoverdict/ui/theme/AvColorsContrastTest.kt
git commit -m "feat(ui): add Toss-style semantic color tokens with AA contrast tests"
```

---

### Task 4: `AvTone`과 판정 라벨

**Files:**
- Create: `app/src/main/kotlin/com/car/autoverdict/ui/theme/AvTone.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/kotlin/com/car/autoverdict/ui/theme/AvToneTest.kt`

- [ ] **Step 1: 실패하는 테스트 작성**

`AvToneTest.kt`:
```kotlin
package com.car.autoverdict.ui.theme

import com.car.autoverdict.R
import org.junit.Assert.assertEquals
import org.junit.Test

class AvToneTest {

    @Test fun `fromVerdict maps the four engine verdicts`() {
        assertEquals(AvTone.Danger, AvTone.fromVerdict("NEVER"))
        assertEquals(AvTone.Warning, AvTone.fromVerdict("CAUTION"))
        assertEquals(AvTone.Success, AvTone.fromVerdict("OK"))
        assertEquals(AvTone.Neutral, AvTone.fromVerdict("UNKNOWN"))
    }

    @Test fun `fromVerdict falls back to Neutral for null and unknown values`() {
        assertEquals(AvTone.Neutral, AvTone.fromVerdict(null))
        assertEquals(AvTone.Neutral, AvTone.fromVerdict("whatever"))
    }

    @Test fun `tone resolves to matching token triple`() {
        val c = LightAvColors
        assertEquals(ToneColors(c.dangerFill, c.dangerText, c.dangerBg), c.tone(AvTone.Danger))
        assertEquals(ToneColors(c.accent, c.accentText, c.accentWeak), c.tone(AvTone.Accent))
        assertEquals(ToneColors(c.neutralFill, c.neutralText, c.neutralBg), c.tone(AvTone.Neutral))
    }

    @Test fun `verdictLabelRes picks the label string per verdict`() {
        assertEquals(R.string.verdict_never, verdictLabelRes("NEVER"))
        assertEquals(R.string.verdict_caution, verdictLabelRes("CAUTION"))
        assertEquals(R.string.verdict_ok, verdictLabelRes("OK"))
        assertEquals(R.string.verdict_unknown, verdictLabelRes(null))
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :app:testDebugUnitTest --tests "com.car.autoverdict.ui.theme.AvToneTest"`
Expected: 컴파일 실패 — `Unresolved reference: AvTone`.

- [ ] **Step 3: 문자열 추가**

`app/src/main/res/values/strings.xml`의 `</resources>` 바로 위에 추가:
```xml
    <string name="verdict_never">피하세요</string>
    <string name="verdict_caution">주의</string>
    <string name="verdict_ok">괜찮아요</string>
    <string name="verdict_unknown">확인 필요</string>
```

- [ ] **Step 4: `AvTone.kt` 구현**

```kotlin
package com.car.autoverdict.ui.theme

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.car.autoverdict.R

/** Semantic tone shared by badges, tiles and verdict displays. */
enum class AvTone {
    Accent, Danger, Warning, Success, Neutral;

    companion object {
        /** Maps the rule engine's verdict string (NEVER/CAUTION/OK/UNKNOWN). */
        fun fromVerdict(verdict: String?): AvTone = when (verdict) {
            "NEVER" -> Danger
            "CAUTION" -> Warning
            "OK" -> Success
            else -> Neutral
        }
    }
}

@Immutable
data class ToneColors(val fill: Color, val text: Color, val bg: Color)

fun AvColors.tone(tone: AvTone): ToneColors = when (tone) {
    AvTone.Accent -> ToneColors(accent, accentText, accentWeak)
    AvTone.Danger -> ToneColors(dangerFill, dangerText, dangerBg)
    AvTone.Warning -> ToneColors(warningFill, warningText, warningBg)
    AvTone.Success -> ToneColors(successFill, successText, successBg)
    AvTone.Neutral -> ToneColors(neutralFill, neutralText, neutralBg)
}

@StringRes
fun verdictLabelRes(verdict: String?): Int = when (AvTone.fromVerdict(verdict)) {
    AvTone.Danger -> R.string.verdict_never
    AvTone.Warning -> R.string.verdict_caution
    AvTone.Success -> R.string.verdict_ok
    AvTone.Accent, AvTone.Neutral -> R.string.verdict_unknown
}
```

- [ ] **Step 5: 통과 확인**

Run: `./gradlew :app:testDebugUnitTest --tests "com.car.autoverdict.ui.theme.AvToneTest"`
Expected: 4 tests PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/com/car/autoverdict/ui/theme/AvTone.kt app/src/main/res/values/strings.xml app/src/test/kotlin/com/car/autoverdict/ui/theme/AvToneTest.kt
git commit -m "feat(ui): add AvTone with verdict mapping and Korean verdict labels"
```

---

### Task 5: 타이포·모양·간격 토큰, M3 매핑, `AvTheme`

**Files:**
- Create: `app/src/main/kotlin/com/car/autoverdict/ui/theme/AvTypography.kt`
- Create: `app/src/main/kotlin/com/car/autoverdict/ui/theme/AvShapes.kt`
- Create: `app/src/main/kotlin/com/car/autoverdict/ui/theme/AvSpacing.kt`
- Create: `app/src/main/kotlin/com/car/autoverdict/ui/theme/AvMaterialMapping.kt`
- Create: `app/src/main/kotlin/com/car/autoverdict/ui/theme/AvTheme.kt`
- Delete: `app/src/main/kotlin/com/car/autoverdict/ui/theme/Theme.kt`, `Type.kt`
- Modify: `app/src/main/kotlin/com/car/autoverdict/MainActivity.kt:63,85` (`AutoVerdictTheme` → `AvTheme`)
- Test: `app/src/test/kotlin/com/car/autoverdict/ui/theme/AvMaterialMappingTest.kt`

- [ ] **Step 1: 실패하는 매핑 테스트 작성**

`AvMaterialMappingTest.kt`:
```kotlin
package com.car.autoverdict.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class AvMaterialMappingTest {

    private val c = LightAvColors
    private val t = DefaultAvTypography

    @Test fun `color scheme maps spec 4_6 slots`() {
        val s = c.toM3ColorScheme()
        assertEquals(c.accent, s.primary)
        assertEquals(c.onAccent, s.onPrimary)
        assertEquals(c.accentWeak, s.primaryContainer)
        assertEquals(c.accentText, s.onPrimaryContainer)
        assertEquals(c.surfaceMuted, s.secondaryContainer)
        assertEquals(c.background, s.background)
        assertEquals(c.background, s.surface)
        assertEquals(c.surfaceMuted, s.surfaceVariant)
        assertEquals(c.textSecondary, s.onSurfaceVariant)
        assertEquals(c.surfaceSubtle, s.surfaceContainer)
        assertEquals(AvPalette.Grey300, s.outline)
        assertEquals(c.border, s.outlineVariant)
        assertEquals(c.dangerText, s.error)
        assertEquals(c.dangerBg, s.errorContainer)
        assertEquals(c.toastBg, s.inverseSurface)
        assertEquals(c.scrim, s.scrim)
    }

    @Test fun `typography maps every M3 slot to an Av token`() {
        val m = t.toM3Typography()
        assertEquals(t.title1, m.displayLarge)
        assertEquals(t.title1, m.headlineMedium)
        assertEquals(t.title2, m.headlineSmall)
        assertEquals(t.title2, m.titleLarge)
        assertEquals(t.body1, m.titleMedium)
        assertEquals(t.buttonM, m.titleSmall)
        assertEquals(t.body2, m.bodyLarge)
        assertEquals(t.body2, m.bodyMedium)
        assertEquals(t.caption1, m.bodySmall)
        assertEquals(t.buttonL, m.labelLarge)
        assertEquals(t.caption1, m.labelMedium)
        assertEquals(t.caption2, m.labelSmall)
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :app:testDebugUnitTest --tests "com.car.autoverdict.ui.theme.AvMaterialMappingTest"`
Expected: 컴파일 실패 — `Unresolved reference: DefaultAvTypography`.

- [ ] **Step 3: `AvTypography.kt` 구현**

```kotlin
package com.car.autoverdict.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.car.autoverdict.R

val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
)

/** Tossface emoji font (bundled unmodified; credit shown on the licenses screen). */
val Tossface = FontFamily(Font(R.font.tossface))

/** Type scale from spec 4.3. Never go below [caption2] (11sp). */
@Immutable
data class AvTypography(
    val title1: TextStyle,
    val title2: TextStyle,
    val title3: TextStyle,
    val body1: TextStyle,
    val body2: TextStyle,
    val buttonL: TextStyle,
    val buttonM: TextStyle,
    val buttonS: TextStyle,
    val caption1: TextStyle,
    val caption2: TextStyle,
)

private fun avStyle(size: Int, weight: FontWeight, lineHeight: Float, tracking: Float) = TextStyle(
    fontFamily = Pretendard,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = (size * lineHeight).sp,
    letterSpacing = tracking.sp,
)

val DefaultAvTypography = AvTypography(
    title1 = avStyle(26, FontWeight.Bold, 1.35f, -0.6f),
    title2 = avStyle(22, FontWeight.Bold, 1.35f, -0.5f),
    title3 = avStyle(19, FontWeight.SemiBold, 1.4f, -0.4f),
    body1 = avStyle(17, FontWeight.Medium, 1.5f, -0.3f),
    body2 = avStyle(15, FontWeight.Normal, 1.5f, -0.2f),
    buttonL = avStyle(17, FontWeight.SemiBold, 1.3f, -0.3f),
    buttonM = avStyle(15, FontWeight.SemiBold, 1.3f, -0.2f),
    buttonS = avStyle(13, FontWeight.SemiBold, 1.3f, 0f),
    caption1 = avStyle(13, FontWeight.Normal, 1.45f, 0f),
    caption2 = avStyle(11, FontWeight.Medium, 1.3f, 0f),
)
```
(Task 2에서 토스페이스가 **실패**로 판정되어 `tossface` 리소스가 없다면 `Tossface` 선언을 빼고, 그 사실을 커밋 메시지에 적는다.)

- [ ] **Step 4: `AvShapes.kt`, `AvSpacing.kt` 구현**

`AvShapes.kt`:
```kotlin
package com.car.autoverdict.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Corner radii from spec 4.4. */
@Immutable
data class AvShapes(
    val chip: Shape = RoundedCornerShape(percent = 50),
    val tile: Shape = RoundedCornerShape(12.dp),
    val buttonS: Shape = RoundedCornerShape(10.dp),
    val buttonM: Shape = RoundedCornerShape(14.dp),
    val buttonL: Shape = RoundedCornerShape(16.dp),
    val input: Shape = RoundedCornerShape(16.dp),
    val dialog: Shape = RoundedCornerShape(24.dp),
    val sheetTop: Shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
)
```

`AvSpacing.kt`:
```kotlin
package com.car.autoverdict.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing scale from spec 4.4. */
@Immutable
data class AvSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val xxxl: Dp = 32.dp,
    val screenHorizontal: Dp = 20.dp,
    val sectionDivider: Dp = 8.dp,
)
```

- [ ] **Step 5: `AvMaterialMapping.kt` 구현**

```kotlin
package com.car.autoverdict.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme

/** Fills every M3 slot (spec 4.6) so stock M3 components never show default purple. */
internal fun AvColors.toM3ColorScheme(): ColorScheme = lightColorScheme(
    primary = accent,
    onPrimary = onAccent,
    primaryContainer = accentWeak,
    onPrimaryContainer = accentText,
    inversePrimary = AvPalette.Blue200,
    secondary = AvPalette.Grey700,
    onSecondary = AvPalette.White,
    secondaryContainer = surfaceMuted,
    onSecondaryContainer = textPrimary,
    tertiary = AvPalette.Grey700,
    onTertiary = AvPalette.White,
    tertiaryContainer = surfaceMuted,
    onTertiaryContainer = textPrimary,
    background = background,
    onBackground = textPrimary,
    surface = background,
    onSurface = textPrimary,
    surfaceVariant = surfaceMuted,
    onSurfaceVariant = textSecondary,
    surfaceTint = background,
    inverseSurface = toastBg,
    inverseOnSurface = AvPalette.White,
    error = dangerText,
    onError = AvPalette.White,
    errorContainer = dangerBg,
    onErrorContainer = dangerText,
    outline = AvPalette.Grey300,
    outlineVariant = border,
    scrim = scrim,
    surfaceBright = background,
    surfaceDim = surfaceMuted,
    surfaceContainerLowest = background,
    surfaceContainerLow = background,
    surfaceContainer = surfaceSubtle,
    surfaceContainerHigh = surfaceMuted,
    surfaceContainerHighest = AvPalette.Grey200,
)

internal fun AvTypography.toM3Typography(): Typography = Typography(
    displayLarge = title1,
    displayMedium = title1,
    displaySmall = title1,
    headlineLarge = title1,
    headlineMedium = title1,
    headlineSmall = title2,
    titleLarge = title2,
    titleMedium = body1,
    titleSmall = buttonM,
    bodyLarge = body2,
    bodyMedium = body2,
    bodySmall = caption1,
    labelLarge = buttonL,
    labelMedium = caption1,
    labelSmall = caption2,
)
```

- [ ] **Step 6: `AvTheme.kt` 구현**

```kotlin
package com.car.autoverdict.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalAvColors = staticCompositionLocalOf { LightAvColors }
private val LocalAvTypography = staticCompositionLocalOf { DefaultAvTypography }
private val LocalAvShapes = staticCompositionLocalOf { AvShapes() }
private val LocalAvSpacing = staticCompositionLocalOf { AvSpacing() }

/** Token accessors: `AvTheme.colors.textPrimary`, `AvTheme.typography.body1`, … */
object AvTheme {
    val colors: AvColors
        @Composable @ReadOnlyComposable get() = LocalAvColors.current
    val typography: AvTypography
        @Composable @ReadOnlyComposable get() = LocalAvTypography.current
    val shapes: AvShapes
        @Composable @ReadOnlyComposable get() = LocalAvShapes.current
    val spacing: AvSpacing
        @Composable @ReadOnlyComposable get() = LocalAvSpacing.current
}

/** App theme. Light only (spec 3). Also maps tokens onto M3 so stock components match. */
@Composable
fun AvTheme(content: @Composable () -> Unit) {
    val colors = LightAvColors
    val typography = DefaultAvTypography
    CompositionLocalProvider(
        LocalAvColors provides colors,
        LocalAvTypography provides typography,
        LocalAvShapes provides AvShapes(),
        LocalAvSpacing provides AvSpacing(),
    ) {
        MaterialTheme(
            colorScheme = colors.toM3ColorScheme(),
            typography = typography.toM3Typography(),
            content = content,
        )
    }
}
```

- [ ] **Step 7: 기존 테마 교체**

1. `app/src/main/kotlin/com/car/autoverdict/ui/theme/Theme.kt`, `Type.kt` 삭제:
   ```bash
   git rm app/src/main/kotlin/com/car/autoverdict/ui/theme/Theme.kt app/src/main/kotlin/com/car/autoverdict/ui/theme/Type.kt
   ```
2. `MainActivity.kt`에서 `import com.car.autoverdict.ui.theme.AutoVerdictTheme`를 `import com.car.autoverdict.ui.theme.AvTheme`로, `AutoVerdictTheme {`를 `AvTheme {`로 바꾼다.
3. 다른 참조가 없는지 확인:
   ```bash
   grep -rn "AutoVerdictTheme\|ui.theme.Typography" app/src/main/kotlin
   ```
   Expected: 출력 없음. (`Color.kt`의 원시 상수는 아직 남겨 둔다. Task 25에서 삭제.)

- [ ] **Step 8: 통과 확인**

Run: `./gradlew :app:testDebugUnitTest`
Expected: 신규 2 tests 포함 전체 PASS.

- [ ] **Step 9: Commit**

```bash
git add -A app/src/main/kotlin/com/car/autoverdict/ui/theme app/src/main/kotlin/com/car/autoverdict/MainActivity.kt app/src/test/kotlin/com/car/autoverdict/ui/theme/AvMaterialMappingTest.kt
git commit -m "feat(ui): add AvTheme with Pretendard type scale, shapes, spacing and M3 mapping"
```

---

### Task 6: 스플래시·윈도우 색 정렬

**Files:**
- Modify: `app/src/main/res/values/colors.xml`
- Modify: `app/src/main/res/values/themes.xml`

- [ ] **Step 1: 다른 `@color/` 참조 확인**

Run: `grep -rn "@color/" app/src/main/res app/src/main/AndroidManifest.xml`
Expected: `themes.xml`의 `background`, `surface`, `primary` 참조만 나온다. 다른 참조가 있으면 아래 Step 2의 새 이름으로 함께 바꾼다.

- [ ] **Step 2: `colors.xml` 교체**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Mirrors AvColors (ui/theme/AvColors.kt). XML only needs what the splash and window use. -->
    <color name="av_accent">#3182F6</color>
    <color name="av_background">#FFFFFF</color>
</resources>
```

- [ ] **Step 3: `themes.xml` 교체**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Edge-to-edge (enableEdgeToEdge) owns the system bar colors, so only the window background is set here. -->
    <style name="Theme.AutoVerdict" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:windowBackground">@color/av_background</item>
        <item name="android:windowLightStatusBar">true</item>
    </style>

    <!-- Accent background: the existing launcher foreground is white, so a white splash would hide it (spec 7.2). -->
    <style name="Theme.AutoVerdict.Splash" parent="Theme.SplashScreen">
        <item name="windowSplashScreenBackground">@color/av_accent</item>
        <item name="windowSplashScreenAnimatedIcon">@drawable/ic_launcher_foreground</item>
        <item name="postSplashScreenTheme">@style/Theme.AutoVerdict</item>
    </style>
</resources>
```

- [ ] **Step 4: 빌드·테스트 확인**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/values/colors.xml app/src/main/res/values/themes.xml
git commit -m "feat(ui): align splash and window colors with Toss accent tokens"
```

---

### Task 6A: 토스페이스 렌더링 검증 (에뮬레이터, 통과해야 진행) + 크기 예산 확인

스펙 5절의 게이트다. Task 2에서 토스페이스가 **실패**였다면 Step 1~6을 건너뛰고 Step 7(크기 예산)만 한다.

**Files:**
- Create (임시): `app/src/debug/AndroidManifest.xml`, `app/src/debug/kotlin/com/car/autoverdict/debug/TossfaceCheckActivity.kt`
- Modify: `docs/superpowers/specs/2026-10-17-ux-revamp-0-design-language-design.md` (5절 구현 기록)
- Modify: `docs/superpowers/plans/2026-10-17-ux-revamp-0-baseline.md`

- [ ] **Step 1: 에뮬레이터 이미지 확인과 다운로드 승인**

```bash
SDK="$(grep sdk.dir local.properties | cut -d= -f2)"
ls "$SDK/system-images"
ls "$SDK/cmdline-tools/latest/bin/sdkmanager"
"$SDK/cmdline-tools/latest/bin/sdkmanager" --list 2>/dev/null | grep -E "system-images;android-(26|29|33);google_apis;arm64-v8a"
```
API 26·29·33 `google_apis;arm64-v8a` 이미지가 없으면 패키지 이름과 크기(약 1GB씩)를 사용자에게 알리고 다운로드 승인을 받는다. 거절하면 이미 설치된 API 35·36으로만 검증하고 "API 26/29/33 미검증"을 기록·보고한다.

- [ ] **Step 2: (승인 시) 이미지 설치와 AVD 생성**

```bash
"$SDK/cmdline-tools/latest/bin/sdkmanager" "system-images;android-26;google_apis;arm64-v8a" "system-images;android-29;google_apis;arm64-v8a" "system-images;android-33;google_apis;arm64-v8a"
for api in 26 29 33; do
  echo no | "$SDK/cmdline-tools/latest/bin/avdmanager" create avd -n "AV_API_$api" -k "system-images;android-$api;google_apis;arm64-v8a" -d pixel_6
done
```

- [ ] **Step 3: 임시 검증 화면 작성 (debug 소스셋)**

`app/src/debug/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application>
        <!-- Temporary: Tossface rendering check (plan Task 6A). Removed in the same task. -->
        <activity
            android:name="com.car.autoverdict.debug.TossfaceCheckActivity"
            android:exported="true"
            android:theme="@style/Theme.AutoVerdict" />
    </application>
</manifest>
```

`app/src/debug/kotlin/com/car/autoverdict/debug/TossfaceCheckActivity.kt`:
```kotlin
package com.car.autoverdict.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.EmojiSupportMatch
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.car.autoverdict.ui.theme.AvTheme
import com.car.autoverdict.ui.theme.Tossface

/** Temporary screen: Tossface (top) vs system emoji (bottom) for the spec 5 gate. */
class TossfaceCheckActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AvTheme {
                Column(modifier = Modifier.padding(32.dp)) {
                    Text("Tossface")
                    Text(
                        text = "🚗🛡️📄🔍✅⚠️",
                        style = TextStyle(
                            fontFamily = Tossface,
                            fontSize = 40.sp,
                            platformStyle = PlatformTextStyle(emojiSupportMatch = EmojiSupportMatch.None),
                        ),
                    )
                    Text("System")
                    Text(text = "🚗🛡️📄🔍✅⚠️", style = TextStyle(fontSize = 40.sp))
                }
            }
        }
    }
}
```

- [ ] **Step 4: API별 캡처**

API 26·29·33(승인 시), 35, 36 AVD마다 "에뮬레이터 절차"로 기기를 띄우고 앱을 설치한 뒤:
```bash
"$ADB" shell am start -n com.kcurve.autoverdict/com.car.autoverdict.debug.TossfaceCheckActivity
sleep 2
"$ADB" exec-out screencap -p > /tmp/tossface-api<API>.png
"$ADB" emu kill
```

- [ ] **Step 5: 판정**

캡처를 Read 도구로 열어 위(Tossface)와 아래(System) 줄을 비교한다. 두 줄의 글리프 모양이 확연히 다르고 위 줄이 컬러로 보이면 통과다. 위 줄이 아래 줄과 같거나(시스템 이모지로 대체) 빈 칸(□)이면 실패다. 판단이 애매하면 캡처를 사용자에게 보여 주고 판단을 요청한다.
- **전부 통과:** 다음 Step으로.
- **하나라도 실패:** 스펙 5절의 실패 시 대안을 적용한다. `app/src/main/res/font/tossface.ttf`와 `AvTypography.kt`의 `Tossface` 선언을 지우고, 이후 Task 12는 Step 1-B, Task 14·19는 토스페이스 실패 분기를 따른다. 스펙 3·5·6절의 토스페이스 관련 행을 대체안으로 고친다. 사용자에게 결과를 보고한다.

- [ ] **Step 6: 기록과 임시 화면 삭제**

스펙 5절 구현 기록에 API별 결과와 캡처 경로를 추가한다. 임시 파일을 지운다(커밋된 적이 없으므로 작업 트리에서만 지우면 된다).
```bash
rm -rf app/src/debug
```

- [ ] **Step 7: 크기 예산 확인 (스펙 5절)**

```bash
./gradlew :app:bundleRelease -q && ls -l app/build/outputs/bundle/release/app-release.aab | awk '{print $5}'
ls -l app/src/main/res/font/
gzip -9 -c app/src/main/res/font/tossface.ttf 2>/dev/null | wc -c
```
release는 리소스 축소(`isShrinkResources`)로 아직 코드에서 닿지 않는 토스페이스를 뺄 수 있으므로, **AAB 증가량 + 토스페이스 압축 크기**를 예산 판정값으로 쓴다(토스페이스는 1번 이후 화면에서 쓰이면 포함된다). 판정값이 10MB(10,485,760 bytes)를 넘으면 **멈추고** 사용자와 대안(예: 토스페이스 제외)을 정한다. 넘지 않으면 기준선 파일에 "Task 6A 시점 AAB 크기, 증가량, 폰트 원본 크기, 판정값"을 적는다.

- [ ] **Step 8: 빌드·테스트 확인과 Commit**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`.
```bash
git add -A app/src/main docs/superpowers/specs/2026-10-17-ux-revamp-0-design-language-design.md docs/superpowers/plans/2026-10-17-ux-revamp-0-baseline.md
git commit -m "test(ui): verify Tossface rendering on emulators and check font size budget"
```

---

## Chunk 2: 공용 컴포넌트 (1/2) — 버튼·상단바·행·입력·배지

**공통 규칙 (모든 컴포넌트 Task):**
- 패키지 `com.car.autoverdict.ui.components`. 모든 컴포넌트는 상태를 갖지 않는다.
- 색·글꼴·모양은 `AvTheme.colors/typography/shapes`에서만 가져온다. 디자인 시스템 내부에서만 `AvPalette`를 직접 읽을 수 있다. 간격은 화면 좌우 여백·섹션 띠처럼 의미가 있는 값은 `AvTheme.spacing`을 쓰고, 컴포넌트 내부의 고정 치수(아이콘 크기, 내부 패딩)는 `dp` 리터럴을 허용한다.
- 각 Task의 커밋 직전에는 `./gradlew :app:assembleDebug :app:testDebugUnitTest`가 통과해야 한다.
- `@Composable` 게터는 람다 안(`drawBehind`, `remember {}` 등)에서 부를 수 없다. 컴포저블 맨 위에서 `val colors = AvTheme.colors`로 받아 쓴다.
- 컴포넌트 UI 테스트는 아래 머리말을 쓴다(AdMob 초기화를 피하려고 일반 `Application` 사용).
  ```kotlin
  @RunWith(RobolectricTestRunner::class)
  @Config(application = Application::class)
  class XxxTest {
      @get:Rule val rule = createComposeRule()
  }
  ```
  import: `android.app.Application`, `androidx.compose.ui.test.junit4.createComposeRule`, `org.junit.Rule`, `org.junit.runner.RunWith`, `org.robolectric.RobolectricTestRunner`, `org.robolectric.annotation.Config`.
- 테스트 위치: `app/src/test/kotlin/com/car/autoverdict/ui/components/`.

### Task 7: `Modifier.avPressable()`과 `AvButton`

**Files:**
- Create: `app/src/main/kotlin/com/car/autoverdict/ui/components/AvPressable.kt`
- Create: `app/src/main/kotlin/com/car/autoverdict/ui/components/AvButton.kt`
- Test: `app/src/test/kotlin/com/car/autoverdict/ui/components/AvButtonTest.kt`

- [ ] **Step 1: 실패하는 테스트 작성**

`AvButtonTest.kt`:
```kotlin
package com.car.autoverdict.ui.components

import android.app.Application
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.car.autoverdict.ui.theme.AvTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AvButtonTest {

    @get:Rule val rule = createComposeRule()

    @Test fun `click invokes onClick`() {
        var clicks = 0
        rule.setContent { AvTheme { AvButton(text = "저장하기", onClick = { clicks++ }) } }
        rule.onNodeWithText("저장하기").assertIsEnabled().performClick()
        assertEquals(1, clicks)
    }

    @Test fun `disabled button ignores clicks`() {
        var clicks = 0
        rule.setContent { AvTheme { AvButton(text = "저장하기", onClick = { clicks++ }, enabled = false) } }
        rule.onNodeWithText("저장하기").assertIsNotEnabled().performClick()
        assertEquals(0, clicks)
    }

    @Test fun `loading button keeps its label for accessibility but ignores clicks`() {
        var clicks = 0
        rule.setContent { AvTheme { AvButton(text = "저장하기", onClick = { clicks++ }, loading = true) } }
        rule.onNodeWithText("저장하기").assertExists().assertIsNotEnabled().performClick()
        assertEquals(0, clicks)
    }

    @Test fun `primary is allowed only in large and medium`() {
        assertEquals(true, isAllowedButtonCombination(AvButtonStyle.Primary, AvButtonSize.Large))
        assertEquals(true, isAllowedButtonCombination(AvButtonStyle.Primary, AvButtonSize.Medium))
        assertEquals(false, isAllowedButtonCombination(AvButtonStyle.Primary, AvButtonSize.Small))
        assertEquals(true, isAllowedButtonCombination(AvButtonStyle.Secondary, AvButtonSize.Small))
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :app:testDebugUnitTest --tests "com.car.autoverdict.ui.components.AvButtonTest"`
Expected: 컴파일 실패 — `Unresolved reference: AvButton`.

- [ ] **Step 3: `AvPressable.kt` 구현**

```kotlin
package com.car.autoverdict.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role

/**
 * Toss-style press feedback: shrinks to 96% for 100ms instead of a ripple.
 * Merges descendant semantics like any clickable.
 */
fun Modifier.avPressable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    role: Role = Role.Button,
    onClickLabel: String? = null,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "avPressScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.clickable(
        interactionSource = interactionSource,
        indication = null,
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        onClick = onClick,
    )
}
```

- [ ] **Step 4: `AvButton.kt` 구현**

```kotlin
package com.car.autoverdict.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.car.autoverdict.ui.theme.AvTheme

enum class AvButtonStyle { Primary, Weak, Secondary, Danger }

enum class AvButtonSize(val minHeight: Dp, val horizontalPadding: Dp) {
    Large(56.dp, 20.dp),
    Medium(48.dp, 18.dp),
    Small(36.dp, 14.dp),
}

internal fun isAllowedButtonCombination(style: AvButtonStyle, size: AvButtonSize): Boolean =
    !(style == AvButtonStyle.Primary && size == AvButtonSize.Small)

/**
 * Primary is Large/Medium only: white on accent is the spec's one allowed
 * sub-AA pair (3.71:1) and is limited to SemiBold 15sp+ labels.
 * Danger uses the tinted pair (dangerBg + dangerText), never white on dangerFill.
 */
@Composable
fun AvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AvButtonStyle = AvButtonStyle.Primary,
    size: AvButtonSize = AvButtonSize.Large,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    require(isAllowedButtonCombination(style, size)) { "Primary buttons are Large or Medium only" }
    val colors = AvTheme.colors
    val shapes = AvTheme.shapes
    val typography = AvTheme.typography
    val (container, content) = when {
        !enabled -> colors.surfaceMuted to colors.textDisabled
        style == AvButtonStyle.Primary -> colors.accent to colors.onAccent
        style == AvButtonStyle.Weak -> colors.accentWeak to colors.accentText
        style == AvButtonStyle.Secondary -> colors.surfaceMuted to colors.textSecondary
        else -> colors.dangerBg to colors.dangerText
    }
    val shape = when (size) {
        AvButtonSize.Large -> shapes.buttonL
        AvButtonSize.Medium -> shapes.buttonM
        AvButtonSize.Small -> shapes.buttonS
    }
    val textStyle = when (size) {
        AvButtonSize.Large -> typography.buttonL
        AvButtonSize.Medium -> typography.buttonM
        AvButtonSize.Small -> typography.buttonS
    }
    Box(
        modifier = modifier
            // Small (36dp) still gets a 48dp touch target (spec 6).
            .minimumInteractiveComponentSize()
            .avPressable(onClick = onClick, enabled = enabled && !loading)
            .heightIn(min = size.minHeight)
            .clip(shape)
            .background(container)
            .padding(horizontal = size.horizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = textStyle,
            color = content,
            maxLines = 1,
            modifier = Modifier.alpha(if (loading) 0f else 1f),
        )
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = content,
                strokeWidth = 2.dp,
            )
        }
    }
}
```

- [ ] **Step 5: 통과 확인**

Run: `./gradlew :app:testDebugUnitTest --tests "com.car.autoverdict.ui.components.AvButtonTest"`
Expected: 4 tests PASS. Espresso 관련 `NoSuchMethodException`이 나면 `app/build.gradle.kts`에 `testImplementation("androidx.test.espresso:espresso-core:3.6.1")`을 추가해 버전을 고정한다. Robolectric에서 Pretendard 리소스 로딩 오류가 나면 오류 메시지를 확인하고 `systematic-debugging` 스킬로 원인을 찾는다(테스트에서 폰트를 빼는 우회는 하지 않는다).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/com/car/autoverdict/ui/components/AvPressable.kt app/src/main/kotlin/com/car/autoverdict/ui/components/AvButton.kt app/src/test/kotlin/com/car/autoverdict/ui/components/AvButtonTest.kt
git commit -m "feat(ui): add avPressable modifier and AvButton"
```

---

### Task 8: `AvTopBar`·`AvIconButton`, `AvBottomCta`

**Files:**
- Create: `app/src/main/kotlin/com/car/autoverdict/ui/components/AvTopBar.kt`
- Create: `app/src/main/kotlin/com/car/autoverdict/ui/components/AvBottomCta.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/kotlin/com/car/autoverdict/ui/components/AvTopBarTest.kt`
- Test: `app/src/test/kotlin/com/car/autoverdict/ui/components/AvBottomCtaTest.kt`

- [ ] **Step 1: 문자열 추가**

`strings.xml`의 `</resources>` 위에:
```xml
    <string name="cd_back">뒤로</string>
    <string name="cd_clear">지우기</string>
    <string name="cd_settings">설정</string>
```

- [ ] **Step 2: 실패하는 테스트 작성**

`AvTopBarTest.kt`:
```kotlin
package com.car.autoverdict.ui.components

import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.car.autoverdict.ui.theme.AvTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AvTopBarTest {

    @get:Rule val rule = createComposeRule()

    @Test fun `back button is labelled and invokes onBack`() {
        var backs = 0
        rule.setContent { AvTheme { AvTopBar(onBack = { backs++ }, title = "분석 결과", subtitle = "저장된 매물") } }
        rule.onNodeWithText("분석 결과").assertExists()
        rule.onNodeWithText("저장된 매물").assertExists()
        rule.onNodeWithContentDescription("뒤로").performClick()
        assertEquals(1, backs)
    }

    @Test fun `tab root bar has no back button`() {
        rule.setContent { AvTheme { AvTopBar(onBack = null, title = "AutoVerdict") } }
        rule.onNodeWithContentDescription("뒤로").assertDoesNotExist()
    }

    @Test fun `icon button exposes its description and click`() {
        var clicks = 0
        rule.setContent {
            AvTheme {
                AvTopBar(onBack = null, title = "AutoVerdict", actions = {
                    AvIconButton(
                        icon = Icons.Rounded.Settings,
                        contentDescription = "설정",
                        onClick = { clicks++ },
                    )
                })
            }
        }
        rule.onNodeWithContentDescription("설정").performClick()
        assertEquals(1, clicks)
    }
}
```

`AvBottomCtaTest.kt`:
```kotlin
package com.car.autoverdict.ui.components

import android.app.Application
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.car.autoverdict.ui.theme.AvTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AvBottomCtaTest {

    @get:Rule val rule = createComposeRule()

    @Test fun `renders its buttons`() {
        rule.setContent {
            AvTheme {
                AvBottomCta {
                    AvButton(text = "판매자에게 물어볼 것", onClick = {}, modifier = Modifier.weight(1f))
                }
            }
        }
        rule.onNodeWithText("판매자에게 물어볼 것").assertIsDisplayed()
    }
}
```
(`Modifier.weight`는 `RowScope` 안이라 바로 쓸 수 있다.)

- [ ] **Step 3: 실패 확인**

Run: `./gradlew :app:testDebugUnitTest --tests "com.car.autoverdict.ui.components.AvTopBarTest" --tests "com.car.autoverdict.ui.components.AvBottomCtaTest"`
Expected: 컴파일 실패 — `Unresolved reference: AvTopBar`.

- [ ] **Step 4: `AvTopBar.kt` 구현**

```kotlin
package com.car.autoverdict.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.ui.theme.AvTheme

/**
 * Shared top bar. Tab roots pass `onBack = null` and get a large title; sub-screens
 * get a back button and a compact title. Owns the status-bar inset (spec 6).
 */
@Composable
fun AvTopBar(
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .heightIn(min = 56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            AvIconButton(
                icon = Icons.Rounded.ArrowBackIosNew,
                contentDescription = stringResource(R.string.cd_back),
                onClick = onBack,
                tint = colors.textPrimary,
            )
        } else {
            Spacer(Modifier.width(16.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = if (onBack == null) typography.title2 else typography.body1.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = typography.caption1,
                    color = colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        Spacer(Modifier.width(4.dp))
    }
}

/** 48dp touch target, 24dp icon. Use for top-bar actions and inline icon actions. */
@Composable
fun AvIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = AvTheme.colors.textSecondary,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .avPressable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
    }
}
```

- [ ] **Step 5: `AvBottomCta.kt` 구현**

```kotlin
package com.car.autoverdict.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.car.autoverdict.ui.theme.AvTheme

/** Fixed bottom action area. Owns navigation-bar and IME insets (spec 6). */
@Composable
fun AvBottomCta(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = AvTheme.colors
    Column(modifier = modifier.fillMaxWidth().background(colors.background)) {
        HorizontalDivider(thickness = 1.dp, color = colors.border)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}
```

- [ ] **Step 6: 통과 확인**

Run: `./gradlew :app:testDebugUnitTest --tests "com.car.autoverdict.ui.components.AvTopBarTest" --tests "com.car.autoverdict.ui.components.AvBottomCtaTest"`
Expected: 4 tests PASS.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/kotlin/com/car/autoverdict/ui/components/AvTopBar.kt app/src/main/kotlin/com/car/autoverdict/ui/components/AvBottomCta.kt app/src/main/res/values/strings.xml app/src/test/kotlin/com/car/autoverdict/ui/components/AvTopBarTest.kt app/src/test/kotlin/com/car/autoverdict/ui/components/AvBottomCtaTest.kt
git commit -m "feat(ui): add AvTopBar, AvIconButton and AvBottomCta"
```

---

### Task 9: `AvListRow`, `AvTextField`

**Files:**
- Create: `app/src/main/kotlin/com/car/autoverdict/ui/components/AvListRow.kt`
- Create: `app/src/main/kotlin/com/car/autoverdict/ui/components/AvTextField.kt`
- Test: `app/src/test/kotlin/com/car/autoverdict/ui/components/AvListRowTest.kt`
- Test: `app/src/test/kotlin/com/car/autoverdict/ui/components/AvTextFieldTest.kt`

- [ ] **Step 1: 실패하는 테스트 작성**

`AvListRowTest.kt`:
```kotlin
package com.car.autoverdict.ui.components

import android.app.Application
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import com.car.autoverdict.ui.theme.AvTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AvListRowTest {

    @get:Rule val rule = createComposeRule()

    private val mergedRow = hasText("그랜저 IG") and hasText("8.2만km · 2,180만원")

    @Test fun `clickable row is one button node that reads title and subtitle together`() {
        var clicks = 0
        rule.setContent {
            AvTheme { AvListRow(title = "그랜저 IG", subtitle = "8.2만km · 2,180만원", onClick = { clicks++ }) }
        }
        rule.onNode(mergedRow)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()
        assertEquals(1, clicks)
    }

    @Test fun `static row still merges its texts`() {
        rule.setContent { AvTheme { AvListRow(title = "그랜저 IG", subtitle = "8.2만km · 2,180만원") } }
        rule.onNode(mergedRow).assertExists()
    }
}
```

`AvTextFieldTest.kt`:
```kotlin
package com.car.autoverdict.ui.components

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.car.autoverdict.ui.theme.AvTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AvTextFieldTest {

    @get:Rule val rule = createComposeRule()

    @Test fun `shows placeholder when empty`() {
        rule.setContent { AvTheme { AvTextField(value = "", onValueChange = {}, placeholder = "엔카 매물 링크 붙여넣기") } }
        rule.onNodeWithText("엔카 매물 링크 붙여넣기").assertIsDisplayed()
    }

    @Test fun `shows error text under the field`() {
        rule.setContent {
            AvTheme {
                AvTextField(value = "abc", onValueChange = {}, placeholder = "p", errorText = "엔카 매물 링크가 아니에요")
            }
        }
        rule.onNodeWithText("엔카 매물 링크가 아니에요").assertIsDisplayed()
    }

    @Test fun `clear button appears with text and calls onClear`() {
        var cleared = 0
        rule.setContent {
            AvTheme { AvTextField(value = "abc", onValueChange = {}, placeholder = "p", onClear = { cleared++ }) }
        }
        rule.onNodeWithContentDescription("지우기").performClick()
        assertEquals(1, cleared)
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :app:testDebugUnitTest --tests "com.car.autoverdict.ui.components.AvListRowTest" --tests "com.car.autoverdict.ui.components.AvTextFieldTest"`
Expected: 컴파일 실패 — `Unresolved reference: AvListRow`.

- [ ] **Step 3: `AvListRow.kt` 구현**

```kotlin
package com.car.autoverdict.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.car.autoverdict.ui.theme.AvTheme

/** List row: leading slot · title/subtitle · trailing slot. Min 56dp; reads as one node. */
@Composable
fun AvListRow(
    title: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    val interaction = if (onClick != null) {
        Modifier.avPressable(onClick = onClick)
    } else {
        Modifier.semantics(mergeDescendants = true) {}
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(interaction)
            .heightIn(min = 56.dp)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = typography.body1,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = typography.body2,
                    color = colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.invoke()
    }
}
```

- [ ] **Step 4: `AvTextField.kt` 구현**

```kotlin
package com.car.autoverdict.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.ui.theme.AvTheme

/**
 * Filled single-line field. Placeholder uses textTertiary on surfaceMuted — the one
 * allowed exception in the spec's contrast table (4.2).
 */
@Composable
fun AvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    errorText: String? = null,
    onClear: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    Column(modifier = modifier) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = typography.body1.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clip(AvTheme.shapes.input)
                        .background(colors.surfaceMuted)
                        .padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (leadingIcon != null) {
                        Icon(leadingIcon, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(20.dp))
                    }
                    Box(modifier = Modifier.weight(1f).padding(vertical = 14.dp)) {
                        if (value.isEmpty()) {
                            Text(text = placeholder, style = typography.body1, color = colors.textTertiary, maxLines = 1)
                        }
                        innerTextField()
                    }
                    if (onClear != null && value.isNotEmpty()) {
                        AvIconButton(
                            icon = Icons.Rounded.Cancel,
                            contentDescription = stringResource(R.string.cd_clear),
                            onClick = onClear,
                            tint = colors.textTertiary,
                        )
                    }
                }
            },
        )
        if (errorText != null) {
            Text(
                text = errorText,
                style = typography.caption1,
                color = colors.dangerText,
                modifier = Modifier
                    .padding(start = 4.dp, top = 6.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}
```

- [ ] **Step 5: 통과 확인**

Run: `./gradlew :app:testDebugUnitTest --tests "com.car.autoverdict.ui.components.AvListRowTest" --tests "com.car.autoverdict.ui.components.AvTextFieldTest"`
Expected: 5 tests PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/com/car/autoverdict/ui/components/AvListRow.kt app/src/main/kotlin/com/car/autoverdict/ui/components/AvTextField.kt app/src/test/kotlin/com/car/autoverdict/ui/components/AvListRowTest.kt app/src/test/kotlin/com/car/autoverdict/ui/components/AvTextFieldTest.kt
git commit -m "feat(ui): add AvListRow and AvTextField"
```

---

### Task 10: `AvBadge`·`AvFactTile`, `AvSectionHeader`·`AvSectionDivider`

이 Task의 컴포넌트는 로직이 없는 표시용이라 단위 테스트 대신 갤러리(Task 14)에서 시각 확인한다. 갤러리는 Task 23 이후 설정 화면에서 열 수 있고, 실제 확인은 Task 26 수동 QA에서 한다(스펙 12절의 테스트 대상 목록에 없음).

**Files:**
- Create: `app/src/main/kotlin/com/car/autoverdict/ui/components/AvBadge.kt`
- Create: `app/src/main/kotlin/com/car/autoverdict/ui/components/AvSection.kt`

- [ ] **Step 1: `AvBadge.kt` 구현**

```kotlin
package com.car.autoverdict.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.car.autoverdict.ui.theme.AvTheme
import com.car.autoverdict.ui.theme.AvTone
import com.car.autoverdict.ui.theme.tone

/** Pill label in a semantic tone (e.g. verdict). */
@Composable
fun AvBadge(text: String, tone: AvTone, modifier: Modifier = Modifier) {
    val toneColors = AvTheme.colors.tone(tone)
    Text(
        text = text,
        style = AvTheme.typography.caption2,
        color = toneColors.text,
        modifier = modifier
            .clip(AvTheme.shapes.chip)
            .background(toneColors.bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** Small fact tile (label over value). Non-neutral tones tint the whole tile. */
@Composable
fun AvFactTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    tone: AvTone = AvTone.Neutral,
) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    val toneColors = colors.tone(tone)
    val neutral = tone == AvTone.Neutral
    Column(
        modifier = modifier
            .clip(AvTheme.shapes.tile)
            .background(toneColors.bg)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        Text(text = label, style = typography.caption1, color = if (neutral) colors.textSecondary else toneColors.text)
        Text(
            text = value,
            style = typography.body1.copy(fontWeight = FontWeight.SemiBold),
            color = if (neutral) colors.textPrimary else toneColors.text,
        )
    }
}
```

- [ ] **Step 2: `AvSection.kt` 구현**

```kotlin
package com.car.autoverdict.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.car.autoverdict.ui.theme.AvTheme

@Composable
fun AvSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 20.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (count != null) "$title $count" else title,
            style = typography.buttonM,
            color = colors.textSecondary,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        if (actionLabel != null && onAction != null) {
            Box(
                modifier = Modifier
                    .avPressable(onClick = onAction)
                    .minimumInteractiveComponentSize()
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = actionLabel, style = typography.caption1, color = colors.textTertiary)
            }
        }
    }
}

/** 8dp grey band between sections (no shadows, no hairlines). */
@Composable
fun AvSectionDivider(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(AvTheme.spacing.sectionDivider)
            .background(AvTheme.colors.surfaceMuted),
    )
}
```

- [ ] **Step 3: 빌드 확인**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/car/autoverdict/ui/components/AvBadge.kt app/src/main/kotlin/com/car/autoverdict/ui/components/AvSection.kt
git commit -m "feat(ui): add AvBadge, AvFactTile and section header/divider"
```

---

## Chunk 3: 공용 컴포넌트 (2/2) — 시트·토스트·이모지·탭바·갤러리

Chunk 2의 **공통 규칙**이 그대로 적용된다.

### Task 11: `AvBottomSheet`·`AvDialog`, `AvToast`

**Files:**
- Create: `app/src/main/kotlin/com/car/autoverdict/ui/components/AvBottomSheet.kt`
- Create: `app/src/main/kotlin/com/car/autoverdict/ui/components/AvToast.kt`

표시용 래퍼라 단위 테스트는 두지 않고 갤러리(Task 14)를 통해 Task 26 수동 QA에서 확인한다.

- [ ] **Step 1: `AvBottomSheet.kt` 구현**

```kotlin
package com.car.autoverdict.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.car.autoverdict.ui.theme.AvPalette
import com.car.autoverdict.ui.theme.AvTheme

/** Modal sheet with a 24dp top radius, drag handle and drag-to-dismiss. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvBottomSheet(
    onDismiss: () -> Unit,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AvTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = AvTheme.shapes.sheetTop,
        containerColor = colors.background,
        scrimColor = colors.scrim,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(AvTheme.shapes.chip)
                    .background(AvPalette.Grey300),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        ) {
            if (title != null) {
                Text(text = title, style = AvTheme.typography.title3, color = colors.textPrimary)
                Spacer(Modifier.height(12.dp))
            }
            content()
        }
    }
}

/** Confirm dialog. `destructive` turns the confirm button into the Danger style. */
@Composable
fun AvDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    val colors = AvTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = AvTheme.shapes.dialog,
        containerColor = colors.background,
        title = { Text(text = title, style = AvTheme.typography.title3, color = colors.textPrimary) },
        text = { Text(text = message, style = AvTheme.typography.body2, color = colors.textSecondary) },
        confirmButton = {
            AvButton(
                text = confirmLabel,
                onClick = onConfirm,
                style = if (destructive) AvButtonStyle.Danger else AvButtonStyle.Primary,
                size = AvButtonSize.Medium,
            )
        },
        dismissButton = {
            AvButton(text = dismissLabel, onClick = onDismiss, style = AvButtonStyle.Secondary, size = AvButtonSize.Medium)
        },
    )
}
```

- [ ] **Step 2: `AvToast.kt` 구현**

```kotlin
package com.car.autoverdict.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.car.autoverdict.ui.theme.AvPalette
import com.car.autoverdict.ui.theme.AvTheme

/** Owned by AppRoot and handed down via [LocalAvToast] (spec 6). */
@Stable
class AvToastState {
    internal val host = SnackbarHostState()

    suspend fun show(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        val result = host.showSnackbar(message = message, actionLabel = actionLabel, duration = SnackbarDuration.Short)
        if (result == SnackbarResult.ActionPerformed) onAction?.invoke()
    }
}

val LocalAvToast = staticCompositionLocalOf<AvToastState> {
    error("AvToastState not provided — wrap content in AppRoot")
}

@Composable
fun rememberAvToastState(): AvToastState = remember { AvToastState() }

@Composable
fun AvToastHost(state: AvToastState, modifier: Modifier = Modifier) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    SnackbarHost(hostState = state.host, modifier = modifier) { data ->
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .fillMaxWidth()
                .clip(AvTheme.shapes.buttonM)
                .background(colors.toastBg)
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = data.visuals.message,
                style = typography.body2,
                color = AvPalette.White,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 14.dp),
            )
            val action = data.visuals.actionLabel
            if (action != null) {
                Box(
                    modifier = Modifier
                        .avPressable(onClick = { data.performAction() })
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                ) {
                    Text(text = action, style = typography.buttonM, color = colors.toastAction)
                }
            }
        }
    }
}
```

- [ ] **Step 3: 빌드 확인**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`. `ModalBottomSheet` 파라미터 이름이 Material3 버전과 맞지 않아 컴파일 오류가 나면, 오류 메시지의 파라미터 목록에 맞춰 이름만 고친다(동작은 위와 같게 유지).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/car/autoverdict/ui/components/AvBottomSheet.kt app/src/main/kotlin/com/car/autoverdict/ui/components/AvToast.kt
git commit -m "feat(ui): add AvBottomSheet, AvDialog and AvToast"
```

---

### Task 12: `AvIllustrationIcon`·`AvEmptyState`, `AvLoadingSteps`

**Files:**
- Create: `app/src/main/kotlin/com/car/autoverdict/ui/components/AvIllustration.kt`
- Create: `app/src/main/kotlin/com/car/autoverdict/ui/components/AvLoadingSteps.kt`
- Modify: `app/src/main/res/values/strings.xml`

토스페이스는 제외됐다(맨 위 결정 기록). 그림 자리는 톤 배경 원 안의 `Icons.Rounded` 아이콘으로 채운다(스펙 6절 `AvIllustrationIcon`).

- [ ] **Step 1: `AvIllustration.kt` 구현**

```kotlin
package com.car.autoverdict.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.car.autoverdict.ui.theme.AvTheme
import com.car.autoverdict.ui.theme.AvTone
import com.car.autoverdict.ui.theme.tone

/** Decorative illustration: a rounded icon inside a tone-tinted circle (replaces Tossface; spec 6). */
@Composable
fun AvIllustrationIcon(icon: ImageVector, tone: AvTone, size: Dp, modifier: Modifier = Modifier) {
    val toneColors = AvTheme.colors.tone(tone)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(toneColors.bg)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = toneColors.fill, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun AvEmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    tone: AvTone = AvTone.Neutral,
    description: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AvIllustrationIcon(icon = icon, tone = tone, size = 64.dp)
        Spacer(Modifier.height(16.dp))
        Text(text = title, style = typography.title3, color = colors.textPrimary, textAlign = TextAlign.Center)
        if (description != null) {
            Spacer(Modifier.height(6.dp))
            Text(text = description, style = typography.body2, color = colors.textSecondary, textAlign = TextAlign.Center)
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            AvButton(text = actionLabel, onClick = onAction, style = AvButtonStyle.Weak, size = AvButtonSize.Medium)
        }
    }
}
```

- [ ] **Step 2: `AvLoadingSteps.kt` 구현**

`strings.xml`의 `</resources>` 위에 먼저 추가:
```xml
    <string name="loading_step_done">완료</string>
    <string name="loading_step_active">진행 중</string>
    <string name="loading_step_pending">대기 중</string>
```

```kotlin
package com.car.autoverdict.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.ui.theme.AvTheme

enum class StepState { Done, Active, Pending }

data class LoadingStep(val label: String, val state: StepState)

/** Step list for staged loading. Wiring to real collector stages happens in sub-project 1. */
@Composable
fun AvLoadingSteps(steps: List<LoadingStep>, modifier: Modifier = Modifier) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        steps.forEach { step ->
            val stateLabel = stringResource(
                when (step.state) {
                    StepState.Done -> R.string.loading_step_done
                    StepState.Active -> R.string.loading_step_active
                    StepState.Pending -> R.string.loading_step_pending
                },
            )
            Row(
                modifier = Modifier.semantics(mergeDescendants = true) { stateDescription = stateLabel },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                    when (step.state) {
                        StepState.Done -> Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(20.dp))
                        StepState.Active -> CircularProgressIndicator(modifier = Modifier.size(18.dp), color = colors.accentText, strokeWidth = 2.dp)
                        StepState.Pending -> Icon(Icons.Rounded.RadioButtonUnchecked, contentDescription = null, tint = colors.textDisabled, modifier = Modifier.size(20.dp))
                    }
                }
                Text(
                    text = step.label,
                    style = typography.body2,
                    color = if (step.state == StepState.Pending) colors.textTertiary else colors.textPrimary,
                )
            }
        }
    }
}
```

- [ ] **Step 3: 빌드 확인**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/car/autoverdict/ui/components/AvIllustration.kt app/src/main/kotlin/com/car/autoverdict/ui/components/AvLoadingSteps.kt app/src/main/res/values/strings.xml
git commit -m "feat(ui): add AvIllustrationIcon, AvEmptyState and AvLoadingSteps"
```

---

### Task 13: `AvTabBar`

**선행:** Task 15(`MainTab` 생성). 병렬 실행 계획상 Task 15는 웨이브 1, Task 13은 웨이브 2라 순서가 보장된다.

**Files:**
- Create: `app/src/main/kotlin/com/car/autoverdict/ui/components/AvTabBar.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/kotlin/com/car/autoverdict/ui/components/AvTabBarTest.kt`

- [ ] **Step 1: 문자열 추가·수정**

`strings.xml`에서 `tab_saved` 값을 `저장한 차`로 바꾸고, `</resources>` 위에 추가:
```xml
    <string name="tab_home">홈</string>
    <string name="tab_saved_count">저장한 차 %1$d</string>
```
(`tab_analyze`, `tab_settings`는 아직 `MainActivity`가 쓰므로 Task 23에서 지운다.)

- [ ] **Step 2: 실패하는 테스트 작성**

`AvTabBarTest.kt`:
```kotlin
package com.car.autoverdict.ui.components

import android.app.Application
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.car.autoverdict.ui.navigation.MainTab
import com.car.autoverdict.ui.theme.AvTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AvTabBarTest {

    @get:Rule val rule = createComposeRule()

    @Test fun `selecting a tab reports it`() {
        var selected: MainTab? = null
        rule.setContent { AvTheme { AvTabBar(selected = MainTab.Home, onSelect = { selected = it }, savedCount = 0) } }
        rule.onNodeWithText("홈").assertIsSelected()
        rule.onNodeWithText("저장한 차").performClick()
        assertEquals(MainTab.Saved, selected)
    }

    @Test fun `saved tab shows the count when there are saved cars`() {
        rule.setContent { AvTheme { AvTabBar(selected = MainTab.Home, onSelect = {}, savedCount = 3) } }
        rule.onNodeWithText("저장한 차 3").assertExists()
    }
}
```

- [ ] **Step 3: 실패 확인**

Run: `./gradlew :app:testDebugUnitTest --tests "com.car.autoverdict.ui.components.AvTabBarTest"`
Expected: 컴파일 실패 — `Unresolved reference: AvTabBar`.

- [ ] **Step 4: `AvTabBar.kt` 구현**

```kotlin
package com.car.autoverdict.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.ui.navigation.MainTab
import com.car.autoverdict.ui.theme.AvTheme

/** Two-tab bar. Unselected tabs use textTertiary (active, not disabled). Owns nav-bar inset. */
@Composable
fun AvTabBar(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    savedCount: Int,
    modifier: Modifier = Modifier,
) {
    val colors = AvTheme.colors
    Column(modifier = modifier.fillMaxWidth().background(colors.background)) {
        HorizontalDivider(thickness = 1.dp, color = colors.border)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .heightIn(min = 56.dp)
                .selectableGroup(),
        ) {
            MainTab.entries.forEach { tab ->
                val isSelected = tab == selected
                val tint = if (isSelected) colors.textPrimary else colors.textTertiary
                val label = when (tab) {
                    MainTab.Home -> stringResource(R.string.tab_home)
                    MainTab.Saved -> if (savedCount > 0) {
                        stringResource(R.string.tab_saved_count, savedCount)
                    } else {
                        stringResource(R.string.tab_saved)
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .selectable(
                            selected = isSelected,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelect(tab) },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = if (tab == MainTab.Home) Icons.Rounded.Home else Icons.Rounded.Bookmark,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(text = label, style = AvTheme.typography.caption2, color = tint)
                }
            }
        }
    }
}
```

- [ ] **Step 5: 통과 확인**

Run: `./gradlew :app:testDebugUnitTest --tests "com.car.autoverdict.ui.components.AvTabBarTest"`
Expected: 2 tests PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/com/car/autoverdict/ui/components/AvTabBar.kt app/src/main/res/values/strings.xml app/src/test/kotlin/com/car/autoverdict/ui/components/AvTabBarTest.kt
git commit -m "feat(ui): add two-tab AvTabBar"
```

---

### Task 14: 컴포넌트 갤러리 (debug 전용)

**Files:**
- Create: `app/src/main/kotlin/com/car/autoverdict/ui/gallery/ComponentGalleryScreen.kt`

진입 경로(설정 화면의 debug 전용 행)는 Task 21·22에서 연결한다.

- [ ] **Step 1: 갤러리 화면 구현**

```kotlin
package com.car.autoverdict.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.car.autoverdict.ui.components.AvBadge
import com.car.autoverdict.ui.components.AvBottomCta
import com.car.autoverdict.ui.components.AvBottomSheet
import com.car.autoverdict.ui.components.AvButton
import com.car.autoverdict.ui.components.AvButtonSize
import com.car.autoverdict.ui.components.AvButtonStyle
import com.car.autoverdict.ui.components.AvDialog
import com.car.autoverdict.ui.components.AvIllustrationIcon
import com.car.autoverdict.ui.components.AvEmptyState
import com.car.autoverdict.ui.components.AvFactTile
import com.car.autoverdict.ui.components.AvListRow
import com.car.autoverdict.ui.components.AvLoadingSteps
import com.car.autoverdict.ui.components.AvSectionDivider
import com.car.autoverdict.ui.components.AvSectionHeader
import com.car.autoverdict.ui.components.AvTabBar
import com.car.autoverdict.ui.components.AvTextField
import com.car.autoverdict.ui.components.AvToastHost
import com.car.autoverdict.ui.components.AvTopBar
import com.car.autoverdict.ui.components.LoadingStep
import com.car.autoverdict.ui.components.StepState
import com.car.autoverdict.ui.components.rememberAvToastState
import com.car.autoverdict.ui.navigation.MainTab
import com.car.autoverdict.ui.theme.AvTheme
import com.car.autoverdict.ui.theme.AvTone
import kotlinx.coroutines.launch

/** Debug-only visual check of every shared component and state (spec 6). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComponentGalleryScreen(onBack: () -> Unit) {
    val colors = AvTheme.colors
    val toast = rememberAvToastState()
    val scope = rememberCoroutineScope()
    var field by remember { mutableStateOf("") }
    var showSheet by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(MainTab.Home) }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AvTopBar(onBack = onBack, title = "컴포넌트 갤러리", subtitle = "debug 전용")
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                AvSectionHeader(title = "Button")
                FlowRow(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AvButton(text = "Primary L", onClick = {})
                    AvButton(text = "Primary M", onClick = {}, size = AvButtonSize.Medium)
                    AvButton(text = "Weak M", onClick = {}, style = AvButtonStyle.Weak, size = AvButtonSize.Medium)
                    AvButton(text = "Secondary S", onClick = {}, style = AvButtonStyle.Secondary, size = AvButtonSize.Small)
                    AvButton(text = "Danger M", onClick = {}, style = AvButtonStyle.Danger, size = AvButtonSize.Medium)
                    AvButton(text = "Disabled", onClick = {}, enabled = false, size = AvButtonSize.Medium)
                    AvButton(text = "Loading", onClick = {}, loading = true, size = AvButtonSize.Medium)
                }
                AvSectionDivider(modifier = Modifier.padding(top = 20.dp))

                AvSectionHeader(title = "List row", count = 2, actionLabel = "전체 보기", onAction = {})
                AvListRow(
                    title = "그랜저 IG 2.4",
                    subtitle = "8.2만km · 2,180만원",
                    onClick = {},
                    leading = { Icon(Icons.Rounded.DirectionsCar, contentDescription = null, tint = colors.textSecondary) },
                    trailing = { AvBadge(text = "주의", tone = AvTone.Warning) },
                )
                AvListRow(
                    title = "설정 행",
                    onClick = {},
                    trailing = { Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.textTertiary) },
                )
                AvListRow(title = "정적 행 (클릭 없음)", subtitle = "부제는 body2")
                AvSectionDivider()

                AvSectionHeader(title = "Top bar (tab root)")
                AvTopBar(onBack = null, title = "AutoVerdict", subtitle = "탭 루트 변형")
                AvSectionDivider()

                AvSectionHeader(title = "Text field")
                AvTextField(
                    value = field,
                    onValueChange = { field = it },
                    placeholder = "엔카 매물 링크 붙여넣기",
                    leadingIcon = Icons.Rounded.Link,
                    errorText = if (field.isNotEmpty() && !field.contains("encar")) "엔카 매물 링크가 아니에요" else null,
                    onClear = { field = "" },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                AvTextField(
                    value = "https://google.com",
                    onValueChange = {},
                    placeholder = "엔카 매물 링크 붙여넣기",
                    leadingIcon = Icons.Rounded.Link,
                    errorText = "엔카 매물 링크가 아니에요",
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
                )
                AvSectionDivider(modifier = Modifier.padding(top = 20.dp))

                AvSectionHeader(title = "Badge · Fact tile")
                Row(modifier = Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AvBadge(text = "피하세요", tone = AvTone.Danger)
                    AvBadge(text = "주의", tone = AvTone.Warning)
                    AvBadge(text = "괜찮아요", tone = AvTone.Success)
                    AvBadge(text = "확인 필요", tone = AvTone.Neutral)
                }
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AvFactTile(label = "사고", value = "내차 0건", modifier = Modifier.weight(1f))
                    AvFactTile(label = "소유자", value = "3명", tone = AvTone.Warning, modifier = Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AvFactTile(label = "침수·전손", value = "있음", tone = AvTone.Danger, modifier = Modifier.weight(1f))
                    AvFactTile(label = "프레임", value = "무사고", tone = AvTone.Success, modifier = Modifier.weight(1f))
                }
                AvSectionDivider()

                AvSectionHeader(title = "Illustration · Empty state")
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AvIllustrationIcon(icon = Icons.Rounded.DirectionsCar, tone = AvTone.Accent, size = 48.dp)
                    AvIllustrationIcon(icon = Icons.Rounded.Shield, tone = AvTone.Success, size = 48.dp)
                    AvIllustrationIcon(icon = Icons.Rounded.Warning, tone = AvTone.Warning, size = 48.dp)
                    AvIllustrationIcon(icon = Icons.Rounded.Block, tone = AvTone.Danger, size = 48.dp)
                    AvIllustrationIcon(icon = Icons.Rounded.Search, tone = AvTone.Neutral, size = 48.dp)
                }
                AvEmptyState(
                    icon = Icons.Rounded.BookmarkBorder,
                    title = "마음에 드는 차를 저장해 보세요",
                    description = "결과 화면에서 저장하면 여기서 비교할 수 있어요",
                    actionLabel = "차 확인하러 가기",
                    onAction = {},
                )
                AvSectionDivider()

                AvSectionHeader(title = "Loading steps")
                AvLoadingSteps(
                    steps = listOf(
                        LoadingStep("매물 정보 가져오기", StepState.Done),
                        LoadingStep("사고·보험 이력 확인 중", StepState.Active),
                        LoadingStep("12가지 기준으로 평가", StepState.Pending),
                    ),
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                AvSectionDivider(modifier = Modifier.padding(top = 20.dp))

                AvSectionHeader(title = "Sheet · Dialog · Toast")
                FlowRow(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AvButton(text = "시트", onClick = { showSheet = true }, style = AvButtonStyle.Weak, size = AvButtonSize.Medium)
                    AvButton(text = "다이얼로그", onClick = { showDialog = true }, style = AvButtonStyle.Weak, size = AvButtonSize.Medium)
                    AvButton(
                        text = "토스트",
                        onClick = { scope.launch { toast.show("저장했어요", actionLabel = "보기") } },
                        style = AvButtonStyle.Weak,
                        size = AvButtonSize.Medium,
                    )
                }
                AvSectionDivider(modifier = Modifier.padding(top = 20.dp))

                AvSectionHeader(title = "Tab bar")
                AvTabBar(selected = tab, onSelect = { tab = it }, savedCount = 3)
            }
            AvBottomCta {
                AvButton(text = "판매자에게 물어볼 것", onClick = {}, modifier = Modifier.weight(1f))
            }
        }
        AvToastHost(state = toast, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
    }

    if (showSheet) {
        AvBottomSheet(onDismiss = { showSheet = false }, title = "이 판정은 무슨 뜻인가요?") {
            AvListRow(title = "시트 안의 행", subtitle = "드래그해서 닫을 수 있어요")
        }
    }
    if (showDialog) {
        AvDialog(
            title = "캐시를 삭제할까요?",
            message = "저장된 분석 캐시가 모두 삭제돼요.",
            confirmLabel = "삭제",
            onConfirm = { showDialog = false },
            dismissLabel = "취소",
            onDismiss = { showDialog = false },
            destructive = true,
        )
    }
}
```
(아이콘 중 `material-icons-extended`에 없는 이름이 있으면 컴파일 오류 메시지를 보고 같은 의미의 `Icons.Rounded.*`로 바꾼다.)

- [ ] **Step 2: 빌드 확인**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/com/car/autoverdict/ui/gallery/ComponentGalleryScreen.kt
git commit -m "feat(ui): add debug component gallery"
```

---

## Chunk 4: 내비게이션 코어 (순수 Kotlin)

이 Chunk의 코드는 Compose·Android 의존성이 없는 순수 Kotlin이다(예외: `NavStateSaver`는 `runtime-saveable`의 `listSaver`만 쓴다). 테스트는 Robolectric 없이 JUnit으로 돌린다. 패키지는 `com.car.autoverdict.ui.navigation`, 테스트 위치는 `app/src/test/kotlin/com/car/autoverdict/ui/navigation/`.

### Task 15: `MainTab`, `Route`, `RouteCodec`

**Files:**
- Create: `app/src/main/kotlin/com/car/autoverdict/ui/navigation/MainTab.kt`
- Create: `app/src/main/kotlin/com/car/autoverdict/ui/navigation/Route.kt`
- Create: `app/src/main/kotlin/com/car/autoverdict/ui/navigation/RouteCodec.kt`
- Test: `app/src/test/kotlin/com/car/autoverdict/ui/navigation/RouteCodecTest.kt`

- [ ] **Step 1: 실패하는 테스트 작성**

`RouteCodecTest.kt`:
```kotlin
package com.car.autoverdict.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RouteCodecTest {

    private val url = "https://fem.encar.com/cars/detail/41623743"

    @Test fun `every route survives an encode-decode round trip`() {
        val routes = listOf(
            Route.Onboarding, Route.Home, Route.Saved, Route.Settings,
            Route.PrivacyPolicy, Route.Licenses, Route.Gallery,
            Route.Result(url, fromSaved = false),
            Route.Result(url, fromSaved = true),
            Route.Compare(listOf("111", "222")),
        )
        routes.forEach { assertEquals(it, RouteCodec.decode(RouteCodec.encode(it))) }
    }

    @Test fun `result url keeps separator characters intact`() {
        val odd = Route.Result("https://x.test/a|b?c=1|2", fromSaved = false)
        assertEquals(odd, RouteCodec.decode(RouteCodec.encode(odd)))
    }

    @Test fun `malformed values decode to null`() {
        assertNull(RouteCodec.decode("nope"))
        assertNull(RouteCodec.decode("result|1"))
        assertNull(RouteCodec.decode("compare|"))
        assertNull(RouteCodec.decode(""))
    }

    @Test fun `tab roots map to tabs and back`() {
        assertEquals(MainTab.Home, Route.Home.tabOrNull())
        assertEquals(MainTab.Saved, Route.Saved.tabOrNull())
        assertNull(Route.Settings.tabOrNull())
        assertEquals(Route.Home, MainTab.Home.route)
        assertEquals(Route.Saved, MainTab.Saved.route)
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :app:testDebugUnitTest --tests "com.car.autoverdict.ui.navigation.RouteCodecTest"`
Expected: 컴파일 실패 — `Unresolved reference: Route`.

- [ ] **Step 3: `MainTab.kt`과 `Route.kt` 구현**

`MainTab.kt`:
```kotlin
package com.car.autoverdict.ui.navigation

/** Bottom tabs (spec 7.1). Maps 1:1 to the tab-root routes. */
enum class MainTab { Home, Saved }
```

`Route.kt`:
```kotlin
package com.car.autoverdict.ui.navigation

/** Navigation destinations (spec 7.2). Only small values live here — they are saved in a Bundle. */
sealed interface Route {
    data object Onboarding : Route
    data object Home : Route
    data object Saved : Route
    data object Settings : Route
    data object PrivacyPolicy : Route
    data object Licenses : Route
    data object Gallery : Route
    data class Result(val url: String, val fromSaved: Boolean) : Route
    data class Compare(val carIds: List<String>) : Route
}

val MainTab.route: Route
    get() = when (this) {
        MainTab.Home -> Route.Home
        MainTab.Saved -> Route.Saved
    }

/** Non-null only for tab roots — the tab bar and banner render only then. */
fun Route.tabOrNull(): MainTab? = when (this) {
    Route.Home -> MainTab.Home
    Route.Saved -> MainTab.Saved
    else -> null
}
```

- [ ] **Step 4: `RouteCodec.kt` 구현**

```kotlin
package com.car.autoverdict.ui.navigation

/** String form of [Route] for saved-instance state. Unknown or malformed input decodes to null. */
object RouteCodec {

    fun encode(route: Route): String = when (route) {
        Route.Onboarding -> "onboarding"
        Route.Home -> "home"
        Route.Saved -> "saved"
        Route.Settings -> "settings"
        Route.PrivacyPolicy -> "privacy"
        Route.Licenses -> "licenses"
        Route.Gallery -> "gallery"
        is Route.Result -> "result|${if (route.fromSaved) 1 else 0}|${route.url}"
        is Route.Compare -> "compare|${route.carIds.joinToString(",")}"
    }

    fun decode(value: String): Route? {
        val parts = value.split("|", limit = 3)
        return when (parts[0]) {
            "onboarding" -> Route.Onboarding
            "home" -> Route.Home
            "saved" -> Route.Saved
            "settings" -> Route.Settings
            "privacy" -> Route.PrivacyPolicy
            "licenses" -> Route.Licenses
            "gallery" -> Route.Gallery
            "result" -> if (parts.size == 3) Route.Result(parts[2], fromSaved = parts[1] == "1") else null
            "compare" -> parts.getOrNull(1)
                ?.split(",")
                ?.filter { it.isNotEmpty() }
                ?.takeIf { it.isNotEmpty() }
                ?.let { Route.Compare(it) }
            else -> null
        }
    }
}
```

- [ ] **Step 5: 통과 확인**

Run: `./gradlew :app:testDebugUnitTest --tests "com.car.autoverdict.ui.navigation.RouteCodecTest"`
Expected: 4 tests PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/com/car/autoverdict/ui/navigation/MainTab.kt app/src/main/kotlin/com/car/autoverdict/ui/navigation/Route.kt app/src/main/kotlin/com/car/autoverdict/ui/navigation/RouteCodec.kt app/src/test/kotlin/com/car/autoverdict/ui/navigation/RouteCodecTest.kt
git commit -m "feat(nav): add MainTab, Route destinations and string codec"
```

---

### Task 16: `AppNavigator` 리듀서

**Files:**
- Create: `app/src/main/kotlin/com/car/autoverdict/ui/navigation/AppNavigator.kt`
- Test: `app/src/test/kotlin/com/car/autoverdict/ui/navigation/AppNavigatorTest.kt`

스펙 7.2절 이벤트 표의 모든 행을 테스트로 고정한다.

- [ ] **Step 1: 실패하는 테스트 작성**

`AppNavigatorTest.kt`:
```kotlin
package com.car.autoverdict.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigatorTest {

    private val url = "https://fem.encar.com/cars/detail/41623743"
    private val otherUrl = "https://fem.encar.com/cars/detail/99999999"

    private fun state(vararg routes: Route, pending: String? = null) = NavState(routes.toList(), pending)

    private fun reduce(s: NavState, e: NavEvent) = AppNavigator.reduce(s, e)

    // 앱 시작
    @Test fun `initial stack depends on onboarding completion`() {
        assertEquals(listOf(Route.Onboarding), AppNavigator.initial(onboardingComplete = false).stack)
        assertEquals(listOf(Route.Home), AppNavigator.initial(onboardingComplete = true).stack)
    }

    // 탭
    @Test fun `selecting another tab replaces the stack with that root`() {
        val r = reduce(state(Route.Home), NavEvent.SelectTab(MainTab.Saved))
        assertEquals(listOf(Route.Saved), r.state.stack)
        assertEquals(NavTransition.Replace, r.transition)
    }

    @Test fun `reselecting the current tab changes nothing`() {
        val s = state(Route.Saved)
        val r = reduce(s, NavEvent.SelectTab(MainTab.Saved))
        assertEquals(s, r.state)
        assertEquals(NavTransition.None, r.transition)
    }

    // push / pop
    @Test fun `open pushes the route`() {
        val r = reduce(state(Route.Home), NavEvent.Open(Route.Settings))
        assertEquals(listOf(Route.Home, Route.Settings), r.state.stack)
        assertEquals(NavTransition.Push, r.transition)
    }

    @Test fun `back pops when the stack has more than one route`() {
        val r = reduce(state(Route.Home, Route.Settings, Route.Licenses), NavEvent.Back)
        assertEquals(listOf(Route.Home, Route.Settings), r.state.stack)
        assertEquals(NavTransition.Pop, r.transition)
        assertTrue(r.handled)
    }

    @Test fun `back on saved tab goes to home tab`() {
        val r = reduce(state(Route.Saved), NavEvent.Back)
        assertEquals(listOf(Route.Home), r.state.stack)
        assertEquals(NavTransition.Replace, r.transition)
    }

    @Test fun `back on home or first-run onboarding is left to the system`() {
        assertFalse(reduce(state(Route.Home), NavEvent.Back).handled)
        assertFalse(reduce(state(Route.Onboarding), NavEvent.Back).handled)
    }

    @Test fun `canHandleBack mirrors the back rules`() {
        assertFalse(AppNavigator.canHandleBack(state(Route.Home)))
        assertFalse(AppNavigator.canHandleBack(state(Route.Onboarding)))
        assertTrue(AppNavigator.canHandleBack(state(Route.Saved)))
        assertTrue(AppNavigator.canHandleBack(state(Route.Home, Route.Settings)))
    }

    // 공유
    @Test fun `valid share after onboarding opens home then result`() {
        val r = reduce(state(Route.Saved, Route.Compare(listOf("1", "2"))), NavEvent.OpenShared(url))
        assertEquals(listOf(Route.Home, Route.Result(url, fromSaved = false)), r.state.stack)
        assertEquals(NavTransition.Replace, r.transition)
    }

    @Test fun `valid share during first-run onboarding is kept as pending`() {
        val r = reduce(state(Route.Onboarding), NavEvent.OpenShared(url))
        assertEquals(listOf(Route.Onboarding), r.state.stack)
        assertEquals(url, r.state.pendingSharedUrl)
        assertEquals(NavTransition.None, r.transition)
    }

    @Test fun `invalid share keeps the stack and asks for a toast`() {
        val s = state(Route.Home, Route.Settings)
        val r = reduce(s, NavEvent.InvalidShare)
        assertEquals(s, r.state)
        assertEquals(NavMessage.InvalidShare, r.message)
    }

    // 온보딩 완료
    @Test fun `finishing first-run onboarding with a pending share opens it`() {
        val r = reduce(state(Route.Onboarding, pending = url), NavEvent.OnboardingFinished)
        assertEquals(listOf(Route.Home, Route.Result(url, fromSaved = false)), r.state.stack)
        assertNull(r.state.pendingSharedUrl)
        assertEquals(NavTransition.Replace, r.transition)
    }

    @Test fun `finishing first-run onboarding without a pending share goes home`() {
        val r = reduce(state(Route.Onboarding), NavEvent.OnboardingFinished)
        assertEquals(listOf(Route.Home), r.state.stack)
    }

    @Test fun `finishing a replayed onboarding pops back to settings`() {
        val r = reduce(state(Route.Home, Route.Settings, Route.Onboarding), NavEvent.OnboardingFinished)
        assertEquals(listOf(Route.Home, Route.Settings), r.state.stack)
        assertEquals(NavTransition.Pop, r.transition)
    }

    @Test fun `finishing onboarding when it is no longer on top does nothing`() {
        val s = state(Route.Home, Route.Settings)
        val r = reduce(s, NavEvent.OnboardingFinished)
        assertEquals(s, r.state)
        assertEquals(NavTransition.None, r.transition)
    }

    // 중복 Result
    @Test fun `opening the same car twice does not stack a second result`() {
        val s = state(Route.Home, Route.Result(url, fromSaved = false))
        val r = reduce(s, NavEvent.Open(Route.Result("$url?utm_source=app", fromSaved = true)))
        assertEquals(s, r.state)
        assertEquals(NavTransition.None, r.transition)
    }

    @Test fun `opening a different car pushes`() {
        val s = state(Route.Home, Route.Result(url, fromSaved = false))
        val r = reduce(s, NavEvent.Open(Route.Result(otherUrl, fromSaved = false)))
        assertEquals(3, r.state.stack.size)
        assertEquals(NavTransition.Push, r.transition)
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :app:testDebugUnitTest --tests "com.car.autoverdict.ui.navigation.AppNavigatorTest"`
Expected: 컴파일 실패 — `Unresolved reference: NavState`.

- [ ] **Step 3: `AppNavigator.kt` 구현**

```kotlin
package com.car.autoverdict.ui.navigation

import com.car.autoverdict.util.EncarUrl

/** Back stack plus a share link received before first-run onboarding finished. */
data class NavState(
    val stack: List<Route>,
    val pendingSharedUrl: String? = null,
) {
    init {
        require(stack.isNotEmpty()) { "Back stack must never be empty" }
    }

    val top: Route get() = stack.last()
}

sealed interface NavEvent {
    data class Open(val route: Route) : NavEvent
    data object Back : NavEvent
    data class SelectTab(val tab: MainTab) : NavEvent
    data class OpenShared(val url: String) : NavEvent
    data object InvalidShare : NavEvent
    data object OnboardingFinished : NavEvent
}

/** How AppRoot should animate the change. Replace and None render without animation. */
enum class NavTransition { None, Push, Pop, Replace }

enum class NavMessage { InvalidShare }

data class NavResult(
    val state: NavState,
    val transition: NavTransition,
    /** False when the event should fall through to the system (back on a root). */
    val handled: Boolean = true,
    val message: NavMessage? = null,
)

/** Pure navigation reducer — every rule in spec 7.2's event table lives here. */
object AppNavigator {

    fun initial(onboardingComplete: Boolean): NavState =
        NavState(listOf(if (onboardingComplete) Route.Home else Route.Onboarding))

    fun canHandleBack(state: NavState): Boolean =
        state.stack.size > 1 || state.top == Route.Saved

    fun reduce(state: NavState, event: NavEvent): NavResult = when (event) {
        is NavEvent.Open -> open(state, event.route)
        NavEvent.Back -> back(state)
        is NavEvent.SelectTab -> selectTab(state, event.tab)
        is NavEvent.OpenShared -> openShared(state, event.url)
        NavEvent.InvalidShare -> NavResult(state, NavTransition.None, message = NavMessage.InvalidShare)
        NavEvent.OnboardingFinished -> finishOnboarding(state)
    }

    private fun open(state: NavState, route: Route): NavResult {
        val top = state.top
        if (route is Route.Result && top is Route.Result && sameCar(route.url, top.url)) {
            return NavResult(state, NavTransition.None)
        }
        return NavResult(state.copy(stack = state.stack + route), NavTransition.Push)
    }

    private fun back(state: NavState): NavResult = when {
        state.stack.size > 1 -> NavResult(state.copy(stack = state.stack.dropLast(1)), NavTransition.Pop)
        state.top == Route.Saved -> NavResult(state.copy(stack = listOf(Route.Home)), NavTransition.Replace)
        else -> NavResult(state, NavTransition.None, handled = false)
    }

    private fun selectTab(state: NavState, tab: MainTab): NavResult {
        val root = tab.route
        return if (state.stack == listOf(root)) {
            NavResult(state, NavTransition.None)
        } else {
            NavResult(state.copy(stack = listOf(root)), NavTransition.Replace)
        }
    }

    private fun openShared(state: NavState, url: String): NavResult =
        if (isFirstRunOnboarding(state)) {
            NavResult(state.copy(pendingSharedUrl = url), NavTransition.None)
        } else {
            NavResult(
                state.copy(stack = listOf(Route.Home, Route.Result(url, fromSaved = false))),
                NavTransition.Replace,
            )
        }

    private fun finishOnboarding(state: NavState): NavResult {
        // A stale callback (e.g. a double tap while the screen animates out) must not pop anything else.
        if (state.top != Route.Onboarding) return NavResult(state, NavTransition.None)
        if (!isFirstRunOnboarding(state)) {
            return NavResult(state.copy(stack = state.stack.dropLast(1)), NavTransition.Pop)
        }
        val pending = state.pendingSharedUrl
        val stack = if (pending != null) {
            listOf(Route.Home, Route.Result(pending, fromSaved = false))
        } else {
            listOf(Route.Home)
        }
        return NavResult(NavState(stack), NavTransition.Replace)
    }

    private fun isFirstRunOnboarding(state: NavState) = state.stack == listOf(Route.Onboarding)

    private fun sameCar(a: String, b: String): Boolean {
        val idA = EncarUrl.extractCarId(a)
        return if (idA != null) idA == EncarUrl.extractCarId(b) else a == b
    }
}
```

- [ ] **Step 4: 통과 확인**

Run: `./gradlew :app:testDebugUnitTest --tests "com.car.autoverdict.ui.navigation.AppNavigatorTest"`
Expected: 17 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/car/autoverdict/ui/navigation/AppNavigator.kt app/src/test/kotlin/com/car/autoverdict/ui/navigation/AppNavigatorTest.kt
git commit -m "feat(nav): add pure AppNavigator reducer covering spec 7.2 rules"
```

---

### Task 17: `NavState` 저장(`NavStateSaver`)

**Files:**
- Modify: `app/src/main/kotlin/com/car/autoverdict/ui/navigation/RouteCodec.kt` (파일 끝에 추가)
- Test: `app/src/test/kotlin/com/car/autoverdict/ui/navigation/NavStateCodecTest.kt`

- [ ] **Step 1: 실패하는 테스트 작성**

`NavStateCodecTest.kt`:
```kotlin
package com.car.autoverdict.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavStateCodecTest {

    private val url = "https://fem.encar.com/cars/detail/41623743"

    @Test fun `state with pending url round trips`() {
        val s = NavState(listOf(Route.Onboarding), pendingSharedUrl = url)
        assertEquals(s, NavStateCodec.decode(NavStateCodec.encode(s)))
    }

    @Test fun `state without pending url round trips`() {
        val s = NavState(listOf(Route.Home, Route.Result(url, fromSaved = true)))
        assertEquals(s, NavStateCodec.decode(NavStateCodec.encode(s)))
    }

    @Test fun `unreadable saved state decodes to null so the initial state is used`() {
        assertNull(NavStateCodec.decode(listOf("")))
        assertNull(NavStateCodec.decode(listOf("", "garbage")))
        assertNull(NavStateCodec.decode(listOf("", "home", "garbage")))
        assertNull(NavStateCodec.decode(emptyList()))
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :app:testDebugUnitTest --tests "com.car.autoverdict.ui.navigation.NavStateCodecTest"`
Expected: 컴파일 실패 — `Unresolved reference: NavStateCodec`.

- [ ] **Step 3: `RouteCodec.kt` 끝에 추가**

`package` 줄 바로 다음에 추가(이 파일에는 아직 import가 없다):
```kotlin
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
```
파일 끝에 추가:
```kotlin
/** Saved form: first element is the pending shared url ("" if none), then encoded routes. */
object NavStateCodec {

    fun encode(state: NavState): List<String> =
        listOf(state.pendingSharedUrl.orEmpty()) + state.stack.map(RouteCodec::encode)

    fun decode(saved: List<String>): NavState? {
        if (saved.isEmpty()) return null
        // Any unreadable route invalidates the whole stack, so we never restore e.g. [Settings] without its root.
        val stack = saved.drop(1).map { RouteCodec.decode(it) ?: return null }
        if (stack.isEmpty()) return null
        return NavState(stack, pendingSharedUrl = saved.first().ifEmpty { null })
    }
}

/** rememberSaveable saver for the navigation state (no Parcelize plugin needed). */
val NavStateSaver: Saver<NavState, Any> = listSaver(
    save = { NavStateCodec.encode(it) },
    restore = { NavStateCodec.decode(it) },
)
```

- [ ] **Step 4: 통과 확인**

Run: `./gradlew :app:testDebugUnitTest --tests "com.car.autoverdict.ui.navigation.NavStateCodecTest"`
Expected: 3 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/car/autoverdict/ui/navigation/RouteCodec.kt app/src/test/kotlin/com/car/autoverdict/ui/navigation/NavStateCodecTest.kt
git commit -m "feat(nav): add NavState saver for rememberSaveable"
```

---

### Task 18: `ShareIntentParser`

**Files:**
- Create: `app/src/main/kotlin/com/car/autoverdict/ui/navigation/ShareIntentParser.kt`
- Test: `app/src/test/kotlin/com/car/autoverdict/ui/navigation/ShareIntentParserTest.kt`

- [ ] **Step 1: 실패하는 테스트 작성**

`ShareIntentParserTest.kt`:
```kotlin
package com.car.autoverdict.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShareIntentParserTest {

    private val send = "android.intent.action.SEND"
    private val canonical = "https://fem.encar.com/cars/detail/41623743"

    @Test fun `encar share text is normalized to the canonical detail url`() {
        val text = "[엔카] 현대 그랜저 IG 2.4 https://fem.encar.com/cars/detail/41623743?utm_source=share 확인해보세요"
        assertEquals(NavEvent.OpenShared(canonical), ShareIntentParser.parse(send, "text/plain", text, isRestored = false))
    }

    @Test fun `non-encar text is an invalid share`() {
        assertEquals(NavEvent.InvalidShare, ShareIntentParser.parse(send, "text/plain", "https://google.com", isRestored = false))
    }

    @Test fun `missing text is ignored`() {
        assertNull(ShareIntentParser.parse(send, "text/plain", null, isRestored = false))
    }

    @Test fun `other actions and types are ignored`() {
        assertNull(ShareIntentParser.parse("android.intent.action.MAIN", null, null, isRestored = false))
        assertNull(ShareIntentParser.parse(send, "image/png", canonical, isRestored = false))
    }

    @Test fun `restored activity never re-handles the launch intent`() {
        assertNull(ShareIntentParser.parse(send, "text/plain", canonical, isRestored = true))
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :app:testDebugUnitTest --tests "com.car.autoverdict.ui.navigation.ShareIntentParserTest"`
Expected: 컴파일 실패 — `Unresolved reference: ShareIntentParser`.

- [ ] **Step 3: 구현**

```kotlin
package com.car.autoverdict.ui.navigation

import com.car.autoverdict.util.EncarUrl

/**
 * Turns a share intent into a navigation event. Pure so it can be unit tested; the
 * activity passes `isRestored = savedInstanceState != null` so rotation or process
 * restore never replays the launch intent over the restored back stack (spec 7.2).
 */
object ShareIntentParser {

    private const val ACTION_SEND = "android.intent.action.SEND"
    private const val TYPE_TEXT = "text/plain"

    fun parse(action: String?, type: String?, text: String?, isRestored: Boolean): NavEvent? {
        if (isRestored) return null
        if (action != ACTION_SEND || type != TYPE_TEXT) return null
        if (text == null) return null
        val carId = EncarUrl.extractCarId(text) ?: return NavEvent.InvalidShare
        return NavEvent.OpenShared(EncarUrl.buildDetailUrl(carId))
    }
}
```

- [ ] **Step 4: 통과 확인**

Run: `./gradlew :app:testDebugUnitTest --tests "com.car.autoverdict.ui.navigation.ShareIntentParserTest"`
Expected: 5 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/car/autoverdict/ui/navigation/ShareIntentParser.kt app/src/test/kotlin/com/car/autoverdict/ui/navigation/ShareIntentParserTest.kt
git commit -m "feat(nav): add pure ShareIntentParser with url normalization"
```

---

## Chunk 5: 앱 셸과 기존 화면 연결

이 Chunk는 화면 **내용**을 바꾸지 않는다(스펙 7.3). 바꾸는 것은 상단바, 화면 간 연결, 설정 화면의 예외 항목뿐이다. 화면 본문의 색 상수 치환은 Chunk 6에서 한다. 각 Task 뒤 `./gradlew :app:testDebugUnitTest`가 통과해야 한다. 수정으로 쓰이지 않게 된 import는 지운다(컴파일러 경고 `Unused import`로 확인).

### Task 19: 오픈소스 라이선스 화면

**Files:**
- Create: `app/src/main/kotlin/com/car/autoverdict/ui/screen/LicensesScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: 문자열 추가**

`strings.xml`의 `</resources>` 위에:
```xml
    <string name="licenses_title">오픈소스 라이선스</string>
    <string name="license_pretendard_notice">Pretendard는 SIL Open Font License 1.1에 따라 사용합니다.</string>
    <string name="license_material_notice">Material Symbols 아이콘은 Apache License 2.0에 따라 사용합니다.</string>
    <string name="license_show_full">전문 보기</string>
    <string name="license_hide_full">접기</string>
```
(토스페이스는 제외됐으므로 고지하지 않는다.)

- [ ] **Step 2: `LicensesScreen.kt` 구현**

```kotlin
package com.car.autoverdict.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.ui.components.AvButton
import com.car.autoverdict.ui.components.AvButtonSize
import com.car.autoverdict.ui.components.AvButtonStyle
import com.car.autoverdict.ui.components.AvSectionDivider
import com.car.autoverdict.ui.components.AvTopBar
import com.car.autoverdict.ui.theme.AvTheme

/** Notices for assets added in sub-project 0 (spec 5). Full OSS list is sub-project 4. */
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val oflText = remember { readRaw(context, R.raw.license_pretendard_ofl) }
    val apacheText = remember { readRaw(context, R.raw.license_apache_2_0) }

    Column(modifier = Modifier.fillMaxSize().background(AvTheme.colors.background)) {
        AvTopBar(onBack = onBack, title = stringResource(R.string.licenses_title))
        LazyColumn(modifier = Modifier.weight(1f)) {
            item { LicenseSection(title = "Pretendard", notice = stringResource(R.string.license_pretendard_notice), fullText = oflText) }
            item { AvSectionDivider() }
            item { LicenseSection(title = "Material Symbols", notice = stringResource(R.string.license_material_notice), fullText = apacheText) }
            item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
        }
    }
}

@Composable
private fun LicenseSection(
    title: String,
    notice: String,
    fullText: String,
) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)) {
        Text(text = title, style = typography.body1, color = colors.textPrimary)
        Spacer(Modifier.height(4.dp))
        Text(text = notice, style = typography.body2, color = colors.textSecondary)
        Spacer(Modifier.height(12.dp))
        AvButton(
            text = stringResource(if (expanded) R.string.license_hide_full else R.string.license_show_full),
            onClick = { expanded = !expanded },
            style = AvButtonStyle.Secondary,
            size = AvButtonSize.Small,
        )
        if (expanded) {
            Spacer(Modifier.height(12.dp))
            Text(text = fullText, style = typography.caption1, color = colors.textSecondary)
        }
    }
}

private fun readRaw(context: android.content.Context, resId: Int): String =
    context.resources.openRawResource(resId).bufferedReader().use { it.readText() }
```

- [ ] **Step 3: 빌드 확인**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/car/autoverdict/ui/screen/LicensesScreen.kt app/src/main/res/values/strings.xml
git commit -m "feat(ui): add open-source licenses screen for bundled fonts and icons"
```

---

### Task 20: 탭 루트 화면 상단바 (`AnalyzeScreen`, `SavedListScreen`)

**Files:**
- Modify: `app/src/main/kotlin/com/car/autoverdict/ui/screen/AnalyzeScreen.kt:90-142`
- Modify: `app/src/main/kotlin/com/car/autoverdict/ui/screen/SavedListScreen.kt:111-137`

과도기 메모: `MainActivity`의 기존 `Scaffold`가 인셋을 소비(`consumeWindowInsets`)하므로 이 Task 직후에도 상단 여백이 이중으로 생기지 않는다. 새 셸은 Task 22~23에서 붙는다.

- [ ] **Step 1: `AnalyzeScreen` 시그니처에 설정 콜백 추가**

```kotlin
fun AnalyzeScreen(
    modifier: Modifier = Modifier,
    lastAnalyzedUrl: String? = null,
    onOpenSettings: () -> Unit = {},
    onAnalyze: (String) -> Unit,
) {
```

- [ ] **Step 2: `AnalyzeScreen` 상단바 교체와 IME 패딩**

루트 `Column`의 modifier에 `.imePadding()`을 추가하고(`.background(Background)` 다음), `TopAppBar(...)` 블록(현재 130~142행) 전체를 다음으로 바꾼다.
```kotlin
        AvTopBar(
            onBack = null,
            title = "AutoVerdict",
            actions = {
                AvIconButton(
                    icon = Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.cd_settings),
                    onClick = onOpenSettings,
                )
            },
        )
```
추가 import:
```kotlin
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.res.stringResource
import com.car.autoverdict.R
import com.car.autoverdict.ui.components.AvIconButton
import com.car.autoverdict.ui.components.AvTopBar
```
`TopAppBar`, `TopAppBarDefaults`, `WindowInsets` import는 지운다.

- [ ] **Step 3: `SavedListScreen` 상단바 교체**

`TopAppBar(...)` 블록(현재 111~137행) 전체를 다음으로 바꾼다. 제목은 탭 이름과 같은 "저장한 차"(스펙 7.3).
```kotlin
            AvTopBar(
                onBack = null,
                title = "저장한 차",
                actions = {
                    if (savedCars.size >= 2) {
                        AvIconButton(
                            icon = if (compareMode) Icons.Outlined.Close else Icons.AutoMirrored.Outlined.CompareArrows,
                            contentDescription = if (compareMode) "비교 취소" else "비교 모드",
                            onClick = {
                                compareMode = !compareMode
                                if (!compareMode) selectedCarIds = emptySet()
                            },
                            tint = if (compareMode) Danger else Primary,
                        )
                    }
                },
            )
```
추가 import: `com.car.autoverdict.ui.components.AvIconButton`, `com.car.autoverdict.ui.components.AvTopBar`. 쓰이지 않게 된 `TopAppBar`, `TopAppBarDefaults`, `IconButton`, `WindowInsets` import는 지운다(다른 곳에서 쓰면 남긴다). `tint`의 `Danger`/`Primary`는 Task 24에서 토큰으로 바뀐다.

- [ ] **Step 4: 빌드·테스트 확인**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/car/autoverdict/ui/screen/AnalyzeScreen.kt app/src/main/kotlin/com/car/autoverdict/ui/screen/SavedListScreen.kt
git commit -m "feat(ui): use AvTopBar on home and saved tab roots"
```

---

### Task 21: 서브화면 상단바 (`ResultScreen`, `CompareScreen`, `PrivacyPolicyScreen`, `SettingsScreen`)

**Files:**
- Modify: `app/src/main/kotlin/com/car/autoverdict/ui/screen/ResultScreen.kt:73-99,157-164,196-254`
- Modify: `app/src/main/kotlin/com/car/autoverdict/ui/screen/CompareScreen.kt:93-122`
- Modify: `app/src/main/kotlin/com/car/autoverdict/ui/screen/PrivacyPolicyScreen.kt:92-129`
- Modify: `app/src/main/kotlin/com/car/autoverdict/ui/screen/SettingsScreen.kt:65-97,159-209`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: `ResultScreen` — 뒤로 콜백을 항상 최신으로**

`val scope = rememberCoroutineScope()` 다음 줄에 추가:
```kotlin
    // The bridge is created once inside remember{}; read onBack through updated state so
    // "close" always pops the current back stack (spec 7.2).
    val currentOnBack by rememberUpdatedState(onBack)
```
`NativeBridge(...)`의 `onClose = onBack,`을 `onClose = { currentOnBack() },`로 바꾼다. import `androidx.compose.runtime.rememberUpdatedState` 추가.

- [ ] **Step 2: `ResultScreen` — 상단바 교체**

`ResultTopBar(...)` 호출(157~164행)을 다음으로 바꾼다.
```kotlin
        AvTopBar(
            onBack = onBack,
            title = "분석 결과",
            subtitle = if (isAlreadySaved) "저장된 매물" else null,
            actions = {
                OpenInEncarChip(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    },
                )
            },
        )
```
그리고 `private fun ResultTopBar(...)` 전체(196~254행)를 다음으로 바꾼다. 칩의 시각은 그대로 두고 터치 영역만 48dp로 넓힌다.
```kotlin
@Composable
private fun OpenInEncarChip(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .avPressable(onClick = onClick)
            .minimumInteractiveComponentSize(),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Primary.copy(alpha = 0.1f))
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "엔카에서 보기",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Primary,
            )
        }
    }
}
```
추가 import: `androidx.compose.material3.minimumInteractiveComponentSize`, `com.car.autoverdict.ui.components.AvTopBar`, `com.car.autoverdict.ui.components.avPressable`. 쓰이지 않게 된 `IconButton`, `ArrowBack`, `WindowInsets`, `statusBars`, `windowInsetsPadding`, `clickable` import는 지운다. 루트 `Column`의 `.navigationBarsPadding()`은 유지한다(결과 화면 하단 영역이 내비바 인셋을 맡는다 — 스펙 6절 인셋 표).

- [ ] **Step 3: `CompareScreen` 상단바 교체**

상단의 수제 `Row(...) { IconButton(...) … Column { "비교 분석" … } }` 블록(93~122행)을 다음으로 바꾼다.
```kotlin
        AvTopBar(
            onBack = onBack,
            title = "비교 분석",
            subtitle = if (cars.isNotEmpty()) "2대 매물을 한눈에" else null,
        )
```
import `com.car.autoverdict.ui.components.AvTopBar` 추가. 쓰이지 않게 된 `IconButton`, `painterResource`, `WindowInsets`, `statusBars`, `windowInsetsPadding` import는 지운다.

- [ ] **Step 4: `PrivacyPolicyScreen` 상단바 교체**

`val scrollBehavior = …` 줄과 루트 `Column`의 `.nestedScroll(scrollBehavior.nestedScrollConnection)`을 지우고, `LargeTopAppBar(...)` 블록 전체를 다음으로 바꾼다(스크롤 접힘 동작은 의도적으로 없앤다 — 스펙 7.3).
```kotlin
        AvTopBar(onBack = onBack, title = "개인정보 처리방침")
```
`@OptIn(ExperimentalMaterial3Api::class)` 어노테이션은 다른 실험 API를 쓰지 않으면 지운다. import `com.car.autoverdict.ui.components.AvTopBar` 추가, `LargeTopAppBar`, `TopAppBarDefaults`, `IconButton`, `ArrowBack`, `nestedScroll`, `ExperimentalMaterial3Api` 중 쓰이지 않게 된 import는 지운다.

- [ ] **Step 5: `SettingsScreen` 문자열 추가**

`strings.xml`의 `</resources>` 위에:
```xml
    <string name="settings_licenses_title">오픈소스 라이선스</string>
    <string name="settings_licenses_subtitle">폰트·아이콘 라이선스 고지</string>
    <string name="settings_section_dev">개발</string>
    <string name="settings_gallery_title">컴포넌트 갤러리</string>
    <string name="settings_gallery_subtitle">debug 빌드 전용</string>
```

- [ ] **Step 6: `SettingsScreen` — 서브화면으로 전환 (스펙 7.3 예외 변경)**

시그니처를 다음으로 바꾼다. `onBack`의 기본값 `{}`은 `MainActivity`가 아직 탭으로 이 화면을 띄우는 동안만 필요하며 Task 23에서 지운다.
```kotlin
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onPrivacyPolicy: () -> Unit = {},
    onLicenses: () -> Unit = {},
    onReplayGuide: () -> Unit = {},
    onGallery: (() -> Unit)? = null,
) {
```
`TopAppBar(...)` 블록(85~97행)을 `AvTopBar(onBack = onBack, title = "설정")`로 바꾼다.

"정보" 그룹에서 "개인정보 처리방침" `SettingsRow` 바로 다음, `SettingsDivider()` + "버전" 행 앞에 추가:
```kotlin
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Outlined.Description,
                        iconTint = Primary,
                        iconBg = Primary.copy(alpha = 0.1f),
                        title = stringResource(R.string.settings_licenses_title),
                        subtitle = stringResource(R.string.settings_licenses_subtitle),
                        onClick = onLicenses,
                        trailing = {
                            Icon(
                                imageVector = Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                tint = TextSecondary,
                            )
                        },
                    )
```
`item { Spacer(Modifier.height(28.dp)) }`(푸터 앞) 바로 앞에 추가:
```kotlin
            if (onGallery != null) {
                item { Spacer(Modifier.height(16.dp)) }
                item { SectionHeader(stringResource(R.string.settings_section_dev)) }
                item {
                    SettingsGroup {
                        SettingsRow(
                            icon = Icons.Outlined.Palette,
                            iconTint = Primary,
                            iconBg = Primary.copy(alpha = 0.1f),
                            title = stringResource(R.string.settings_gallery_title),
                            subtitle = stringResource(R.string.settings_gallery_subtitle),
                            onClick = onGallery,
                            trailing = {
                                Icon(
                                    imageVector = Icons.Outlined.ChevronRight,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                )
                            },
                        )
                    }
                }
            }
```
추가 import: `androidx.compose.material.icons.outlined.Description`, `androidx.compose.material.icons.outlined.Palette`, `androidx.compose.ui.res.stringResource`, `com.car.autoverdict.R`, `com.car.autoverdict.ui.components.AvTopBar`. 쓰이지 않게 된 `TopAppBar`, `TopAppBarDefaults`, `WindowInsets` import는 지운다. (하단 배너는 탭 구조가 바뀌는 Task 23에서 붙인다. 지금 붙이면 기존 탭 배너와 겹친다.)

- [ ] **Step 7: 빌드·테스트 확인**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/kotlin/com/car/autoverdict/ui/screen app/src/main/res/values/strings.xml
git commit -m "feat(ui): use AvTopBar on sub-screens and turn settings into a sub-screen"
```

---

### Task 22: `AppRoot` 셸과 `AppScreenContent`

**Files:**
- Create: `app/src/main/kotlin/com/car/autoverdict/ui/navigation/AppRoot.kt`
- Create: `app/src/main/kotlin/com/car/autoverdict/ui/navigation/AppScreenContent.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/kotlin/com/car/autoverdict/ui/navigation/AppRootTest.kt`

- [ ] **Step 1: 문자열 추가**

`strings.xml`의 `</resources>` 위에:
```xml
    <string name="toast_invalid_share">엔카 매물 링크만 확인할 수 있어요</string>
```

- [ ] **Step 2: 실패하는 테스트 작성**

`AppRootTest.kt` — 실제 화면 대신 가짜 화면과 빈 광고 슬롯을 주입해 AdMob·WebView 초기화를 피한다(스펙 12절).
```kotlin
package com.car.autoverdict.ui.navigation

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.car.autoverdict.ui.theme.AvTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AppRootTest {

    @get:Rule val rule = createComposeRule()

    private val url = "https://fem.encar.com/cars/detail/41623743"

    @Composable
    private fun FakeScreen(route: Route, host: ScreenHost) {
        Column {
            Text("route:${RouteCodec.encode(route)}")
            Text("last:${host.lastAnalyzedUrl}")
            TextButton(onClick = { host.onAnalyzed(url); host.navigate(Route.Result(url, fromSaved = false)) }) { Text("open-result") }
            TextButton(onClick = host.back) { Text("go-back") }
            TextButton(onClick = host.finishOnboarding) { Text("finish-onboarding") }
        }
    }

    @Composable
    private fun Root(onboardingComplete: Boolean, pending: NavEvent?, onConsumed: () -> Unit) {
        AvTheme {
            AppRoot(
                onboardingComplete = onboardingComplete,
                onOnboardingComplete = {},
                pendingNavEvent = pending,
                onPendingNavEventConsumed = onConsumed,
                savedCount = 0,
                adBanner = {},
                screenContent = { route, host -> FakeScreen(route, host) },
            )
        }
    }

    @Test fun `back stack and last analyzed url survive recreation`() {
        val tester = StateRestorationTester(rule)
        tester.setContent { Root(onboardingComplete = true, pending = null, onConsumed = {}) }

        rule.onNodeWithText("open-result").performClick()
        rule.onNodeWithText("route:result|0|$url").assertExists()

        tester.emulateSavedInstanceStateRestore()

        rule.onNodeWithText("route:result|0|$url").assertExists()
        rule.onNodeWithText("go-back").performClick()
        rule.onNodeWithText("route:home").assertExists()
        rule.onNodeWithText("last:$url").assertExists()
    }

    @Test fun `share during first-run onboarding survives recreation and opens after finishing`() {
        var pending: NavEvent? by mutableStateOf(NavEvent.OpenShared(url))
        val tester = StateRestorationTester(rule)
        tester.setContent { Root(onboardingComplete = false, pending = pending, onConsumed = { pending = null }) }

        rule.onNodeWithText("route:onboarding").assertExists()
        tester.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("finish-onboarding").performClick()
        rule.onNodeWithText("route:result|0|$url").assertExists()
    }

    @Test fun `tab bar shows only on tab roots`() {
        rule.setContent { Root(onboardingComplete = true, pending = null, onConsumed = {}) }
        rule.onNodeWithText("홈").assertExists()
        rule.onNodeWithText("open-result").performClick()
        rule.onNodeWithText("홈").assertDoesNotExist()
    }

    @Test fun `invalid share shows a toast`() {
        val message = "엔카 매물 링크만 확인할 수 있어요"
        rule.setContent { Root(onboardingComplete = true, pending = NavEvent.InvalidShare, onConsumed = {}) }
        rule.waitUntil(timeoutMillis = 3_000) {
            rule.onAllNodesWithText(message).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
```

- [ ] **Step 3: 실패 확인**

Run: `./gradlew :app:testDebugUnitTest --tests "com.car.autoverdict.ui.navigation.AppRootTest"`
Expected: 컴파일 실패 — `Unresolved reference: AppRoot`.

- [ ] **Step 4: `AppRoot.kt` 구현**

```kotlin
package com.car.autoverdict.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.ui.components.AvTabBar
import com.car.autoverdict.ui.components.AvToastHost
import com.car.autoverdict.ui.components.LocalAvToast
import com.car.autoverdict.ui.components.rememberAvToastState
import com.car.autoverdict.ui.theme.AvTheme

/** What a screen may do to the app shell. Built by [AppRoot], consumed by [AppScreenContent]. */
data class ScreenHost(
    val navigate: (Route) -> Unit,
    val back: () -> Unit,
    val finishOnboarding: () -> Unit,
    /** Last link handed to analysis; the home screen skips re-filling it from the clipboard. */
    val lastAnalyzedUrl: String?,
    val onAnalyzed: (String) -> Unit,
)

/**
 * App shell: owns the back stack, toast host, tab bar and banner slot. Screens and the
 * banner are injected so tests can run without AdMob or WebView (spec 12).
 */
@Composable
fun AppRoot(
    onboardingComplete: Boolean,
    onOnboardingComplete: () -> Unit,
    pendingNavEvent: NavEvent?,
    onPendingNavEventConsumed: () -> Unit,
    savedCount: Int,
    adBanner: @Composable () -> Unit,
    screenContent: @Composable (Route, ScreenHost) -> Unit,
) {
    var navState by rememberSaveable(stateSaver = NavStateSaver) {
        mutableStateOf(AppNavigator.initial(onboardingComplete))
    }
    var transition by remember { mutableStateOf(NavTransition.None) }
    var lastAnalyzedUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var bottomChromeHeight by remember { mutableStateOf(0.dp) }
    val toast = rememberAvToastState()
    val density = LocalDensity.current
    val invalidShareMessage = stringResource(R.string.toast_invalid_share)

    fun dispatch(event: NavEvent) {
        if (event == NavEvent.OnboardingFinished) onOnboardingComplete()
        val result = AppNavigator.reduce(navState, event)
        navState = result.state
        transition = result.transition
        if (result.message == NavMessage.InvalidShare) {
            toast.show(invalidShareMessage)
        }
    }

    LaunchedEffect(pendingNavEvent) {
        if (pendingNavEvent != null) {
            dispatch(pendingNavEvent)
            onPendingNavEventConsumed()
        }
    }

    BackHandler(enabled = AppNavigator.canHandleBack(navState)) { dispatch(NavEvent.Back) }

    // Each screen gets a host bound to its own route. During a transition the outgoing screen
    // is still composed and tappable; its callbacks are ignored so a double tap can't pop twice.
    fun hostFor(route: Route) = ScreenHost(
        navigate = { if (navState.top == route) dispatch(NavEvent.Open(it)) },
        back = { if (navState.top == route) dispatch(NavEvent.Back) },
        finishOnboarding = { if (navState.top == route) dispatch(NavEvent.OnboardingFinished) },
        lastAnalyzedUrl = lastAnalyzedUrl,
        onAnalyzed = { lastAnalyzedUrl = it },
    )
    val top = navState.top
    val currentTab = top.tabOrNull()

    CompositionLocalProvider(LocalAvToast provides toast) {
        Box(modifier = Modifier.fillMaxSize().background(AvTheme.colors.background)) {
            Column(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = top,
                    modifier = Modifier
                        .weight(1f)
                        // Tab roots end above the banner + tab bar; consume that height so a
                        // screen's imePadding() only adds the part of the keyboard overlapping it.
                        .then(
                            if (currentTab != null) {
                                Modifier.consumeWindowInsets(PaddingValues(bottom = bottomChromeHeight))
                            } else {
                                Modifier
                            },
                        ),
                    transitionSpec = { transitionFor(transition) },
                    label = "route",
                ) { route ->
                    screenContent(route, hostFor(route))
                }
                // Outside AnimatedContent so switching tabs keeps the same AdView (spec 10).
                if (currentTab != null) {
                    Column(
                        modifier = Modifier.onSizeChanged {
                            bottomChromeHeight = with(density) { it.height.toDp() }
                        },
                    ) {
                        adBanner()
                        AvTabBar(
                            selected = currentTab,
                            onSelect = { dispatch(NavEvent.SelectTab(it)) },
                            savedCount = savedCount,
                        )
                    }
                }
            }
            AvToastHost(
                state = toast,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .then(toastOffset(currentTab != null, bottomChromeHeight)),
            )
        }
    }
}

private fun toastOffset(onTabRoot: Boolean, bottomChromeHeight: Dp): Modifier =
    if (onTabRoot) {
        Modifier.padding(bottom = bottomChromeHeight)
    } else {
        Modifier.navigationBarsPadding().padding(bottom = 16.dp)
    }

private fun AnimatedContentTransitionScope<Route>.transitionFor(transition: NavTransition): ContentTransform =
    when (transition) {
        NavTransition.Push ->
            slideInHorizontally(tween(250)) { it } togetherWith slideOutHorizontally(tween(250)) { -it / 4 }
        NavTransition.Pop ->
            (slideInHorizontally(tween(250)) { -it / 4 } togetherWith slideOutHorizontally(tween(250)) { it })
                .apply { targetContentZIndex = -1f }
        NavTransition.None, NavTransition.Replace ->
            EnterTransition.None togetherWith ExitTransition.None
    }
```

- [ ] **Step 5: `AppScreenContent.kt` 구현**

```kotlin
package com.car.autoverdict.ui.navigation

import androidx.compose.runtime.Composable
import com.car.autoverdict.BuildConfig
import com.car.autoverdict.ui.gallery.ComponentGalleryScreen
import com.car.autoverdict.ui.screen.AnalyzeScreen
import com.car.autoverdict.ui.screen.CompareScreen
import com.car.autoverdict.ui.screen.LicensesScreen
import com.car.autoverdict.ui.screen.OnboardingScreen
import com.car.autoverdict.ui.screen.PrivacyPolicyScreen
import com.car.autoverdict.ui.screen.ResultScreen
import com.car.autoverdict.ui.screen.SavedListScreen
import com.car.autoverdict.ui.screen.SettingsScreen

/** Maps each [Route] to its real screen. Kept apart from AppRoot so tests can inject fakes. */
@Composable
fun AppScreenContent(route: Route, host: ScreenHost) {
    when (route) {
        Route.Onboarding -> OnboardingScreen(onComplete = host.finishOnboarding)
        Route.Home -> AnalyzeScreen(
            lastAnalyzedUrl = host.lastAnalyzedUrl,
            onOpenSettings = { host.navigate(Route.Settings) },
            onAnalyze = { url ->
                host.onAnalyzed(url)
                host.navigate(Route.Result(url, fromSaved = false))
            },
        )
        Route.Saved -> SavedListScreen(
            onCarClick = { url -> host.navigate(Route.Result(url, fromSaved = true)) },
            onCompare = { ids -> host.navigate(Route.Compare(ids)) },
        )
        Route.Settings -> SettingsScreen(
            onBack = host.back,
            onPrivacyPolicy = { host.navigate(Route.PrivacyPolicy) },
            onLicenses = { host.navigate(Route.Licenses) },
            onReplayGuide = { host.navigate(Route.Onboarding) },
            onGallery = if (BuildConfig.DEBUG) ({ host.navigate(Route.Gallery) }) else null,
        )
        Route.PrivacyPolicy -> PrivacyPolicyScreen(onBack = host.back)
        Route.Licenses -> LicensesScreen(onBack = host.back)
        // Debug-only: release builds never reach it, and R8 drops the gallery code (spec 6).
        Route.Gallery -> if (BuildConfig.DEBUG) ComponentGalleryScreen(onBack = host.back)
        is Route.Result -> ResultScreen(url = route.url, onBack = host.back, isAlreadySaved = route.fromSaved)
        is Route.Compare -> CompareScreen(carIds = route.carIds, onBack = host.back)
    }
}
```

- [ ] **Step 6: 통과 확인**

Run: `./gradlew :app:testDebugUnitTest --tests "com.car.autoverdict.ui.navigation.AppRootTest"`
Expected: 4 tests PASS. 토스트 테스트가 시간 초과로 실패하면 `rule.mainClock.autoAdvance = false`로 두고 `rule.mainClock.advanceTimeByFrame()`을 반복 호출하며 스낵바가 언제 나타나고 사라지는지 프레임 단위로 확인해 원인을 찾는다.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/kotlin/com/car/autoverdict/ui/navigation/AppRoot.kt app/src/main/kotlin/com/car/autoverdict/ui/navigation/AppScreenContent.kt app/src/main/res/values/strings.xml app/src/test/kotlin/com/car/autoverdict/ui/navigation/AppRootTest.kt
git commit -m "feat(nav): add AppRoot shell with saveable back stack, tab bar and toast host"
```

---

### Task 23: `MainActivity` 교체, 설정 배너, 정리

**Files:**
- Modify (전면 교체): `app/src/main/kotlin/com/car/autoverdict/MainActivity.kt`
- Modify: `app/src/main/kotlin/com/car/autoverdict/ui/screen/SettingsScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: `MainActivity.kt` 전면 교체**

```kotlin
package com.car.autoverdict

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.car.autoverdict.preferences.AppPreferences
import com.car.autoverdict.ui.AdBanner
import com.car.autoverdict.ui.navigation.AppRoot
import com.car.autoverdict.ui.navigation.AppScreenContent
import com.car.autoverdict.ui.navigation.NavEvent
import com.car.autoverdict.ui.navigation.ShareIntentParser
import com.car.autoverdict.ui.theme.AvTheme
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {

    /** One-shot event from a share intent, consumed by AppRoot once composed. */
    private val pendingNavEvent = mutableStateOf<NavEvent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // The only splash: onboarding state is a synchronous SharedPreferences read,
        // so no keep-on-screen condition is needed (spec 7.2).
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        val preferences = AppPreferences(this)
        val app = application as AutoVerdictApp
        pendingNavEvent.value = parseShare(intent, isRestored = savedInstanceState != null)

        setContent {
            AvTheme {
                val savedCount by remember {
                    app.database.savedCarDao().getAllFlow().map { it.size }
                }.collectAsState(initial = 0)
                AppRoot(
                    onboardingComplete = preferences.isOnboardingComplete,
                    onOnboardingComplete = preferences::setOnboardingComplete,
                    pendingNavEvent = pendingNavEvent.value,
                    onPendingNavEventConsumed = { pendingNavEvent.value = null },
                    savedCount = savedCount,
                    adBanner = { AdBanner() },
                    screenContent = { route, host -> AppScreenContent(route, host) },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingNavEvent.value = parseShare(intent, isRestored = false)
    }

    private fun parseShare(intent: Intent?, isRestored: Boolean): NavEvent? =
        ShareIntentParser.parse(
            action = intent?.action,
            type = intent?.type,
            text = intent?.getStringExtra(Intent.EXTRA_TEXT),
            isRestored = isRestored,
        )
}
```

- [ ] **Step 2: `SettingsScreen` — 기본값 제거와 하단 배너**

1. 시그니처에서 `onBack: () -> Unit = {},`를 `onBack: () -> Unit,`으로 바꾸고 첫 파라미터로 옮긴다(`modifier` 앞).
2. `LazyColumn(modifier = Modifier.fillMaxSize(), …)`의 modifier를 `Modifier.weight(1f).fillMaxWidth()`로 바꾼다.
3. `LazyColumn { … }` 닫는 괄호 다음(루트 `Column` 안 마지막)에 추가:
```kotlin
        // Settings used to be a tab with the shared banner; keep that exposure (spec 10).
        Box(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            AdBanner()
        }
```
추가 import: `androidx.compose.foundation.layout.navigationBarsPadding`, `com.car.autoverdict.ui.AdBanner`.

- [ ] **Step 3: 쓰지 않는 문자열 제거**

`strings.xml`에서 `tab_analyze`, `tab_settings` 줄을 지운다. 확인:
```bash
grep -rn "tab_analyze\|tab_settings\|AppScreen\.\|BrandSplashScreen\|AutoVerdictTheme" app/src/main/kotlin
```
Expected: 출력 없음.

- [ ] **Step 4: 빌드·테스트 확인**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: 에뮬레이터 스모크 확인**

"에뮬레이터 절차"로 `AV_API_36`을 띄우고 앱을 설치·실행한다. 확인 항목(각각 `"$ADB" exec-out screencap -p > /tmp/av-smoke-<n>.png` 캡처를 Read로 확인하거나 `"$ADB" exec-out uiautomator dump /dev/tty`로 텍스트 확인):
1. 스플래시가 한 번만(파란 배경) 보이고 홈 탭으로 온다(첫 실행이면 온보딩).
2. 홈 우상단 설정 아이콘 → 설정 화면 → 뒤로 → 홈.
3. 저장한 차 탭 → 시스템 뒤로(`"$ADB" shell input keyevent BACK`) → 홈 탭. 홈에서 다시 뒤로 → 앱이 백그라운드로.
4. **onCreate 경로:** 앱을 종료(`"$ADB" shell am force-stop com.kcurve.autoverdict`)한 뒤
   `"$ADB" shell am start -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT "[엔카] 그랜저 https://fem.encar.com/cars/detail/41623743" -n com.kcurve.autoverdict/com.car.autoverdict.MainActivity` → 결과 화면, 뒤로 → 홈 탭.
5. **onNewIntent 경로:** 앱이 떠 있는 상태에서 같은 명령에 `-f 0x20000000`(FLAG_ACTIVITY_SINGLE_TOP)을 붙여 실행 → 결과 화면. 같은 방식으로 `--es android.intent.extra.TEXT "hello"` → 현재 화면은 그대로 유지되고 토스트 "엔카 매물 링크만 확인할 수 있어요"가 뜬다.
6. 결과 화면에서 회전: `"$ADB" shell settings put system accelerometer_rotation 0` 후 `"$ADB" shell settings put system user_rotation 1` → 결과 화면 유지(원복: `user_rotation 0`).

실패 항목이 있으면 `systematic-debugging` 스킬로 원인을 찾고 고친 뒤 다시 확인한다.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/com/car/autoverdict/MainActivity.kt app/src/main/kotlin/com/car/autoverdict/ui/screen/SettingsScreen.kt app/src/main/res/values/strings.xml
git commit -m "feat(nav): route MainActivity through AppRoot and drop the brand splash"
```

---

## Chunk 6: 색 치환, 토스페이스 검증, 문서, 최종 검증

### 색 치환 규칙 (Task 24·25 공통)

스펙 4.5절 매핑표를 코드 패턴으로 풀어 쓴 것이다. **판단 기준은 "그 색이 무엇을 칠하는가"**다.

| 기존 | 쓰인 자리 | 새 토큰 |
|---|---|---|
| `Primary` | `.background(…)`, `containerColor =`, `border(…)`, 진행 바·`Box`·`Divider`의 `color =` (면을 칠함) | `colors.accent` |
| `Primary` | `Text`의 `color =`, `Icon`의 `tint =`, `textColor =` (글자·아이콘) | `colors.accentText` |
| `Primary.copy(alpha = …)` | 배경 틴트 | `colors.accentWeak` (alpha 제거) |
| `Background` | 어디든 | `colors.surfaceMuted` |
| `TextPrimary` | 어디든 | `colors.textPrimary` |
| `TextSecondary` | 어디든 | `colors.textSecondary` (`.copy(alpha = …)`가 붙어 있으면 그대로 유지) |
| `Danger` / `Warning` / `Success` | 글자·아이콘 | `colors.dangerText` / `warningText` / `successText` |
| `Danger` / `Warning` / `Success` | 면(`.background(…)` 등) | `colors.dangerFill` / `warningFill` / `successFill` |
| `Danger` / `Warning` / `Success` `.copy(alpha = …)` | 배경 틴트 | `colors.dangerBg` / `warningBg` / `successBg` (alpha 제거) |
| `DangerBg` / `WarningBg` / `SuccessBg` | 어디든 | `colors.dangerBg` / `warningBg` / `successBg` |
| `Border`, `Hairline` | 어디든 | `colors.border` (`.copy(alpha = …)`는 유지) |
| 점수 그라디언트 RGB(`COLOR_STOPS`, `scoreColor*`) | — | **그대로 둔다** (3번에서 재설계) |
| `MaterialTheme.colorScheme.*` | — | 그대로 둔다 (M3 매핑으로 이미 토큰 값) |

**절차 (파일마다):**
1. `import com.car.autoverdict.ui.theme.<기존 상수>` 줄을 모두 지우고 `import com.car.autoverdict.ui.theme.AvTheme`를 추가한다.
2. 색을 쓰는 각 `@Composable` 함수 맨 위에 `val colors = AvTheme.colors`를 두고 위 표대로 바꾼다. 람다(`drawBehind`, `remember {}` 등) 안에서는 이 `colors` 변수를 쓴다.
3. 색을 파라미터로 받는 비-컴포저블 헬퍼가 있으면 호출부(컴포저블)에서 토큰 값을 넘긴다.
4. 확인: `grep -nwE "Primary|PrimaryFocus|Background|TextPrimary|TextSecondary|Danger|DangerBg|Warning|WarningBg|Success|SuccessBg|Border|Hairline" <파일>` 결과에 기존 상수 참조가 남지 않아야 한다(`AvButtonStyle.Danger` 같은 새 API 이름, 문자열 리터럴 안의 단어는 예외).

### Task 24: 색 치환 — 홈·저장·온보딩·일러스트

**Files:**
- Modify: `app/src/main/kotlin/com/car/autoverdict/ui/screen/AnalyzeScreen.kt`
- Modify: `app/src/main/kotlin/com/car/autoverdict/ui/screen/SavedListScreen.kt`
- Modify: `app/src/main/kotlin/com/car/autoverdict/ui/screen/OnboardingScreen.kt`
- Modify: `app/src/main/kotlin/com/car/autoverdict/ui/components/BrowserShareIllustration.kt`
- Modify: `app/src/main/kotlin/com/car/autoverdict/ui/components/ShareSheetIllustration.kt`

- [ ] **Step 1: 다섯 파일에 위 규칙 적용**

파일별로 위 "절차"를 따른다. 대표 예:
- `SavedListScreen`의 상단바 액션 `tint = if (compareMode) Danger else Primary` → `tint = if (compareMode) colors.dangerText else colors.accentText`
- `SavedListScreen`의 FAB `containerColor = Primary` → `colors.accent`
- `AnalyzeScreen`의 에러 문구 `color = Danger` → `colors.dangerText`
- `.background(Background)` → `.background(colors.surfaceMuted)`

- [ ] **Step 2: 남은 참조 확인**

```bash
for f in AnalyzeScreen SavedListScreen OnboardingScreen; do grep -nwE "Primary|PrimaryFocus|Background|TextPrimary|TextSecondary|Danger|DangerBg|Warning|WarningBg|Success|SuccessBg|Border|Hairline" app/src/main/kotlin/com/car/autoverdict/ui/screen/$f.kt; done
grep -nwE "Primary|Background|TextPrimary|TextSecondary|Border|Hairline" app/src/main/kotlin/com/car/autoverdict/ui/components/*Illustration.kt
```
Expected: 기존 상수 참조 없음(예외는 절차 4 참고).

- [ ] **Step 3: 빌드·테스트 확인**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/car/autoverdict/ui/screen/AnalyzeScreen.kt app/src/main/kotlin/com/car/autoverdict/ui/screen/SavedListScreen.kt app/src/main/kotlin/com/car/autoverdict/ui/screen/OnboardingScreen.kt app/src/main/kotlin/com/car/autoverdict/ui/components/BrowserShareIllustration.kt app/src/main/kotlin/com/car/autoverdict/ui/components/ShareSheetIllustration.kt
git commit -m "refactor(ui): move home, saved, onboarding and illustrations to Av color tokens"
```

---

### Task 25: 색 치환 — 결과·비교·설정·방침, `Color.kt` 삭제

**Files:**
- Modify: `app/src/main/kotlin/com/car/autoverdict/ui/screen/ResultScreen.kt`
- Modify: `app/src/main/kotlin/com/car/autoverdict/ui/screen/CompareScreen.kt`
- Modify: `app/src/main/kotlin/com/car/autoverdict/ui/screen/SettingsScreen.kt`
- Modify: `app/src/main/kotlin/com/car/autoverdict/ui/screen/PrivacyPolicyScreen.kt`
- Delete: `app/src/main/kotlin/com/car/autoverdict/ui/theme/Color.kt`

- [ ] **Step 1: 네 파일에 규칙 적용**

대표 예:
- `ResultScreen`의 `OpenInEncarChip`: `.background(Primary.copy(alpha = 0.1f))` → `.background(colors.accentWeak)`, 아이콘 `tint = Primary`와 텍스트 `color = Primary` → `colors.accentText`
- `ResultScreen`의 `LoadingOverlay`: `.background(Primary.copy(alpha = 0.1f))` → `colors.accentWeak`, 아이콘·텍스트의 `Primary` → `colors.accentText`, 진행 바 `.background(Primary)` → `colors.accent`
- `CompareScreen`의 `CountCell(textColor = Danger, bgColor = DangerBg, …)` → `textColor = colors.dangerText, bgColor = colors.dangerBg` (Warning·Success도 같은 방식)
- `CompareScreen`의 `VerdictCell` `score >= 78 -> SuccessBg to Success` → `colors.successBg to colors.successText` (Warning·Danger도 같은 방식)
- `CompareScreen`의 스택 바 `.background(Danger)` → `colors.dangerFill` (Warning·Success도 같은 방식)
- `CompareScreen`의 `if (isBest) Modifier.background(Primary.copy(alpha = 0.07f))` → `colors.accentWeak`
- `SettingsScreen`의 `iconTint = Primary, iconBg = Primary.copy(alpha = 0.1f)` → `iconTint = colors.accentText, iconBg = colors.accentWeak`; `iconTint = Danger, iconBg = Danger.copy(alpha = 0.1f)` → `colors.dangerText`, `colors.dangerBg`; `titleColor = Danger` → `colors.dangerText`
- `SettingsRow`의 기본값 `titleColor: Color = TextPrimary` → `titleColor: Color = AvTheme.colors.textPrimary` (컴포저블 기본값이라 허용)

- [ ] **Step 1-2: 빌드·테스트 확인과 Commit (웨이브 4의 G는 여기까지)**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL` (`Color.kt`가 아직 있으므로 F의 파일과 무관하게 통과한다).
```bash
git add app/src/main/kotlin/com/car/autoverdict/ui/screen/ResultScreen.kt app/src/main/kotlin/com/car/autoverdict/ui/screen/CompareScreen.kt app/src/main/kotlin/com/car/autoverdict/ui/screen/SettingsScreen.kt app/src/main/kotlin/com/car/autoverdict/ui/screen/PrivacyPolicyScreen.kt
git commit -m "refactor(ui): move result, compare, settings and policy to Av color tokens"
```

- [ ] **Step 2: `Color.kt` 삭제와 전역 확인 (웨이브 4', F·G 병합 후)**

```bash
git rm app/src/main/kotlin/com/car/autoverdict/ui/theme/Color.kt
grep -rnE "import com\.car\.autoverdict\.ui\.theme\.(Primary|PrimaryFocus|Background|Surface|TextPrimary|TextSecondary|Danger|DangerBg|Warning|WarningBg|Success|SuccessBg|Border|Hairline)$" app/src/main/kotlin
```
Expected: 출력 없음.

- [ ] **Step 3: 빌드·테스트 확인**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`. 남은 참조가 있으면 컴파일 오류(`Unresolved reference: Primary` 등)로 드러나므로 규칙대로 고친다.

- [ ] **Step 4: Commit**

```bash
git add -A app/src/main/kotlin/com/car/autoverdict/ui
git commit -m "refactor(ui): drop raw Color.kt now that all screens use Av tokens"
```

---

### Task 26: 수동 QA

**Files:**
- (발견한 문제를 고치는 경우에만) 해당 화면·컴포넌트 파일

토스페이스 렌더링 검증은 Task 6A에서 끝났다. 여기서는 새 셸과 컴포넌트를 실제 기기 화면으로 확인한다.

- [ ] **Step 1: 수동 QA (API 36 전용 AVD `AV_API_36`)**

"에뮬레이터 절차"로 기기를 띄우고 앱을 설치·실행한다. 화면 이동은 `"$ADB" exec-out uiautomator dump /dev/tty`로 요소의 `bounds`를 찾아 `"$ADB" shell input tap X Y`로 한다.

다음을 확인하고 각각 캡처를 `/tmp/av-qa-*.png`로 남긴다.
1. 홈: 상단바(AutoVerdict + 설정 아이콘), 링크 입력 후 키보드를 열면 입력창이 키보드 위에 보이고 하단에 이중 여백이 없다.
2. 저장한 차: 상단바 "저장한 차", 저장 2건 이상이면 비교 아이콘.
3. 결과: 상단바 "분석 결과"(+ 저장된 매물이면 부제), "엔카에서 보기" 칩, 로딩 중 MREC, 로딩 후 하단 배너가 내비바와 겹치지 않는다.
4. 비교: 상단바 "비교 분석", 뒤로 아이콘.
5. 설정: 뒤로 아이콘, 오픈소스 라이선스 행, (debug) 컴포넌트 갤러리 행, 하단 배너.
6. 개인정보 처리방침, 오픈소스 라이선스(전문 보기/접기).
7. 온보딩: 설정 → 사용법 다시 보기 → 완료 시 설정으로 복귀.
8. 컴포넌트 갤러리(설정 → 컴포넌트 갤러리): 모든 섹션이 깨짐 없이 보이고, 버튼 눌림 축소, 시트 드래그 닫기, 다이얼로그, 토스트(액션 포함), 탭바 선택이 동작한다.
10. 버튼·리스트 행을 누르면 96% 축소 반응이 보이고, 하드웨어 키보드 Tab(`"$ADB" shell input keyevent TAB`)으로 이동하면 2dp 파란 포커스 링이 보인다.
9. 탭 루트 → 서브화면 push 전환 때 탭바·배너가 사라지며 화면이 튀는지 기록만 한다(0번에서는 허용, 1번에서 판단). 가로 모드 디스플레이 컷아웃 인셋은 0번 범위 밖으로 기록만 한다.

- [ ] **Step 2: 문제 수정과 Commit (있을 때만)**

문제가 있으면 `systematic-debugging` 스킬로 원인을 찾아 고치고, 빌드·테스트 통과 후 커밋한다.
```bash
git add -A app/src/main
git commit -m "fix(ui): address revamp QA findings"
```
문제가 없으면 커밋하지 않고 QA 결과(항목별 통과)를 보고한다.

---

### Task 27: `DESIGN.md`, `CLAUDE.md` 갱신

**Files:**
- Modify (전면 교체): `DESIGN.md`
- Modify: `CLAUDE.md` (Key Conventions)

- [ ] **Step 1: `DESIGN.md` 전면 교체**

````markdown
# AutoVerdict Design Language

Toss-style, light only. Rationale and the full decision record live in
`docs/superpowers/specs/2026-10-17-ux-revamp-0-design-language-design.md`.
This file is the day-to-day reference.

## Principles

- One message per screen: a large conclusion sentence first, evidence below.
- White page, grey scale for hierarchy, a single accent (Toss blue `#3182F6`).
- List rows and 8dp grey bands instead of bordered cards and shadows.
- Primary action in a fixed bottom CTA (`AvBottomCta`).
- Press feedback is a 96% scale (`Modifier.avPressable`), not a ripple.

## Tokens (`ui/theme`)

Read tokens through `AvTheme.colors / typography / shapes / spacing`. Screens never
use raw hex or `AvPalette`.

| Group | Tokens |
|---|---|
| Surfaces | `background` white · `surfaceMuted` grey100 · `surfaceSubtle` grey50 |
| Text | `textPrimary` · `textSecondary` · `textTertiary` · `textDisabled` (disabled only) |
| Accent | `accent` (fills) · `accentText` (accent-colored text/icons) · `accentWeak` · `accentPressed` · `onAccent` |
| Status | `danger*`, `warning*`, `success*`, `neutral*` — each `Fill` / `Text` / `Bg` |
| Other | `border` · `toastBg` · `toastAction` · `scrim` |

Contrast: all text pairs meet WCAG AA 4.5:1 (locked by `AvColorsContrastTest`) except
white on `accent` (Primary button labels only, SemiBold 15sp+) and placeholders on
`surfaceMuted`. Never put white text on `dangerFill`.

Verdict tones: `AvTone.fromVerdict()` — NEVER→Danger "피하세요", CAUTION→Warning "주의",
OK→Success "괜찮아요", UNKNOWN→Neutral "확인 필요".

Type (Pretendard): `title1` 26 Bold · `title2` 22 Bold · `title3` 19 SemiBold ·
`body1` 17 · `body2` 15 · `buttonL/M/S` 17/15/13 SemiBold · `caption1` 13 ·
`caption2` 11 SemiBold (minimum size). Only Regular, SemiBold and Bold are bundled — don't use Medium.

Shapes: chip pill · tile 12 · button S/M/L 10/14/16 · input 16 · dialog 24 · sheet top 24.
Spacing: 4 · 8 · 12 · 16 · 20 · 24 · 32; screen side padding 20.

## Components (`ui/components`)

`AvTopBar` (+`AvIconButton`) · `AvButton` (Primary/Weak/Secondary/Danger × L/M/S; Primary is
L/M only) · `AvBottomCta` · `AvListRow` · `AvTextField` · `AvBadge` · `AvFactTile` ·
`AvSectionHeader` · `AvSectionDivider` · `AvEmptyState` · `AvIllustrationIcon` ·
`AvBottomSheet` · `AvDialog` · `AvToastHost`/`LocalAvToast` · `AvTabBar` · `AvLoadingSteps`.
Debug builds show all of them under Settings → 컴포넌트 갤러리.

Insets: `AvTopBar` owns the status bar, `AvTabBar`/`AvBottomCta` own the navigation bar
(and IME for `AvBottomCta`). Don't add blanket insets at the screen root.

## Copy

- 해요체 everywhere; conclusion first.
- Say what the user can do, not what the app failed at ("엔카에 로그인하면 확인할 수 있어요",
  not "데이터 추출 실패").
- No English labels or developer terms in UI. Used-car domain terms (전손, 침수, 성능점검,
  프레임, 자차보험 공백) are used as-is; explanations go in detail sheets.
- Errors: what happened + what to do next.
- Buttons: verb, 2–3 words ("저장하기", "비교하기", "다시 시도").
- Numbers: "8.2만km", "2,180만원", "2022년 2월~9월".
- No emoji in UI text; use `AvIllustrationIcon` for pictures.
- New UI strings (components, navigation, new screens) go in `res/values/strings.xml`.
  Existing screen copy moves there when that screen is redesigned (sub-projects 1–4).
````

- [ ] **Step 2: `CLAUDE.md`의 Key Conventions 교체**

`## Key Conventions` 아래 첫 세 줄(Navigation / Screen-level state / Sub-screens)을 다음 네 줄로 바꾼다. 나머지 줄(EncarUrl, ProGuard)은 그대로 둔다.
```markdown
- UI follows the Toss-style design system in `DESIGN.md`: tokens via `AvTheme.colors/typography/shapes/spacing` (`ui/theme`), shared `Av*` components in `ui/components`. Screens never use raw colors.
- Navigation is a small saveable back stack, not a navigation library: `ui/navigation/Route` destinations, the pure `AppNavigator` reducer (unit-tested), and the `AppRoot` shell. Screens receive a `ScreenHost` and hold no navigation state.
- `MainActivity` only parses share intents (`ShareIntentParser`, skipped on restore) and hosts `AppRoot`; app-level state (back stack, last analyzed URL) lives in `AppRoot` via `rememberSaveable`.
- Two tabs (홈, 저장한 차); Settings is a sub-screen opened from the home top bar. The tab bar and banner render only on tab roots.
```

- [ ] **Step 3: Commit**

```bash
git add DESIGN.md CLAUDE.md
git commit -m "docs: replace Apple design analysis with Toss-style DESIGN.md and update conventions"
```

---

### Task 28: 최종 검증과 기록

**Files:**
- Modify: `docs/superpowers/specs/2026-10-17-ux-revamp-0-design-language-design.md` (5절 크기 기록)
- Modify: `docs/superpowers/plans/2026-10-17-ux-revamp-0-baseline.md`

- [ ] **Step 1: 전체 파이프라인**

`build-all.sh`는 존재하지 않는 `JAVA_HOME`(`/opt/homebrew/opt/openjdk@17`)을 강제로 설정하고 `npm ci`로 node_modules를 지우므로 쓰지 않는다. 같은 단계를 직접 실행한다.
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
(cd webview-bundle && npm run build && npx vitest run)
./gradlew :app:assembleDebug :app:testDebugUnitTest
```
Expected: 모두 성공. Kotlin 테스트 수는 42 + 이번에 추가한 수(Task 3·4·5·7·8·9·13·15·16·17·18·22의 테스트)와 같아야 한다. vitest는 56개 그대로다.

- [ ] **Step 2: release AAB 크기 비교**

```bash
./gradlew :app:bundleRelease -q && ls -l app/build/outputs/bundle/release/app-release.aab | awk '{print $5}'
```
기준선(`2026-10-17-ux-revamp-0-baseline.md`)과의 차이를 계산하고, AAB 증가량을 판정값으로 쓴다(토스페이스는 제외됐다). 판정값이 10MB(10,485,760 bytes)를 넘으면 **멈추고** 사용자와 대안을 정한다(스펙 5절 크기 예산). 넘지 않으면 스펙 5절 구현 기록과 기준선 파일에 "최종 크기, 증가량"을 적는다.

- [ ] **Step 3: 스펙 범위 대조**

스펙 11절 "0번에 포함" 6개 항목을 하나씩 확인하고, 각 항목을 만족하는 커밋을 `git log --oneline main..HEAD`에서 찾아 대응시킨다. 빠진 항목이 있으면 보고한다.

- [ ] **Step 4: Commit**

```bash
git add docs/superpowers/specs/2026-10-17-ux-revamp-0-design-language-design.md docs/superpowers/plans/2026-10-17-ux-revamp-0-baseline.md
git commit -m "docs: record final size and verification for revamp sub-project 0"
```

- [ ] **Step 5: 마무리**

`superpowers:finishing-a-development-branch` 스킬로 통합 방식을 사용자와 정한다. 스펙 13절에 따라 0번만으로는 출시하지 않으며, 1번(결과 화면)도 이 브랜치에서 이어 간다.
