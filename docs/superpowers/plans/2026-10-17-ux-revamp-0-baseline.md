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
