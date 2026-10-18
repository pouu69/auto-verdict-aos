# Google Play 출시 체크리스트

> AutoVerdict v1.0.2를 Play Store에 출시하기 위한 사용자 액션 가이드 (첫 Play Store 출시).
> 모든 자동화 가능한 작업은 이미 완료됨 — 아래 단계만 진행하면 출시 가능.

---

## 📌 출시 준비 상태

| 항목 | 상태 |
|---|---|
| Release AAB 빌드 | ✅ 완료 — `app/build/outputs/bundle/release/app-release.aab` (약 7.1MB, v1.0.2 / versionCode 3) |
| Upload Keystore 생성 | ✅ 완료 — `secrets/autoverdict-upload.jks` (2026-10-17 재발급, 기존 키는 분실) |
| 서명 설정 | ✅ 완료 — `keystore.properties` |
| AD_ID 권한 명시 | ✅ 완료 |
| 앱 아이콘 (512×512) | ✅ 완료 — `docs/assets/play-icon-512.png` |
| 피처 그래픽 (1024×500) | ✅ 완료 — `docs/assets/feature-graphic-1024x500.png` |
| 개인정보처리방침 작성 | ✅ 완료 — `docs/privacy-policy.md` |
| 스토어 카피 작성 | ✅ 완료 — `docs/store-listing.md` |
| 개인정보처리방침 호스팅 | ✅ 완료 — https://pouu69.github.io/auto-verdict-aos/ |
| 스크린샷 캡처 | ✅ 완료 — 새 디자인 8장, 스토어용 `docs/assets/screenshots/play/01~08.png` (원본 `raw/`, 목록·캡션은 `docs/assets-guide.md`) |
| 오픈소스 고지 | 의존성을 바꿨으면 `scripts/oss-notices/README.md` 절차로 다시 생성 |
| EEA·영국 광고 동의(UMP) | ⛔ 미구현 — 해당 지역 배포 전 동의 흐름 필요 |
| targetSdk 36 대응 | ✅ 완료 — compileSdk/targetSdk 36, Robolectric 4.16 |
| **Keystore 외부 백업** | ⛔ **사용자 진행 필요** |
| **Play Console 등록·업로드** | ⛔ **사용자 진행 필요** |

---

## 📋 앱 기술 정보 (Play Console 입력용)

> `app/build.gradle.kts` · `AndroidManifest.xml`에서 확인한 실제 값. Play Console 양식에 그대로 입력하면 됩니다.

| 항목 | 값 | 비고 |
|---|---|---|
| **패키지 이름 (applicationId)** | `com.car.autoverdict` | ⚠️ 한번 등록하면 변경 불가 — 모든 향후 업데이트의 영구 식별자 |
| namespace | `com.car.autoverdict` | applicationId와 동일 |
| **버전 코드 (versionCode)** | `3` | 업로드마다 +1 필수 |
| **버전 이름 (versionName)** | `1.0.2` | 사용자에게 표시되는 버전 |
| 최소 SDK (minSdk) | API 26 (Android 8.0 Oreo) | 지원 최저 기기 |
| 타겟 SDK (targetSdk) | API 36 (Android 16) | Play 정책 충족 (2026-08-31부터 신규 앱·업데이트는 API 36 필수) |
| 컴파일 SDK (compileSdk) | API 36 | — |
| 빌드 산출물 | AAB (Android App Bundle) | `app/build/outputs/bundle/release/app-release.aab` |
| 코드 난독화 | R8 활성화 (`isMinifyEnabled = true`, `isShrinkResources = true`) | ProGuard 규칙: `app/proguard-rules.pro` |
| 백업 허용 | `allowBackup="false"` | — |

### 권한 (Permissions)

| 권한 | 용도 |
|---|---|
| `android.permission.INTERNET` | 엔카 페이지 로드 및 API 호출 |
| `com.google.android.gms.permission.AD_ID` | AdMob 광고 식별자 (AAID) |

> 위치·카메라·연락처·저장소 등 민감 권한은 **요청하지 않음**.

### 인텐트 필터 (앱 진입점)

- `MAIN` / `LAUNCHER` — 일반 앱 실행
- `SEND` (`text/plain`) — 엔카 앱·브라우저의 **공유 시트에서 AutoVerdict로 URL 공유** 가능

### AdMob

| 항목 | 값 |
|---|---|
| AdMob 앱 ID | `ca-app-pub-5353705048338468~6253564104` |
| 광고 형식 | 배너만 (`play-services-ads:23.6.0`, 전면 광고 없음) |

> AdMob 앱 ID는 `AndroidManifest.xml`의 `<meta-data>`에 명시되어 있으며, Play Console **데이터 안전성** 및 **광고 선언** 항목과 일치시켜야 합니다.

### 개발자 정보

| 항목 | 값                    |
|---|----------------------|
| 개발자명 | �AutoVerdict                  |
| 연락처 이메일 | pouu69@gmail.com     |
| 서명 키 별칭 | `autoverdict-upload` |

---

## 🚨 STEP 0 — 즉시 처리 (분실 위험)

### Keystore 백업

⚠️ **이 파일과 비밀번호를 잃으면 동일 앱 업데이트가 영원히 불가능합니다.**

백업할 항목:

```
파일:       secrets/autoverdict-upload.jks  (저장소 루트 기준)
별칭:       autoverdict-upload
비밀번호:    <비공개 — keystore.properties 파일 참조 또는 별도 보관>
유효기간:   2026-10-17 ~ 약 25년
```

> 실제 비밀번호와 SHA-256 지문은 git에 절대 커밋하지 마세요.
> 비밀번호는 로컬 `keystore.properties` (이미 `.gitignore`에 포함)에서 확인할 수 있고,
> SHA-256 지문은 다음 명령으로 다시 조회 가능합니다:
>
> ```bash
> "$JAVA_HOME/bin/keytool" -list -keystore secrets/autoverdict-upload.jks
> ```

추천 백업 위치 (**최소 2곳**):
- ✅ 1Password / Bitwarden / Keychain (비밀번호 매니저)
- ✅ iCloud Drive / Dropbox 개인 폴더
- ✅ 외장 SSD 또는 USB
- ❌ Git 저장소 (gitignore되어 있지만 절대 push 금지)
- ❌ 평문 이메일

---

## STEP 1 — 개인정보처리방침 공개 호스팅 ✅ 완료

> ✅ 호스팅 완료: **https://pouu69.github.io/auto-verdict-aos/** — 아래 옵션은 참고용.

Play Console은 **공개 URL**을 요구합니다. 앱 내 정책 화면만으로는 부족합니다.

### 옵션 A — GitHub Pages (무료, 추천)

```bash
# 새 저장소 또는 기존 저장소의 docs/ 폴더 사용
# 1) 새 public 저장소 생성: e.g., autoverdict-legal
# 2) docs/privacy-policy.md를 README.md 또는 index.md로 복사
# 3) Settings → Pages → Source: main branch / root
# 4) URL 형식: https://<username>.github.io/autoverdict-legal/
```

### 옵션 B — Notion 공개 페이지

1. Notion에 새 페이지 생성
2. `docs/privacy-policy.md` 내용 붙여넣기
3. Share → Publish to web → URL 복사

### 옵션 C — Vercel / Netlify

`docs/privacy-policy.md`만 들어있는 단일 정적 사이트 배포.

### 발급된 URL을 다음에 입력
- Play Console → 앱 콘텐츠 → 개인정보처리방침
- 앱 내 `PrivacyPolicyScreen`에 외부 링크 버튼 추가 (선택)

---

## STEP 2 — 스크린샷 캡처 (최소 2장, 권장 8장) ✅ 완료

> ✅ 캡처 완료: 원본 `docs/assets/screenshots/raw/01~08.png`, 틀에 넣은 스토어용 `docs/assets/screenshots/play/01~08.png` — 촬영·합성 절차는 `docs/assets-guide.md` 3절, 아래는 참고용.

### 디바이스 준비

```bash
# 에뮬레이터 (Pixel 6, API 36) 또는 실기기 연결
adb devices

# Debug 빌드 설치
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew installDebug
```

### 캡처 절차 (테스트 데이터로 화면을 실제로 띄워야 함)

| # | 화면 | 준비 |
|---|---|---|
| 1 | Onboarding 1페이지 | 첫 실행 시 자동 표시 |
| 2 | AnalyzeScreen | URL 입력 + 공유 가이드 펼친 상태 |
| 3 | ResultScreen | 실제 엔카 URL 분석 후 결과 화면 |
| 4 | SavedListScreen | 저장된 매물 3~5개 (직접 저장 진행) |
| 5 | SavedListScreen | 비교 모드 + 2개 선택 + FAB 표시 |
| 6 | CompareScreen | 기본 정보 섹션 |
| 7 | CompareScreen | 진단 결과 + 스택 바 |
| 8 | SettingsScreen | 그룹화된 설정 행 |

### 캡처 명령

```bash
# Android Studio: Logcat 옆 카메라 버튼 (가장 편함)
# 또는 adb로:
adb exec-out screencap -p > docs/assets/screenshots/raw/01.png
adb exec-out screencap -p > docs/assets/screenshots/raw/02.png
# ... 8장 반복
```

해상도 320~3840px, PNG/JPG.

---

## STEP 3 — Google Play Console 신규 앱 등록

### 3-1. 개발자 계정

- https://play.google.com/console
- 1회 25 USD 등록 + 신원 확인 (여권 또는 신분증)
- 개인 정보 검증에 1~3일 소요

### 3-2. 새 앱 만들기

| 입력 항목 | 값 |
|---|---|
| 앱 이름 | AutoVerdict — 중고차 매물 평가 (`store-listing.md`와 동일, 상표 '엔카' 제외) |
| 기본 언어 | 한국어 (대한민국) |
| 앱 또는 게임 | 앱 |
| 무료 또는 유료 | 무료 |
| 선언 | 모두 체크 |

### 3-3. 앱 무결성 → Play 앱 서명

- "Play 앱 서명 사용" **활성화** (필수)
- 첫 AAB 업로드 시 Google이 자동으로 앱 서명 키를 관리
- `secrets/autoverdict-upload.jks`는 **업로드 키**로만 사용됨

### 3-4. 스토어 등록정보 (Main Store Listing)

`docs/store-listing.md`의 내용을 그대로 복사:

- **짧은 설명** (80자) — 짧은 설명 그대로
- **자세한 설명** (4000자) — 긴 설명 그대로
- **앱 아이콘** — `docs/assets/play-icon-512.png`
- **피처 그래픽** — `docs/assets/feature-graphic-1024x500.png`
- **휴대전화 스크린샷** — `docs/assets/screenshots/play/01~08.png` (Step 2에서 생성)
- **앱 카테고리** — 자동차

### 3-5. 앱 콘텐츠 (정책 페이지)

| 양식 | 응답 |
|---|---|
| 개인정보처리방침 | https://pouu69.github.io/auto-verdict-aos/ |
| 광고 | **예** — AdMob 배너 광고 |
| 인앱 결제 | 아니오 |
| 대상 연령 | 만 13세 이상 |
| 콘텐츠 등급 | IARC 설문 진행 (모두 "아니오" 응답 → 전체이용가) |
| 정부 앱 | 아니오 |
| 뉴스 앱 | 아니오 |
| 데이터 안전성 | `docs/store-listing.md`의 "데이터 안전성 (Data Safety) 응답 가이드" 절 참조 (기준: `docs/privacy-policy.md`) |

#### 데이터 안전성 핵심 응답

- **데이터 수집·공유 (Google AdMob SDK → Google):** 광고 ID(AAID)·앱 세트 ID, IP 주소, 기기·앱 정보(모델·OS 버전·언어 등), 광고 노출·클릭
- **IP 주소 → 위치 > 대략적인 위치:** 수집 예 / 공유 예(Google, AdMob SDK) / 일시적 처리 아니오 / 필수 / 용도: 광고 또는 마케팅, 분석, 사기 예방·보안·규정 준수 (근거: Google Mobile Ads SDK Play 데이터 공개 안내)
  - Play Console 경로: **앱 콘텐츠 → 데이터 보안 → 데이터 유형 → 위치 → "대략적인 위치" 체크 → 수집·공유 모두 체크 → 일시적 처리 "아니요" → "필수" → 용도 체크**
- **엔카로 직접 전달:** 매물 ID와 차량번호(이력 조회용), IP 주소 등 일반 접속 정보 — 사용자가 요청한 매물 조회용, 앱 운영자 서버를 거치지 않음
- **앱이 만드는 데이터:** 매물 URL·분석 결과·저장한 차는 기기에만 저장 (앱 운영자 서버 없음, 기기 백업 제외)
- **암호화:** ✅ 전송 시 HTTPS 전용
- **삭제:** ✅ 설정 → "최근 본 차 지우기" / "저장한 차 모두 지우기", 저장한 차 개별 삭제, 앱 삭제 시 전부 제거

---

## STEP 4 — 테스트 트랙 진행

### 4-1. Internal Testing (먼저)

```
테스트 → 내부 테스트 → 새 버전 만들기
```

- AAB 업로드: `app/build/outputs/bundle/release/app-release.aab`
- 출시 노트: `docs/store-listing.md`의 "무엇이 새로운가요?" 내용
- 테스터 이메일 등록 (본인 + 신뢰할 수 있는 1~2명)
- 검토 후 출시 (수 시간~수일)

### 4-2. Closed Testing (필수)

신규 개인 개발자는 **활성 테스터 12명 이상 14일 이상** 의무.

```
테스트 → 비공개 테스트 → 새 트랙 생성 (예: "베타 12명")
```

- Internal에서 사용한 AAB를 비공개 트랙으로 승격 또는 새로 업로드
- Google 그룹 또는 이메일 리스트로 12명 이상 초대
- 14일 동안 실제 사용 활동이 기록되어야 함
- 테스터가 앱을 설치만 하고 사용하지 않으면 카운트 안 됨

#### 12명 모집 팁
- 가족·지인·동료 우선
- Reddit `/r/AndroidApps/`, Discord 개발자 커뮤니티
- "AutoVerdict 베타 테스터 모집" 트윗/X 게시

### 4-3. Production (프로덕션)

Closed Testing 14일 + 12명 충족 후:

```
프로덕션 → 새 버전 만들기
```

- Closed 트랙의 AAB를 프로덕션으로 승격
- "출시 국가" — 대한민국 선택
- 검토 제출 → 보통 1~7일 심사 → 통과 시 자동 게시

---

## STEP 5 — 출시 후 모니터링

### 첫 7일
- Play Console → 통계 → 설치/제거/평점
- Play Console → 품질 → 크래시 및 ANR
- AdMob → 노출/수익

### 1.0.3 패치를 위한 준비
- 버그 제보·평점·리뷰 수집
- 다음 버전 작업 시:
  ```kotlin
  // app/build.gradle.kts
  versionCode = 4          // 매번 +1
  versionName = "1.0.3"    // semver
  ```

---

## ⏱ 예상 타임라인

| 작업 | 소요 시간 |
|---|---|
| Keystore 백업 | 5분 |
| 정책 호스팅 (GitHub Pages) | 30분 |
| 스크린샷 8장 캡처 | 1~2시간 |
| Play Console 신규 등록 + 신원 확인 | 1~3일 (Google 대기) |
| 스토어 등록정보 입력 | 1시간 |
| Internal Testing 출시 | 즉시 ~ 수 시간 |
| Closed Testing 12명 14일 | **14일 의무 대기** |
| Production 심사 | 1~7일 |

**최단 코스: 약 16~20일** (14일 의무 대기 포함)

---

## 📞 막힐 때

- Play Console 도움말: https://support.google.com/googleplay/android-developer
- 거절 사유는 Play Console 알림에 명시됨 — `docs/release-guide.md` 8절 참고
- 본 프로젝트 문서:
  - `docs/privacy-policy.md`
  - `docs/store-listing.md`
  - `docs/release-guide.md`
  - `docs/assets-guide.md`
  - `docs/play-store-checklist.md` (이 파일)
