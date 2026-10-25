# AdMob 광고 및 앱 인증 설정

## 광고 환경과 단위

2026-10-25 Chrome의 AdMob에서 확인·설정한 값이다. 광고 단위 ID는 앱에 배포되는 공개 식별자이며 인증 비밀이 아니다.

| 구분 | 로컬 개발 (`debug`) | 실서비스 (`release`) |
|---|---|---|
| 설치 패키지 | `com.kcurve.autoverdict.debug` | `com.kcurve.autoverdict` |
| 앱 이름 | AutoVerdict Dev | AutoVerdict |
| AdMob 앱 ID | `ca-app-pub-3940256099942544~3347511713` | `ca-app-pub-5353705048338468~6253564104` |
| 홈·저장 목록 배너 | `ca-app-pub-3940256099942544/9214589741` | `ca-app-pub-5353705048338468/1748874472` (`autoverdict_banner`) |
| 결과 네이티브 | `ca-app-pub-3940256099942544/2247696110` | `ca-app-pub-5353705048338468/2138526753` (`autoverdict_result_native`) |
| 공유 진입 앱 오프닝 | `ca-app-pub-3940256099942544/9257395921` | `ca-app-pub-5353705048338468/2486011110` (`autoverdict_share_app_open`) |

- `debug`는 Google 공식 테스트 앱/광고 단위만 사용한다. 앱 데이터도 실서비스 설치와 분리한다.
- `release`는 실제 단위를 사용한다. 잘못되거나 누락된 실서비스 단위는 광고를 생략하고 테스트 광고로 대체하지 않는다.
- AdMob의 **일반** 광고 단위 목록을 확인한다. **파트너 입찰** 목록의 `autoverdict_main_banner` (`4066384469`)는 직접 SDK 요청에 사용하는 단위가 아니다.
- 네이티브는 이미지·동영상 허용, 최소 eCPM은 Google 최적화/모든 가격으로 생성했다. 앱 오프닝도 같은 최소 eCPM 설정이며 서버 게재빈도는 사용자당 24시간에 2회다. 새 단위의 광고 게재에는 최대 1시간이 걸릴 수 있다.

## 화면별 동작

- 홈·저장 목록: 폭에 맞는 적응형 배너, 로딩 전 공간 확보, 탭 버튼과 여백 분리.
- 성공한 분석 결과: 본문 최하단 네이티브 1개. 핵심 판정과 경고를 먼저 보여주며 광고·AdChoices 표시를 유지한다. 결과 고정 배너는 없다.
- 오류·삭제·설정·분석 로딩의 일반 배너/MREC와 비교·온보딩에는 광고를 표시하지 않는다.
- 앱 오프닝: 온보딩을 마치고 분석 성공을 한 번 경험한 사용자가 외부 공유로 앱에 진입했을 때, 저장·캐시 조회 후 **새 수집이 필요한 초기 로딩**에서만 준비된 광고를 한 번 시도한다.
- 앱 내부 붙여넣기, 저장·캐시 결과, 재시도, 일반 복귀, 엔카에서 보기 후 복귀, 화면 재생성은 앱 오프닝 대상이 아니다.
- 클라이언트는 10분 간격·로컬 날짜 기준 하루 최대 2회를 영속 저장한다. AdMob의 24시간 제한도 함께 적용된다. 테스트 광고에도 동일한 제한을 적용한다.
- 광고 유효기간은 4시간이다. 준비되지 않았거나 표시 실패면 분석을 지연하지 않고 생략한다. 광고 로드 콜백에서 뒤늦게 표시하지 않는다.
- 앱 오프닝을 표시한 전경 방문에서는 배너·네이티브 광고를 억제해 다른 광고가 바로 이어지지 않게 한다.

## 개발 및 검증

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug :app:testDebugUnitTest
./run-emulator.sh --skip-build
```

실기기/에뮬레이터 실행은 `AutoVerdict Dev` 디버그 APK로 한다. 실제 광고를 확인하려고 release 광고를 반복 요청하거나 클릭하지 않는다. release APK/AAB는 빌드 및 ID 검사만 수행하며 Play 업로드는 별도 배포 절차다.

회귀 확인: 첫 분석 광고 생략, 저장·캐시 재열람 생략, 외부 공유의 새 수집, 같은 URL 재공유, 광고 실패/미준비, 결과 표시 후 지연 노출 없음, 화면 회전, 엔카 복귀, 10분/일 2회 제한, 앱 오프닝 직후 콘텐츠 광고 억제.

공식 근거: [테스트 광고](https://developers.google.com/admob/android/test-ads), [배너](https://developers.google.com/admob/android/banner), [네이티브](https://developers.google.com/admob/android/native/advanced), [앱 오프닝](https://developers.google.com/admob/android/app-open), [오류 화면 등 광고 제한](https://support.google.com/publisherpolicies/answer/11112688?hl=ko).

### 네이티브 광고 수명과 검증

네이티브 광고는 URL별 결과 화면이 소유하고, 스크롤 목록의 광고 항목은 뷰만 생성·해제한다. 항목이 화면 밖으로 나갔다 돌아와도 같은 광고를 사용하며 새 요청을 보내지 않는다. 결과 화면 이탈·URL 변경·앱 오프닝에 따른 광고 억제 시 광고를 해제한다. 해제 후 도착한 광고 응답도 즉시 폐기한다.

초기 구현은 `LazyColumn` 항목 안에서 요청과 광고를 소유했다. 테스트 광고 로드가 성공했지만 항목 크기가 바뀌어 목록에서 해제되면서, 첫 측정·노출 전에 광고까지 파괴되는 문제를 실제 기기 로그로 재현했다. 화면 범위로 소유권을 옮긴 뒤 Google 공식 테스트 광고의 제목·이미지·설치 버튼 렌더와 SDK impression을 에뮬레이터에서 확인했다. 개발 빌드의 `ResultNative` 로그로 요청·로드·뷰 생성/해제·측정·노출을 구분할 수 있다. 실서비스 ID나 SDK 버전 변경으로 우회한 수정이 아니다.

### 앱 오프닝 공유 진입 검증

2026-10-25 Pixel_8 에뮬레이터의 Chrome에서 실제 엔카 매물을 열고 **Chrome 공유 → AutoVerdict Dev** 경로를 검증했다. fixture 결과를 실제 열어 첫 분석 완료 조건을 충족하고 테스트 광고가 사전 로드된 상태에서, 캐시에 없는 다른 매물을 공유했다. 현재 코드 수정 없이 앱 오프닝 `Test Ad` 표시와 SDK `shown`·`impression`, 노출 횟수 1회 저장을 확인했다.

해당 테스트 광고는 오른쪽 위 **Continue to app >**가 닫기 동작이다. 버튼을 눌러 실제 신규 수집 결과로 복귀했고, 같은 전경 방문의 리포트 최하단에도 네이티브·배너가 나오지 않는 것을 확인했다. 닫기 버튼의 외형·문구는 SDK와 광고 소재가 제공하며 앱에서 별도 버튼을 광고 위에 덧붙이지 않는다. 테스트용 DB·설정은 검증 후 원상복원한다.

## 앱 인증

2026-10-25 확인: Play Console의 `com.kcurve.autoverdict` 스토어 웹사이트는 `https://pouu69.github.io/auto-verdict-aos/app/`로 등록돼 있고, 게시 개요에 미게시 변경사항이 없었다. 앱 소개 페이지와 도메인 루트 `app-ads.txt` 모두 HTTP 200이며 게시자 ID도 일치했다. 그러나 AdMob의 **업데이트 확인** 이후에도 "Google Play의 내 앱 등록정보에서 개발자 웹사이트를 찾지 못했습니다"가 남아 있다. 인증·앱 검토가 완료된 상태로 간주하지 않는다. Google 측 등록정보 감지 후 다시 확인해야 하며, 앱 코드나 광고 단위 생성만으로 이 상태가 해소되지는 않는다.

AdMob은 Google Play의 앱별 웹사이트 주소에서 도메인을 추출하고, 그 도메인의 루트에 있는 `app-ads.txt`로 게시자를 확인한다.

## 게시할 파일과 동작

| 용도 | 공개 URL | 원본 |
|---|---|---|
| 앱 소개·설치·문의 | https://pouu69.github.io/auto-verdict-aos/app/ | 이 저장소의 `docs/legal/app/index.html` |
| 개인정보처리방침 | https://pouu69.github.io/auto-verdict-aos/ | 이 저장소의 `docs/legal/index.html` |
| 광고 게시자 인증 | https://pouu69.github.io/app-ads.txt | `pouu69/pouu69.github.io` 저장소 루트의 `app-ads.txt` |

- 앱 소개는 정적 HTML이다. 설치 링크는 `com.kcurve.autoverdict`의 Google Play 페이지를 열고, 문의 링크는 `pouu69@gmail.com`으로 이메일 작성을 연다.
- 개인정보처리방침 링크는 기존 공개 주소로 연결한다. 페이지는 입력·저장·JavaScript·외부 글꼴·추적 코드를 사용하지 않는다.
- 좁은 화면에서도 내용과 링크가 가려지지 않아야 하며, 키보드로 모든 링크를 사용할 수 있어야 한다.
- 인증 파일은 로그인 없이 HTTP 200과 일반 텍스트로 제공되어야 한다. GitHub Pages의 Jekyll 처리를 위한 front matter나 HTML을 넣지 않는다.
- 이 앱의 게시자 항목은 `google.com, pub-5353705048338468, DIRECT, f08c47fec0942fa0`이다. 다른 앱이 같은 도메인을 사용한다면 기존 항목을 보존한다.

개발자 웹사이트의 `/auto-verdict-aos/app/` 경로는 인증 파일 탐색에 사용되지 않는다. 이 앱 저장소의 `gh-pages`에만 `app-ads.txt`를 넣으면 도메인 루트에 게시되지 않는다. 근거: [Google의 app-ads.txt 설정 안내](https://support.google.com/admob/answer/9363762?hl=ko).

## 배포와 등록

1. 이 저장소의 `gh-pages` 브랜치에 `docs/legal/app/`을 `app/`으로 복사한다. 개인정보처리방침인 기존 `index.html`은 유지한다. `main`의 `docs/legal`을 수정하는 것만으로는 공개 사이트가 바뀌지 않는다.
2. `pouu69/pouu69.github.io` 저장소에서 기존 Pages 배포 브랜치의 루트에 `app-ads.txt`를 추가한다. 기존 사이트와 설정은 유지한다.
3. GitHub에 push하기 전에 변경사항과 검증 결과를 보여 주고 명시적 확인을 받는다.
4. 배포 후 위 세 URL의 HTTP 200 응답, 인증 파일의 게시자 ID, 앱 페이지의 설치·문의·방침 링크를 확인한다.
5. Play Console에서 **AutoVerdict → 스토어 설정 → 스토어 등록정보 연락처 세부정보 → 웹사이트**에 `https://pouu69.github.io/auto-verdict-aos/app/`을 등록하고 변경사항을 게시한다.
6. [공개 앱 페이지](https://play.google.com/store/apps/details?id=com.kcurve.autoverdict&hl=ko)의 **앱 지원 → 웹사이트** 링크가 표시되는지 확인한다.
7. AdMob의 앱 인증 화면에서 **업데이트 확인**을 누른다. Google은 스토어 변경사항 감지와 `app-ads.txt` 확인에 최대 24시간이 걸릴 수 있다고 안내한다.

## 오류 확인

- **개발자 웹사이트를 찾지 못함:** 개인정보처리방침과 별도로 앱별 웹사이트 필드를 등록해야 한다. 저장 후 공개 앱 페이지에 반영됐는지 확인한다.
- **인증 파일이 404:** `pouu69.github.io` 저장소의 배포 소스와 루트 파일 위치를 확인한다.
- **파일을 찾았지만 인증 실패:** 파일의 게시자 ID가 AdMob에서 제공한 값과 일치하는지 확인한다.

## 변경 기록

- 추가: 앱 소개·설치·문의 페이지와 AdMob 인증을 위한 도메인 루트 게시자 파일을 준비했다.
