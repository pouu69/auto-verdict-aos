# AdMob 앱 인증 설정

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
