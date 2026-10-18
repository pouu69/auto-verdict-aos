# 0번 기준선 (2026-10-17)

- 커밋: 888baec
- release AAB 크기: 7071758 bytes
- Kotlin 단위 테스트: 42개 통과
- vitest: 56개 통과

## Task 6A 이후 (2026-10-17)

- 토스페이스 검증 실패(Android 8에서 단색, FE0F 이모지 시스템 대체) → 사용자 결정으로 토스페이스 제외, Pretendard Medium 제외(3종)
- 토스페이스·Pretendard 4종 포함 시 AAB: 13,195,781 bytes (+6,124,023)
- 정리 후 AAB(웨이브 1 병합 포함): 10,301,582 bytes (+3,229,824) — 예산 10MB 이내
- Kotlin 단위 테스트: 104개 통과

## 0번 최종 (2026-10-17, Task 28)

- 커밋: 27083cc
- release AAB: 10,275,681 bytes (+3,203,923, 예산 10MB 이내)
- Kotlin 단위 테스트: 129개 통과 / vitest: 56개 통과

## 1번 결과 화면 (2026-10-17)

- 커밋: 1e64aae (React 의존성 lockfile 정리 반영)
- release AAB: 10,454,463 bytes (0번 최종 10,275,681 대비 +178,782, 예산 10MiB(10,485,760) 이내, 여유 31,297)
- Kotlin 단위 테스트: 226개 통과 / vitest: 83개 통과
- eval-ui 에셋: 이전 React 번들 JS 약 178KB → 이후 `engine.html` 263 bytes + `engine.js` 36,369 bytes (AAB 내 assets는 이 2개뿐)
- `build-all.sh`: 스크립트가 `JAVA_HOME=/opt/homebrew/opt/openjdk@17`(이 머신에 없음)를 강제해 Gradle 단계에서 실패하던 문제 수정됨 — 바깥 JAVA_HOME을 우선(`${JAVA_HOME:-…}`)하도록 바꿨고, Android Studio JBR로 `./build-all.sh` 전체 통과

## 2·3·4번 + 알려진 한계 (2026-10-18, feat/ux-revamp-2)

- 커밋: 9de27c0 (main 미병합, 사용자 최종 확인 대기)
- release AAB: 10,564,318 bytes (1번 최종 10,454,463 대비 +109,855)
  - 추가분 대부분은 오픈소스 고지 JSON(`res/raw/oss_google_notices.json` 374,878 B + `oss_notices.json` 26,520 B, 압축 후 약 78 KB)과 새 화면(홈·온보딩·저장·비교·설정·라이선스) 코드
  - 한국어 리소스만 포함(`localeFilters`)으로 debug APK는 약 674 KiB 줄었지만, AAB에는 매핑·디버그 심볼 등 BUNDLE-METADATA가 함께 들어 있어 AAB 파일 크기로는 차이가 작게 보인다
  - 0번에서 정한 "10 MiB(10,485,760) 이내" 예산을 약 78 KB 넘음 → 사용자 판단 필요(고지 JSON을 줄이거나 예산 기준을 실제 다운로드 크기로 바꾸는 방안)
- Kotlin 단위 테스트 384개 / vitest 164개 통과

## AAB 크기 최적화 (2026-10-18)

측정: `./gradlew :app:bundleRelease`의 AAB 파일 크기, 다운로드 추정은 Gradle 캐시의 bundletool 1.18.3 `get-size total`(MIN–MAX, 기기별 분할 APK 기준).

| 단계 | 커밋 | AAB (bytes) | 직전 대비 | 다운로드 추정 (bytes) |
|---|---|---|---|---|
| 기준선 (359e2c6) | — | 10,567,006 | — | 6,283,354–6,309,702 |
| 네이티브 디버그 심볼 FULL → SYMBOL_TABLE | 295790c | 10,567,006 | 0 | 동일 |
| Compose runtime 일괄 keep 제거 | 0e40695 | 10,388,567 | −178,439 | 6,095,009–6,119,918 |
| Lifecycle·SplashScreen keep 제거 | 0013efd | 10,357,713 | −30,854 | 6,077,693–6,103,044 |
| 불필요한 Java 리소스 제외 | cbc99ad | 10,326,790 | −30,923 | 6,060,242–6,083,540 |

- 최종 10,326,790 bytes → 예산 10 MiB(10,485,760) 이내, 여유 158,970 bytes. 기준선 대비 −240,216 bytes, 다운로드 약 −223 KB.
- AAB 구성(최종, 압축 후): `proguard.map` 4,190,733(사용자 다운로드에 포함되지 않음, Play 난독화 해제용) / `classes.dex` 2,751,387 / 폰트 3종 3,183,799 / `oss_google_notices.json` 70,720 / `engine.js` 20,010.
- 디버그 심볼: 유일한 네이티브 라이브러리(androidx.graphics.path)가 이미 strip된 상태라 FULL에서도 심볼이 0 bytes였다. 크기 변화 없음.
- 오픈소스 고지 JSON: 이미 텍스트 id로 중복 제거돼 있다. 공백 제거(minify)는 deflate 후 약 50 bytes, Apache 2.0 본문을 세그먼트로 분리하는 방식은 오히려 +1.8 KB라 적용하지 않았다.
- 폰트 선택지(측정만, 미적용): KS X 1001 한글 2,350자 + KS X 1001 비한자 기호 전부 + ASCII/Latin-1/일반 문장부호 + 앱 소스에 쓰인 문자(3,601자, 글리프 14,716 → 3,374)로 3종을 서브셋하면 AAB 7,965,185(−2,361,605), 다운로드 3,702,638–3,725,900. SemiBold를 빼고 2종으로 줄이면 AAB 9,258,813(−1,067,977), 다운로드 4,994,460–5,018,004.
- Kotlin 단위 테스트 385개 통과, `:app:assembleDebug`·`:app:bundleRelease` 성공.
