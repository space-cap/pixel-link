# 14_advanced_create_implementation_plan.md - 메인 단축 폼 경량화 및 '고급 생성 페이지' 분리 구현 계획서

본 문서는 대시보드 진입 시 유저의 인지 부하를 줄이기 위해 메인 단축 폼을 목적지 URL 1개 입력만으로 경량화하고, 스마트 라우팅/페이월/광고 등의 상세 설정을 담은 "고급 생성 페이지(Advanced Link Creator)"를 단독으로 신설하기 위한 구현 계획서입니다.

---

## 1. User Review Required

> [!IMPORTANT]
> **API 호환성 유지**
> - 기존 단축 링크 생성 API(`/api/links`)는 단순 단축과 고급 단축에 상관없이 동일하게 백엔드 데이터 포맷을 처리할 수 있으므로 백엔드 API 컨트롤러(`LinkApiController`) 로직은 수정하지 않고 프론트엔드 HTML/CSS 구조 변경 및 라우팅 추가 위주로 수정 범위를 최적화합니다.

> [!TIP]
> **고급 생성 폼 (Create) UI 개선**
> - 신설되는 고급 단축 페이지(`/dashboard/create`)는 10여 개의 필드가 쏟아져 나오는 부담을 덜기 위해, **접이식 아코디언(Accordion) 방식**을 적용하여 유저가 필요한 옵션(예: 스마트 라우팅)만 펼쳐서 입력할 수 있도록 구성합니다.

---

## 2. Open Questions

> [!NOTE]
> **아코디언 기본 오픈 상태**
> - 고급 단축 폼 진입 시, 모든 아코디언을 접은 상태로 둘 것인지 혹은 가장 기본이 되는 "SEO 및 소셜 공유 설정"을 기본 활성화(Open) 상태로 둘 것인지 조율이 필요합니다. 
> - 본 계획서에서는 첫 진입 시 시각적 편의를 위해 모든 아코디언을 닫아두고 사용자가 직접 탭을 클릭하여 열도록 Vanilla JS 토글 구조를 설계합니다.

---

## 3. Proposed Changes

### 3.1. Controller & Routing Layer

#### [MODIFY] [DashboardController.java](file:///h:/lee/pixel-link/src/main/java/com/pixellink/controller/DashboardController.java)
- `/dashboard/create` GET 핸들러 신설.
- 이 핸들러는 고급 설정 적용 시 테넌트 권한과 테넌트 정보를 전달하기 위해 `currentUser` 및 `mockUsers` 정보를 모델에 얹어 `create` 템플릿을 호출합니다.

---

### 3.2. View & CSS Template Layer

#### [MODIFY] [fragments.html](file:///h:/lee/pixel-link/src/main/resources/templates/fragments.html)
- 사이드바 메뉴 갱신: 대시보드 바로 하단에 `➕ 고급 링크 생성` 메뉴를 행동 유도(CTA) 아이콘과 함께 신설 배치합니다.
- 다국어 키 `#{menu.advanced_create}` 번역 리소스 매핑.

#### [MODIFY] [dashboard.html](file:///h:/lee/pixel-link/src/main/resources/templates/dashboard.html)
- 좌측 단축 생성 폼에서 `Destination URL`을 제외한 모든 입력 필드(SEO, 스마트 라우팅, 페이월, 광고 설정 등) 제거.
- 단순 단축하기 폼 바로 밑에 **"🧭 스마트 라우팅, 페이월 결제 및 마케팅 픽셀 설정이 필요하신가요? [고급 단축 링크 생성하기]"** 텍스트 링크 버튼 추가.
- 자바스크립트 `handleCreateLink` 함수에서 제거된 필드들의 데이터를 `null` 또는 기본값(`false`, `0`)으로 정돈하여 AJAX 전송하도록 단순화.

#### [NEW] [create.html](file:///h:/lee/pixel-link/src/main/resources/templates/create.html)
- 단독 고급 단축 링크 생성 화면.
- **아코디언 그룹 구조 탑재**:
  - 1번 탭: 📁 기본 SNS 공유 (SEO) 설정 (제목, 설명, 이미지)
  - 2번 탭: 🧭 기기별 스마트 라우팅 설정
  - 3번 탭: 📺 중간 대기 광고 노출 설정
  - 4번 탭: 🔒 페이월 콘텐츠 결제 잠금 설정
  - 5번 탭: 🔍 마케팅 트래킹 픽셀 & 스크립트 설정
- 아코디언 헤더 클릭 시 펼쳐지는 Vanilla JS 스크립트 내장.

#### [MODIFY] [global.css](file:///h:/lee/pixel-link/src/main/resources/static/css/global.css)
- 고급 아코디언 스타일 시트 추가:
  ```css
  .accordion-section {
    border: 1px solid var(--card-border);
    border-radius: var(--radius-md);
    margin-bottom: 12px;
    overflow: hidden;
  }
  .accordion-header {
    background: var(--bg-main);
    padding: 16px 20px;
    font-weight: 700;
    cursor: pointer;
    display: flex;
    justify-content: space-between;
    align-items: center;
    user-select: none;
  }
  .accordion-content {
    padding: 24px;
    display: none; /* 기본 접힘 */
    background: #ffffff;
    border-top: 1px solid var(--card-border);
  }
  .accordion-section.active .accordion-content {
    display: block;
  }
  ```

---

## 4. Verification Plan

### 4.1. Automated Tests (자동 검증)
- `DashboardControllerTest.java`에 `/dashboard/create` 경로 GET 호출 테스트 케이스 추가 및 200 OK 렌더링 확인.

### 4.2. Manual Verification (수동 검증 시나리오)
- 로컬 서버 기동 상태에서 브라우저 접속 후 다음 흐름 검증:
  1. 메인 대시보드 홈에서 단순 목적지 URL만 입력하여 1초 만에 링크 단축 발급이 성공하는지 검증.
  2. 메인 하단 고급 링크 텍스트 버튼 또는 사이드바 메뉴 클릭하여 고급 생성 페이지로 이동.
  3. 아코디언 탭을 각각 토글하며 폼 노출 상태 확인.
  4. 고급 옵션(스마트 라우팅 규칙 및 페이월 설정 등)을 꽂아 넣고 단축 성공 후 정상 생성되는지 검증.
