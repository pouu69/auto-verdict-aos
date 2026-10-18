# Play Store 자산 생성 가이드

모든 스토어 이미지는 저장소 안의 SVG 원본을 `rsvg-convert`로 렌더링해 만든다. 원본은 `docs/assets/source/`, 결과 PNG는 `docs/assets/`에 둔다. 스토어 문구는 `docs/store-listing.md`.

| 자산 | 원본 | 결과 | 규격 |
|---|---|---|---|
| 앱 아이콘 원본 | `docs/assets/source/app-icon.svg` | — (108 viewBox, 확인용) | — |
| Play 아이콘 | `docs/assets/source/play-icon-512.svg` | `docs/assets/play-icon-512.png` | 512×512 PNG |
| 피처 그래픽 | `docs/assets/source/feature-graphic-1024x500.svg` | `docs/assets/feature-graphic-1024x500.png` | 1024×500 PNG |
| 스크린샷 틀 | `docs/assets/source/screenshot-frame.svg` | `docs/assets/screenshots/play/01~08.png` | 1080×1920 PNG |
| 원본 캡처 | 에뮬레이터 `AV_API_36` | `docs/assets/screenshots/raw/01~08.png` | 1080×2400 PNG |

명령은 모두 저장소 루트에서 실행한다.

---

## 0. 준비 — 렌더러와 글꼴

`rsvg-convert`(Homebrew `librsvg`)를 쓴다. 글꼴은 앱에 들어 있는 Pretendard(`app/src/main/res/font`, 굵기 400/600/700)만 쓰도록 임시 fontconfig 설정을 만든다. SVG는 `font-family="Pretendard"`와 굵기 400/600/700만 쓴다.

```bash
mkdir -p /tmp/av-fontcache
cat > /tmp/av-fonts.conf <<EOF
<?xml version="1.0"?><!DOCTYPE fontconfig SYSTEM "fonts.dtd">
<fontconfig><dir>$(pwd)/app/src/main/res/font</dir><cachedir>/tmp/av-fontcache</cachedir></fontconfig>
EOF

# 이후 모든 렌더링 명령 앞에 붙인다
export PANGOCAIRO_BACKEND=fc FONTCONFIG_FILE=/tmp/av-fonts.conf

# 확인: Pretendard 3개(Regular·SemiBold·Bold)만 나와야 한다
fc-list
```

> **`PANGOCAIRO_BACKEND=fc`가 꼭 필요하다.** macOS의 pango는 기본으로 CoreText 글꼴을 쓰므로 `FONTCONFIG_FILE`만 주면 설정이 무시된다. 그러면 시스템에 없는 Pretendard 대신 다른 글꼴로 대체돼, 한글은 가는 대체 글꼴로, 숫자·문장부호만 굵게 섞여 나온다.

---

## 1. 앱 아이콘

- 기준은 `app/src/main/res/drawable/ic_launcher_foreground.xml`(흰 정면 자동차 + 그릴 위치 체크)과 배경색 `@color/av_accent`(`#3182F6`)이다.
- `docs/assets/source/app-icon.svg`는 같은 path를 108 viewBox에 옮긴 확인용 원본이다. 전경 path를 바꾸면 `app-icon.svg`, `play-icon-512.svg`, `feature-graphic-1024x500.svg`의 path를 함께 바꾼다.

```bash
# 작은 크기에서 체크·전조등이 뭉치지 않는지 확인 (결과는 저장소 밖)
rsvg-convert -w 48  docs/assets/source/app-icon.svg -o /tmp/av-icon-48.png
rsvg-convert -w 192 docs/assets/source/app-icon.svg -o /tmp/av-icon-192.png

# Play 아이콘 512 (꽉 찬 #3182F6 정사각형 — 모서리는 Play가 마스크로 둥글린다)
rsvg-convert -w 512 -h 512 docs/assets/source/play-icon-512.svg -o docs/assets/play-icon-512.png
```

---

## 2. 피처 그래픽 (1024×500)

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

- 배경은 토스 파랑 `#3182F6` 단색이다. 그라데이션·장식 원·그림자를 쓰지 않는다.
- 왼쪽: 흰 둥근 사각형(72×72, 반경 18) 안에 파란 아이콘 전경(바탕이 파랑이라 색을 뒤집음), 제목 Pretendard Bold 52px 흰색, 부제 Regular 24px 흰색 85%.
- 오른쪽: 결과 카드 미니어처(348×308, 반경 24). `AvBadge` 모양 "주의" + "72점", 결론 문장 2줄, 구분선, 확인할 것 2줄(주황 점).
- 텍스트는 가장자리에서 40px 이상 안쪽에 둔다. 이미지 안에 '엔카' 글자는 넣지 않는다.

```bash
PANGOCAIRO_BACKEND=fc FONTCONFIG_FILE=/tmp/av-fonts.conf rsvg-convert -w 1024 -h 500 \
  docs/assets/source/feature-graphic-1024x500.svg -o docs/assets/feature-graphic-1024x500.png
```

렌더링한 PNG를 열어 한글이 Pretendard Bold로 고르게 나왔는지(0절 주의 참고), 글자가 카드·가장자리와 겹치지 않는지 확인한다.

---

## 3. 스크린샷 (휴대전화 8장)

### 3.1 틀

`docs/assets/source/screenshot-frame.svg` (1080×1920, 9:16)

- 바탕 `#F2F4F6`(`surfaceMuted`).
- 위 캡션 영역(높이 360): 캡션 2줄, Pretendard Bold 60px `#191F28`, 가운데 정렬(기준선 y 170, 246).
- 아래: 원본 캡처(1080×2400)를 폭 900(높이 2000)으로 줄여 x 90, y 400에 반경 48 모서리로 놓는다. 틀 밖은 잘려서 캡처 상단 약 1520px만 보인다. **보여 주고 싶은 내용이 화면 위쪽에 오게 촬영한다.**
- 바꿀 곳은 토큰 세 개뿐이다: `__CAPTION_1__`, `__CAPTION_2__`, `__SHOT__`(01~08).

### 3.2 촬영할 화면과 캡션

| # | 화면·상태 | 캡션 1줄 | 캡션 2줄 |
|---|---|---|---|
| 01 | 결과 상단(차 이름·판정 배지·점수·결론 문장·개수 칩), '주의' 매물 | 결론부터 | 한 문장으로 알려줘요 |
| 02 | 결과 "확인할 것" 카드 2~3개 | 사기 전에 꼭 | 확인할 것만 골라요 |
| 03 | 상세 시트(보험 공백 또는 소유자 변경 근거) | 판단 근거는 | 원본 그대로 보여줘요 |
| 04 | 이력 타임라인 + 교환·수리 부위 | 사고·소유자 변경·보험 공백을 | 한눈에 |
| 05 | 판매자에게 물어볼 것 | 판매자에게 물어볼 | 질문까지 챙겨요 |
| 06 | 홈(링크 입력 + 최근 본 차) | 링크 하나면 | 바로 분석해요 |
| 07 | 브라우저 공유 시트에서 AutoVerdict 선택(또는 홈의 공유 안내 시트) | 엔카에서 공유하면 | 바로 열려요 |
| 08 | 비교 화면 | 저장하고 두 대를 | 나란히 비교해요 |

- 공개 매물을 찍으므로 차량번호·VIN·딜러 연락처가 보이는 화면(엔카 웹 페이지)은 쓰지 않는다. 결과 화면에는 원래 없다.
- 광고 슬롯은 비어 있는 상태로 찍는다(광고 로드 전에 캡처하거나, 저장한 차 화면은 비행기 모드로).

### 3.3 촬영 (에뮬레이터 `AV_API_36`, 1080×2400)

`AV_API_*` AVD만 쓰고 항상 `-no-snapshot`으로 띄운다. 다른 에뮬레이터는 건드리지 않는다.

```bash
emulator -avd AV_API_36 -no-snapshot &
./gradlew :app:installDebug

# 상태바 데모 모드: 시계 9:00, 배터리 100%, 알림 아이콘 숨김
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0900
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false

# 화면마다 캡처 (01~08)
mkdir -p docs/assets/screenshots/raw
adb exec-out screencap -p > docs/assets/screenshots/raw/01.png

# 끝나면 데모 모드 해제
adb shell am broadcast -a com.android.systemui.demo -e command exit
```

### 3.4 틀에 넣기

librsvg는 SVG 파일이 있는 폴더와 그 아래에 있는 이미지만 읽는다. 그래서 토큰을 채운 SVG를 `docs/assets/screenshots/`에 잠깐 만들어 `raw/NN.png`를 상대 경로로 읽게 한 뒤 지운다.

```bash
mkdir -p docs/assets/screenshots/play
while IFS='|' read -r n c1 c2; do
  f="docs/assets/screenshots/.frame-$n.svg"
  sed -e "s|__SHOT__|$n|g" -e "s|__CAPTION_1__|$c1|" -e "s|__CAPTION_2__|$c2|" \
    docs/assets/source/screenshot-frame.svg > "$f"
  PANGOCAIRO_BACKEND=fc FONTCONFIG_FILE=/tmp/av-fonts.conf \
    rsvg-convert -w 1080 -h 1920 "$f" -o "docs/assets/screenshots/play/$n.png"
  rm "$f"
done <<'EOF'
01|결론부터|한 문장으로 알려줘요
02|사기 전에 꼭|확인할 것만 골라요
03|판단 근거는|원본 그대로 보여줘요
04|사고·소유자 변경·보험 공백을|한눈에
05|판매자에게 물어볼|질문까지 챙겨요
06|링크 하나면|바로 분석해요
07|엔카에서 공유하면|바로 열려요
08|저장하고 두 대를|나란히 비교해요
EOF
```

- 캡션에 `|`, `&`, `<`를 쓰지 않는다(sed 구분자·치환 기호, XML 특수문자).
- 한 줄 캡션은 폭 960px(60px 기준 한글 약 16자) 안에 들어가야 한다. 결과 PNG를 열어 캡션이 잘리지 않았는지 확인한다.
- 옛 디자인 캡처 `docs/assets/screenshots/01~08.png`는 새 `raw/`·`play/`로 바꾼 뒤 삭제한다(git 이력에 남는다).

---

## 4. 최종 점검

| 자산 | 규격 | 필수 | 형식 |
|---|---|---|---|
| 앱 아이콘 | 512×512 PNG, 32-bit | ✅ | PNG |
| 피처 그래픽 | 1024×500 PNG/JPG | ✅ | 텍스트가 잘 읽히는지 확인 |
| 스크린샷 (휴대전화) | 320~3840px, 2~8장 | ✅ (2장 이상) | PNG (`screenshots/play/`) |
| 스크린샷 (7"·10" 태블릿) | 1024~7680px | ⛔ (선택) | — |
| 프로모션 동영상 | YouTube URL | ⛔ (선택) | 30~120초 |

휴대전화 스크린샷만 있어도 출시할 수 있다. 업로드는 Play Console에서 직접 한다(`docs/play-console-walkthrough.md`).
