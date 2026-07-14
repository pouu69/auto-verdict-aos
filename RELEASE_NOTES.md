# 출시 노트 (Release Notes)

## 1.0.2 (versionCode 3)

> UI/UX 개선 릴리즈 (내부테스트).

### Play Store — 한국어 (짧은 버전)
```
• 메인 화면과 분석 결과 화면을 더 깔끔하게 다듬었어요
• 분석 점수를 한눈에 보이는 막대 그래프로 표시해요
• 하단 탭 아이콘을 산뜻한 새 아이콘으로 교체했어요
• 결과 화면에서 저장 버튼이 광고에 가려지던 문제를 고쳤어요
```

### Play Store — English (short)
```
• Refreshed the home and analysis result screens for a cleaner look
• The score is now shown as an at-a-glance progress bar
• New, crisper bottom navigation icons
• Fixed the Save button being covered by the ad on the result screen
```

### 상세 변경 내역
**디자인 개선**
- Apple 스타일의 단일 블루 액센트 디자인 언어(DESIGN.md)를 기준으로 전면 정돈
- 메인 화면: 새 Material 아이콘 탭, pill 형태의 주요 버튼, 카드 외곽선·여백·타이포 정리
- 분석 결과: 점수를 미니멀 카드 + 컬러 진행 막대로 재구성, 카테고리 셰브론·배지 다듬기

**버그 수정**
- 분석 결과 화면에서 목록을 끝까지 내렸을 때 오버스크롤 바운스로 '저장하기' 버튼이 하단 광고에 가려지던 문제 수정

### 내부테스트 출시 노트 (Play Console 붙여넣기용)
```
[UI 개선 테스트 빌드]
• 메인 화면 정리 — 새 하단 탭 아이콘, pill 버튼, 카드/여백 다듬기
• 분석 결과 화면 — 점수를 막대 그래프로 표시, 카테고리·배지 정돈
• 저장하기 버튼이 하단 광고에 가려지던 문제 수정

▶ 확인 부탁드려요
- 결과 화면 맨 아래까지 스크롤 → '저장하기'가 광고에 안 가려지는지
- 하단 탭(분석/저장목록/설정) 아이콘이 정상 표시되는지
- 점수 막대 색상·길이가 점수와 맞는지
```
