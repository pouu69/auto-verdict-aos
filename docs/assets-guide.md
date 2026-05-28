# Play Store 자산 생성 가이드

## 1. 앱 아이콘 (512×512 PNG)

### 방법 A — Android Studio Image Asset Studio (권장)

1. Android Studio에서 프로젝트 열기
2. `app/src/main/res` 우클릭 → **New → Image Asset**
3. Icon Type: **Launcher Icons (Adaptive and Legacy)**
4. Foreground Layer: **Image** → `app/src/main/res/drawable/ic_launcher_foreground.xml` 선택
5. Background Layer: **Color** → `#3B82F6` (또는 `#0064FF` Primary)
6. Legacy Tab → **Generate** Round/Square 모두 체크
7. **Next → Finish** — 모든 mipmap-* 디렉터리에 PNG 자동 생성됨
8. 추가로 Play Console용 **512×512 PNG**가 필요하면:
   - 상위 메뉴 **Tools → Resource Manager → Drawables → ic_launcher** 우클릭
   - 또는 ADB로 추출: `adb pull /sdcard/Pictures/icon-512.png`

### 방법 B — 명령줄로 변환 (rsvg-convert 또는 ImageMagick)

```bash
# rsvg-convert (Homebrew)
brew install librsvg
rsvg-convert -w 512 -h 512 \
  --background-color="#3B82F6" \
  app/src/main/res/drawable/ic_launcher_foreground.xml \
  -o docs/assets/play-icon-512.png
```

> 단, `ic_launcher_foreground.xml`은 vector drawable 포맷이라 SVG와 다르므로 Android Studio 방식이 더 안전합니다.

### 방법 C — Figma / Sketch에서 새로 디자인
- 캔버스 512×512
- 배경 둥근 사각형(corner radius ~96px) 또는 원형
- Primary 색상 `#0064FF`
- 흰색 차량 아이콘 (현재 foreground와 동일 스타일)
- PNG 익스포트, 알파 채널 포함

**산출물 저장 위치:** `docs/assets/play-icon-512.png`

---

## 2. 피처 그래픽 (1024×500 PNG)

스토어 상단 배너 이미지.

### 구성안

```
┌──────────────────────────────────────────────────────────┐
│ [Logo]  AutoVerdict           [Score Card Mockup 320×400] │
│                                                           │
│         엔카 매물 종합 평가      [85점 ████░ TOP]         │
│         사고이력·진단·점수를                              │
│         한눈에                                            │
│                                                           │
│  Primary #0064FF → Background #F7F8FA 그라데이션 배경      │
└──────────────────────────────────────────────────────────┘
```

### Figma 템플릿 (수동 작성)

1. 캔버스 1024×500
2. 좌측 절반(0~520px): Hero 텍스트 영역
   - 로고 100×100 (Primary 배경 + 흰색 차량 아이콘)
   - "AutoVerdict" 워드마크 48px Bold
   - 부제 "엔카 매물 종합 평가" 24px Medium
   - 보조 카피 "사고이력 · 진단 · 점수를 한눈에" 18px
3. 우측 절반(520~1024px): 앱 스크린샷 합성 또는 점수 카드 미니어처
4. 배경: `linear-gradient(135deg, #E8F1FF 0%, #F7F8FA 100%)`
5. PNG 익스포트

**산출물 저장 위치:** `docs/assets/feature-graphic-1024x500.png`

---

## 3. 스크린샷 (320~3840px, 최소 2장, 최대 8장)

### 권장 캡처 화면

| # | 화면 | 키 캡처 포인트 |
|---|---|---|
| 1 | AnalyzeScreen | URL 입력 + 공유 가이드 펼친 상태 |
| 2 | ResultScreen | 점수 + 판정 결과 (mock 데이터로) |
| 3 | SavedListScreen | 저장된 매물 리스트 (3~5개) |
| 4 | SavedListScreen | 비교 모드 (배너 + 2개 선택 + FAB) |
| 5 | CompareScreen | Hero 카드 + 기본 정보 섹션 |
| 6 | CompareScreen | 진단 결과 + 스택 바 |
| 7 | SettingsScreen | 그룹화된 설정 행 |
| 8 | OnboardingScreen | Hero 페이지 (Auto Awesome) |

### 캡처 방법

#### 에뮬레이터 (Pixel 6 — 1080×2400)

```bash
# Android Studio AVD → Pixel 6 / API 35 부팅 후
emulator -avd Pixel_6_API_35 &

# 앱 실행
$JAVA_HOME/bin/jarsigner -verify ...  # 서명 확인용 (선택)
./gradlew installDebug

# 스크린샷 캡처
adb shell screencap -p /sdcard/screen1.png
adb pull /sdcard/screen1.png docs/assets/screenshots/01-analyze.png
```

#### 실기기

```bash
# 디바이스 연결
adb devices

# 스크린샷 캡처 (Pixel/Galaxy 등)
adb exec-out screencap -p > docs/assets/screenshots/01-analyze.png
```

#### Android Studio 내장 캡처

- Logcat 옆 카메라 아이콘 → 즉시 PNG 저장 (사이즈 자동)

### 보정 (선택)

- 디바이스 프레임 추가: [shotbot.io](https://shotbot.io) 또는 Figma 디바이스 프레임 플러그인
- 캡션 오버레이: 각 스크린샷 상단에 한 줄 설명 (예: "URL 한 번으로 즉시 분석")

**산출물 저장 위치:** `docs/assets/screenshots/01.png ~ 08.png`

---

## 4. 최종 점검

| 자산 | 규격 | 필수 | 권장 형식 |
|---|---|---|---|
| 앱 아이콘 | 512×512 PNG, 32-bit | ✅ | RGBA, 알파 채널 |
| 피처 그래픽 | 1024×500 PNG/JPG | ✅ | 텍스트 가독성 확보 |
| 스크린샷 (휴대전화) | 320~3840px, 2~8장 | ✅ (2장 이상) | PNG/JPG |
| 스크린샷 (7" 태블릿) | 1024~7680px | ⛔ (선택) | — |
| 스크린샷 (10" 태블릿) | 1080~7680px | ⛔ (선택) | — |
| 프로모션 동영상 | YouTube URL | ⛔ (선택) | 30~120초 |

휴대전화 스크린샷만 있어도 출시 가능합니다.

---

## 5. 임시 자산 디렉터리 생성

```bash
mkdir -p docs/assets/screenshots
mkdir -p docs/assets/source
```

생성된 자산은 Play Console 업로드 후에도 백업용으로 유지하세요.
