# 12_sidebar_layout_implementation_plan.md - 사이드바 다중 메뉴 및 페이지 분리 UI 개선 구현 계획서

본 문서는 대시보드 화면에 밀집되어 있던 기능들(단축, 정산, API Key 관리)을 독립적인 관심사별로 분리하고, 전역 사이드바(Sidebar) 내비게이션 및 공통 템플릿 구조를 구축하여 사용성(UX)을 대폭 개선하는 변경 계획서입니다.

---

## 1. User Review Required

> [!IMPORTANT]
> **Mock User 체계 유지를 위한 쿼리 파라미터 보존**
> - 현재 프로젝트는 Spring Security 세션이 구현되기 전의 Mock User 계정 체계(예: `userId=test-user`)로 동작합니다.
> - 사이드바 메뉴 클릭 시 페이지를 이동하더라도 사용자가 선택한 Mock User 정보가 소실되지 않도록, Thymeleaf 링크 처리 시 `userId=${currentUserId}` 파라미터가 자동으로 전파되도록 구현합니다. (예: `/dashboard/developer?userId=test-user`)

> [!TIP]
> **화면 레이아웃 분할 전략**
> - **대시보드 메인**: 링크 생성(좌) + 링크 목록(우)의 2열 구조 유지
> - **수익/정산**: 1열 가로 확장 구조로, 출금 카드 및 정산 내역 테이블을 와이드하고 쾌적하게 렌더링
> - **개발자 센터**: 1열 가로 확장 구조로, 발급받은 API Key와 API 명세를 한눈에 확인하도록 설계

---

## 2. Open Questions

> [!NOTE]
> **모바일 메뉴 UI 토글 방식**
> - 모바일 뷰(1024px 미만)에서는 사이드바가 접히고 상단 햄버거 메뉴를 통해 활성화되도록 간단한 CSS Media Query 및 Vanilla JS를 연동할 예정입니다. 별도의 무거운 UI 라이브러리 추가 없이 경량화된 반응형 레이아웃을 구축합니다.

---

## 3. Proposed Changes

### 3.1. Routing & Controller Layer

#### [MODIFY] [DashboardController.java](file:///h:/lee/pixel-link/src/main/java/com/pixellink/controller/DashboardController.java)
- 기존 `/dashboard` 외에 수익 및 정산용 `/dashboard/monetization`, 개발자 키 관리용 `/dashboard/developer` GET 매핑 메소드 추가.
- 각 핸들러는 Mock User 및 공통 전환기 정보를 뷰에 전달합니다.

---

### 3.2. View & Thymeleaf Fragment Layer

#### [NEW] [fragments.html](file:///h:/lee/pixel-link/src/main/resources/templates/fragments.html)
- 공통 헤더 조각(`header`)과 공통 사이드바 조각(`sidebar`)을 조각화하여 유지관리성을 극대화합니다.
- 사이드바 조각은 파라미터 `activeMenu`에 따라 활성화 메뉴 표시(Active class)를 동적으로 제어합니다.

#### [MODIFY] [dashboard.html](file:///h:/lee/pixel-link/src/main/resources/templates/dashboard.html)
- 기존에 있던 정산금 현황 카드 및 API Key 관리 카드를 제거합니다.
- 공통 `header` 및 `sidebar` 조각을 연동(`th:replace`)하고 본문 레이아웃을 `.dashboard-container` 구조로 재구성합니다.

#### [NEW] [monetization.html](file:///h:/lee/pixel-link/src/main/resources/templates/monetization.html)
- 독립된 정산 및 출금 신청 전용 화면입니다.
- **포함 UI**: 출금 가능 정산액 요약 카드, `[출금 신청하기]` 버튼, 정산 히스토리 내역 테이블.

#### [NEW] [developer.html](file:///h:/lee/pixel-link/src/main/resources/templates/developer.html)
- 독립된 API Key 발급 및 개발자 사양 확인 화면입니다.
- **포함 UI**: API Key 발급 목록 및 무효화 테이블, API Key 생성 프롬프트 모달 트리거, API 호출 방법 가이드(가이드 코드 예제 탑재).

---

### 3.3. CSS 스타일 레이아웃 보강

#### [MODIFY] [global.css](file:///h:/lee/pixel-link/src/main/resources/static/css/global.css)
- 사이드바 글래스모피즘(Glassmorphism) 스타일 및 메뉴 액티브/호버 상태 CSS 규칙 추가.
- 공통 레이아웃 구조인 `.dashboard-container`, `.content-area`, `.content-grid-2` 스타일 규칙 추가.
- 와이드 가로 테이블 렌더링에 적합한 `.table-responsive` 및 쾌적한 폰트/패딩 여백 조정.

---

## 4. Verification Plan

### 4.1. Automated Tests (자동 검증)
- `/dashboard/monetization` 및 `/dashboard/developer` 핸들러가 정상적인 HTTP `200 OK` 응답 및 지정된 Thymeleaf 템플릿을 호출하는지 MockMvc 기반 통합 테스트 추가 및 수행.

### 4.2. Manual Verification (수동 검증 시나리오)
- 로컬 서버 기동 상태에서 브라우저 접속 (`http://localhost:8090/dashboard`)
  - 사이드바 메뉴가 좌측에 정상 고정 노출되는지 확인.
  - 각 메뉴(대시보드, 수익 및 정산, 개발자 센터)를 왕복 클릭하며 파라미터 `userId` 유실 없이 화면이 전환되는지 확인.
  - 개발자 센터에서 API Key 발급 및 복사 기능 작동 확인.
  - 수익 및 정산 페이지에서 정산 히스토리 테이블 확인.
