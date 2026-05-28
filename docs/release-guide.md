# AutoVerdict 릴리즈 가이드

## 1. Keystore 생성 (최초 1회만)

업로드 키스토어를 생성합니다. **반드시 안전한 곳에 별도 백업** — 분실하면 동일 앱 업데이트가 불가능합니다.

```bash
cd /Users/kwanung/development/experiments/daksin-car-aos

# 1. keystore 디렉터리 생성 (git에 커밋하지 말 것)
mkdir -p secrets

# 2. keystore 생성 (대화형, 비밀번호와 정보 입력)
keytool -genkey -v \
  -keystore secrets/autoverdict-upload.jks \
  -alias autoverdict-upload \
  -keyalg RSA \
  -keysize 2048 \
  -validity 9125

# 입력 예:
#   이름: AutoVerdict
#   조직단위: Personal
#   조직명: Daksin
#   도시: Seoul
#   주: Seoul
#   국가코드: KR
```

생성된 `secrets/autoverdict-upload.jks`를 **별도 외부 저장소(예: iCloud, 1Password)에 백업**해두세요.

## 2. keystore.properties 작성

프로젝트 루트에 `keystore.properties` 파일을 만듭니다 (이미 `.gitignore`에 포함됨).

```properties
# /Users/kwanung/development/experiments/daksin-car-aos/keystore.properties
storeFile=secrets/autoverdict-upload.jks
storePassword=<your_store_password>
keyAlias=autoverdict-upload
keyPassword=<your_key_password>
```

## 3. .gitignore 확인

`keystore.properties`와 `secrets/`이 .gitignore에 포함되어 있는지 확인:

```bash
grep -E "keystore.properties|secrets/" .gitignore || echo "추가 필요!"
```

만약 누락됐다면:

```gitignore
# Signing
keystore.properties
secrets/
*.jks
*.keystore
```

## 4. Release AAB 빌드

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

# 클린 빌드
./gradlew clean

# Release AAB 생성 (Google Play 업로드 형식)
./gradlew :app:bundleRelease

# 결과물 위치:
# app/build/outputs/bundle/release/app-release.aab
```

**선택사항**: APK도 함께 빌드 (사이드로드 테스트용):

```bash
./gradlew :app:assembleRelease
# app/build/outputs/apk/release/app-release.apk
```

## 5. 서명 검증

```bash
# AAB 내부 서명 확인
$JAVA_HOME/bin/jarsigner -verify -verbose -certs \
  app/build/outputs/bundle/release/app-release.aab | head -20

# 또는 bundletool 사용 (권장)
# brew install bundletool
bundletool dump manifest --bundle=app/build/outputs/bundle/release/app-release.aab
```

## 6. 실기기 설치 테스트 (APK)

```bash
# 디바이스 연결 후
adb install -r app/build/outputs/apk/release/app-release.apk

# 또는 AAB를 APK로 변환하여 설치
bundletool build-apks --bundle=app/build/outputs/bundle/release/app-release.aab \
  --output=app/build/outputs/bundle/release/app-release.apks \
  --mode=universal
bundletool install-apks --apks=app/build/outputs/bundle/release/app-release.apks
```

## 7. Google Play Console 업로드 순서

1. **Play Console** → 앱 만들기 → AutoVerdict
2. **앱 무결성 → Play 앱 서명** → "Play 앱 서명 사용" 활성화
3. **테스트 → 내부 테스트** 트랙에서 첫 AAB 업로드
   - 테스터 이메일 등록 (본인 + 지인)
   - 며칠간 안정성 확인
4. **테스트 → 비공개 테스트(Closed Testing)** 트랙으로 승격
   - **활성 테스터 12명 이상 14일** (신규 개인 개발자 의무)
5. **프로덕션** 출시 요청 → 심사 대기 (보통 1~7일)

## 8. 자주 발생하는 거절 사유

| 사유 | 예방책 |
|---|---|
| 개인정보처리방침 URL 누락 또는 접근 불가 | GitHub Pages 등 정적 호스팅으로 공개 |
| 광고 ID 권한 누락 (AdMob 사용시) | `AD_ID` 권한 명시 ✅ |
| target SDK 미달 | targetSdk=35 ✅ |
| 데이터 안전성 양식 미작성 | Play Console에서 필수 작성 |
| 콘텐츠 등급 누락 | IARC 설문 완료 |
| 광고 노출 미선언 | 스토어 등록정보에서 "광고 포함" 체크 |
| 권한 사용 사유 불명확 | INTERNET, AD_ID만 사용 (자명) ✅ |

## 9. 출시 후 모니터링

- **Play Console → 통계** — 설치/제거/평점
- **Play Console → 크래시 및 ANR** — Crashlytics 미사용시 여기서 확인
- **AdMob 대시보드** — 광고 노출/수익 확인
