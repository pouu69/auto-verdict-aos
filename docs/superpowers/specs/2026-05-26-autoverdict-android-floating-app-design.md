# AutoVerdict Android Floating App — Design Spec

## Overview

AutoVerdict Chrome Extension(엔카 중고차 12규칙 자동 평가)을 Android Floating App으로 전환한다. 다른 앱 위에 떠있는 플로팅 버튼을 통해 엔카 매물을 즉시 평가하고, 앱 본체에서 저장/비교/URL 입력 분석이 가능한 하이브리드 앱.

## 핵심 결정 사항

| 항목 | 결정 |
|------|------|
| 플랫폼 | Android (Kotlin) |
| 아키텍처 | Native Shell + WebView 하이브리드 |
| 코드 재사용 | 핵심 로직(parsers, bridge, rules) TS 재사용, UI는 모바일용 신규 작성 |
| 데이터 수집 | 앱에서 직접 (서버 불필요). 숨겨진 WebView로 엔카 페이지 로드 + 페이지 컨텍스트에서 API 호출 |
| AI 평가 | Phase 1 미포함. Phase 2에서 검토 (Chrome Extension의 LLM 평가 기능) |
| 디자인 톤 | 엔카 톤 매칭 — 소프트 뉴트럴(#f7f8fa) + 화이트 카드 + 블루(#0064FF) 포인트 |
| 오버레이 형태 | 전체 화면 오버레이 |
| URL 감지 | Phase 1: 클립보드/공유 기반, Phase 2: Accessibility Service 자동 감지 |

## 3가지 사용 모드

1. **엔카 모바일 앱 위에서 동작** — 플로팅 버튼 탭 → 클립보드 URL 또는 공유 URL로 평가
2. **엔카 모바일 웹 위에서 동작** — 동일 플로팅 버튼 방식
3. **앱 본체에서 URL 직접 입력** — 분석하기 탭에서 URL 붙여넣기 → 분석

## 시스템 아키텍처

### Native Layer (Kotlin)

| 컴포넌트 | 역할 |
|----------|------|
| `FloatingService` | Foreground Service. SYSTEM_ALERT_WINDOW 권한으로 플로팅 버튼 + 오버레이 윈도우 관리 |
| `AccessibilityService` | 엔카 앱/브라우저에서 URL 자동 감지 (Phase 2) |
| `MainActivity` | 앱 본체. 3탭 구조 (분석하기 / 저장목록 / 설정). Share Intent 수신 |
| `EncarCollectorWebView` | 숨겨진(headless) WebView. 엔카 페이지 로드 → JS 실행 후 `__PRELOADED_STATE__` 추출 + 페이지 컨텍스트에서 3개 API 호출 |
| `LocalStorage` | Room DB. MY LIST 저장, 분석 캐시(24h TTL), 설정 값 |

### Web Layer (TypeScript/React → WebView)

| 구분 | 내용 |
|------|------|
| 재사용 | `parsers/encar/*`, `bridge/encar-to-facts`, `rules/*` (12개 규칙), types, constants |
| 신규 작성 | 모바일 최적화 체크리스트 UI, MY LIST 카드 UI, 비교 화면 UI, JS Bridge 어댑터 |

### Native ↔ WebView 통신

`JavaScriptInterface` 브릿지를 통해 통신:
- Native → WebView: `evaluateJavascript()` 로 raw JSON 데이터 전달
- WebView → Native: `@JavascriptInterface` 어노테이션 메서드로 저장/액션 요청

## 데이터 수집 전략

서버 없이 앱에서 직접 데이터를 수집한다. **숨겨진 WebView** 방식을 사용하여 Chrome Extension과 동일한 데이터 접근 환경을 확보한다.

### 왜 OkHttp 직접 호출이 아닌 WebView인가

검증 결과 (2026-05-26):
- `__PRELOADED_STATE__`는 raw HTML에 존재하나 **빈 껍데기**(`cars.base: {}`, `detailFlags: null`)
- 실제 데이터는 클라이언트 JS 실행 후에 채워짐 (Next.js SSR이 아닌 CSR)
- 엔카 API는 same-site 쿠키와 Origin 헤더를 요구하여 외부 HTTP 클라이언트에서 호출 시 제약 있음
- WebView로 페이지를 로드하면 JS 실행 + 쿠키/Origin 자동 포함되어 Chrome Extension과 동일 조건 확보

### 수집 흐름

1. **URL에서 carId 추출**: `/cars/detail/{carId}` 패턴 매칭 (URL의 숫자 ID)
2. **숨겨진 WebView에 엔카 페이지 로드**: `fem.encar.com/cars/detail/{carId}`
   - `WebViewClient.onPageFinished()` 콜백 대기
   - `evaluateJavascript("JSON.stringify(window.__PRELOADED_STATE__.cars)")` 로 데이터 추출
   - 결과: `vehicleId`, `vehicleNo`, `base`(차량 기본정보), `detailFlags`(R01/R02/R03)
3. **같은 WebView 컨텍스트에서 API 3개 병렬 호출** (`evaluateJavascript`로 fetch 실행):
   - `api.encar.com/v1/readside/record/vehicle/{vehicleId}/open?vehicleNo={vehicleNo}`
   - `api.encar.com/v1/readside/diagnosis/vehicle/{vehicleId}`
   - `api.encar.com/v1/readside/inspection/vehicle/{vehicleId}`
   - same-origin 쿠키/헤더가 자동으로 포함됨
4. **수집된 JSON → 평가용 WebView에 전달** → Parser → Bridge → Rules → UI 렌더링

### WebView 수명주기

- **생성**: 첫 분석 요청 시 lazy 생성. `ApplicationContext`에 바인딩하여 Activity/Service 수명주기와 분리.
- **재사용**: 분석 완료 후 WebView를 파괴하지 않고 `loadUrl("about:blank")`로 초기화. 다음 분석 시 재사용하여 생성 비용(~50-100MB) 절감.
- **파괴**: FloatingService 종료 시 또는 메모리 부족 시 `WebView.destroy()` 호출.
- **오버레이 WebView vs 수집 WebView**: 별도 인스턴스. 수집 WebView는 숨겨진(0x0px) 상태로 동작하고, 오버레이 WebView는 평가 UI를 렌더링한다.

### 개인매물(CLIENT) 처리

엔카 매물은 딜러 매물과 개인 매물(CLIENT)로 나뉜다. 개인 매물은 엔카 유료 진단을 받지 않으므로:
- `contact.userType === 'CLIENT'` 또는 `isDealer === false`로 감지
- `diagnosis_api`: 404 반환 → `not_applicable_personal`로 처리 (KILLER가 아님)
- `inspection_api`: 404 반환 → `not_applicable_personal`로 처리
- `record_api`: 정상 작동 (개인 매물도 보험이력 조회 가능)
- R03은 null(규칙 제외), R04는 inspection_api 결과만 사용
- 기존 TS bridge(`encar-to-facts.ts`)의 F3 invariant가 이 로직을 처리하므로 그대로 재사용

### carId vs vehicleId 구분

- `carId`: URL 경로의 숫자 (`/cars/detail/41623743`의 `41623743`)
- `vehicleId`: 엔카 내부 차량 ID (`__PRELOADED_STATE__.cars.base`에서 추출, 대부분 carId와 동일하나 보장되지 않음)
- `vehicleNo`: 차량번호판 (`__PRELOADED_STATE__.cars.base.vehicleNo`, 예: `155우6124`)
- API 호출에는 `vehicleId`와 `vehicleNo`가 필요하므로, 반드시 페이지 로드 후 추출해야 함

### 데이터 소스 → 규칙 매핑

| 규칙 | 데이터 소스 |
|------|-------------|
| R01 보험이력 공개 | `__PRELOADED_STATE__` → `detailFlags.isInsuranceExist` |
| R02 성능점검 공개 | `__PRELOADED_STATE__` → `detailFlags.isHistoryView` |
| R03 엔카 진단 | `__PRELOADED_STATE__` → `detailFlags.isDiagnosisExist` |
| R04 프레임/외판 손상 | diagnosis_api + inspection_api + preloaded_state (3중 레이어) |
| R05 렌탈/택시 이력 | record_api → `loan`(렌트), `business`(영업/택시), `government`(관용) — 필드명과 의미가 역전됨에 주의 |
| R06 전손/침수/도난 | record_api → `totalLossCnt`, `floodTotalLossCnt`, `robberCnt` |
| R07 소유자 변경 횟수 | record_api → `ownerChangeCnt` |
| R08 보험 가입 공백 | record_api → `notJoinDate1..5` |
| R09 미확인 수리비 | record_api (현재 항상 false) |
| R10 보험 청구 금액 | record_api → `myAccidentCost`, `otherAccidentCost` |
| R11 가격 적정성 | `__PRELOADED_STATE__` → `advertisement.price`, `category.newPrice` |
| R12 누유 | inspection_api → `inners[]` 트리 순회, statusType.code 6/7 |

## UI 설계

### 디자인 시스템

| 토큰 | 값 | 용도 |
|------|-----|------|
| Background | `#f7f8fa` | 앱/오버레이 배경 |
| Surface | `#ffffff` | 카드, 입력 필드 |
| Primary | `#0064FF` | CTA 버튼, 점수 배지, 활성 탭, 카테고리 넘버 |
| Text Primary | `#1a1a1a` / `#222222` | 제목, 차량명 |
| Text Secondary | `#888888` / `#999999` | 부가 정보, 설명 |
| Danger | `#E53935` | NEVER 판정, 위험 카운트 |
| Warning | `#F57C00` | CAUTION 판정, 주의 카운트 |
| Success | `#2E7D32` | OK 판정, 통과 카운트 |
| Success Background | `#E8F5E9` | 통과 배지 배경 |
| Warning Background | `#FFF3E0` | 주의 배지 배경 |
| Danger Background | `#FFEBEE` | 위험 배지 배경 |
| Border | `#f0f0f0` / `#e8e8e8` | 카드 테두리, 구분선 |
| Shadow | `0 1px 4px rgba(0,0,0,0.04)` | 카드 그림자 |
| Radius (Card) | `12px` | 카드 라운딩 |
| Radius (Button) | `8px` ~ `12px` | 버튼 라운딩 |
| Font | System (Roboto / Pretendard) | 본문. WebView: Pretendard, Native: 시스템 기본(Roboto) |

### 플로팅 버튼

- 크기: 48x48dp (터치 타겟 충족)
- 모양: 라운드 사각형 (radius 12dp), Primary 블루 배경
- 아이콘: "AV" 텍스트 또는 AutoVerdict 로고
- 드래그 가능, 화면 가장자리에 흡착
- 탭 시: 전체 화면 오버레이 표시

### 플로팅 오버레이 (체크리스트)

**선택된 안: 소프트 뉴트럴 + 블루 포인트 아코디언**

구성 (위→아래):
1. **헤더** — 앱 아이콘 + "점검 리포트" 타이틀 + 닫기 버튼
2. **점수 카드** — 블루 박스에 점수, 차량명, 연식/주행거리/가격, 판정 배지
3. **요약 로우** — 위험/주의/통과/미확인 카운트 4칸 (5단계 severity: killer+fail → 위험, warn → 주의, pass → 통과, unknown → 미확인)
4. **아코디언 그룹** — 카테고리별 접이식 카드 (차량 상태, 이력, 사고, 가격, 투명성 — 기존 codebase `CATEGORY_ORDER` 준수)
   - 접힌 상태: 카테고리명 + 통과 비율 (예: "3/3")
   - 주의/위험 있는 카테고리: 주황/빨간 border-left 강조
   - 펼친 상태: 개별 규칙 라인 (번호 + 이름 + 상태)
5. **하단 액션** — "저장하기" Primary 블루 버튼

### 앱 본체 (3탭)

#### 탭 1: 분석하기
- 상단: 앱 로고 + 타이틀
- URL 입력 카드: 입력 필드 + "분석" 버튼 + "클립보드에서 붙여넣기" 칩
- 최근 분석 이력: 차량명, 스펙, 가격, 점수 배지(색상 코딩), 판정, 시간

#### 탭 2: 저장목록
- 상단: "저장목록" 타이틀 + "비교하기" 버튼
- 필터 칩: 전체 N / 정렬 기준
- 저장 차량 카드: 체크박스(비교 선택) + 차량 정보 + 점수 배지 + 위험/주의/통과 카운트
- 비교 화면: 선택한 2~4대 나란히 비교 (별도 화면)

#### 탭 3: 설정
- 플로팅 버튼: 활성화 토글, 클립보드 자동 감지 토글
- 권한: 다른 앱 위에 표시(허용됨/설정 필요), 접근성 서비스(Phase 2)
- 데이터: 캐시 삭제, 캐시 유효기간
- 정보: 버전, 오픈소스 라이선스

## 구현 단계

### Phase 1 (MVP)
- Android 프로젝트 초기 설정 (Kotlin, Gradle)
- 기존 TS 핵심 로직을 WebView용으로 번들링 (Vite)
- FloatingService + 플로팅 버튼 구현
- 전체 화면 오버레이 (WebView 기반 체크리스트)
- EncarCollectorWebView (숨겨진 WebView 기반 데이터 수집)
- Native ↔ WebView JS Bridge
- MainActivity 3탭 (분석하기, 저장목록, 설정)
- Room DB (MY LIST, 캐시)
- 클립보드 URL 감지
- Share Intent 수신

### Phase 2
- Accessibility Service로 엔카 앱/브라우저 URL 자동 감지
- 비교 화면 구현
- AI 평가 기능 (LLM 기반 — Chrome Extension의 Gemini/OpenAI 평가 포팅 검토)
- 로딩/에러 상태 개선
- 캐시 관리 고도화

## 에러 및 엣지 케이스 처리

### FieldStatus 모델

기존 Chrome Extension의 `FieldStatus<T>` 모델을 그대로 재사용한다:
- `value` — 정상적으로 데이터 수집됨
- `hidden_by_dealer` — 딜러가 비공개 처리한 항목
- `parse_failed` — 파싱 실패 (reason 포함)
- `loading` — 데이터 수집 중
- `timeout` — 수집 시간 초과 (6.5~7초)

### 에러 시나리오

| 시나리오 | 처리 |
|----------|------|
| 유효하지 않은 URL | URL 패턴 매칭 실패 시 "올바른 엔카 매물 URL을 입력하세요" 안내 |
| 매물 삭제/만료 | 페이지 로드 후 `cars.base`가 비어있으면 "매물을 찾을 수 없습니다" 표시 |
| 네트워크 오류 | "네트워크 연결을 확인하세요" + 재시도 버튼 |
| API 타임아웃 | 해당 규칙을 UNKNOWN으로 표시, 나머지 규칙은 정상 평가 |
| 클립보드에 URL 없음 | 플로팅 버튼 탭 시 "엔카 매물 URL을 복사한 후 다시 눌러주세요" 토스트 |
| 동일 매물 재분석 | 캐시 TTL(24h) 내 → 캐시 결과 즉시 표시 + "새로고침" 옵션 제공 |
| 규칙 동적 제외 | R03, R11, R12는 데이터 부재 시 null 반환(규칙 제외) → 점수 분모가 9~12로 변동 |

### FloatingService 안정성

- `startForeground()` 즉시 호출하여 ANR 방지
- `START_STICKY` 반환으로 시스템 kill 시 자동 재시작
- Notification channel 등록 (Android 8.0+)
- `android:foregroundServiceType="specialUse"` 지정 (Android 14+)

## Android 권한

| 권한 | 용도 | 필수 여부 |
|------|------|-----------|
| `SYSTEM_ALERT_WINDOW` | 다른 앱 위에 플로팅 버튼/오버레이 표시 | 필수 |
| `INTERNET` | 엔카 API 호출 | 필수 |
| `FOREGROUND_SERVICE` | FloatingService 실행 | 필수 |
| `POST_NOTIFICATIONS` | Foreground Service 알림 (Android 13+) | 필수 |
| `BIND_ACCESSIBILITY_SERVICE` | URL 자동 감지 (Phase 2) | Phase 2 |

## 기술 스택

| 레이어 | 기술 |
|--------|------|
| Android | Kotlin, Jetpack Compose (앱 본체), View (오버레이) |
| 빌드 | Gradle (Kotlin DSL) |
| 데이터 수집 | 숨겨진 WebView (엔카 페이지 로드 + JS 실행) |
| DB | Room |
| WebView 번들링 | Vite (기존 TS 코드 빌드) |
| 웹 UI | React 18 + TypeScript |
| 테스트 | JUnit 5 (Kotlin), Vitest (TS) |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |
