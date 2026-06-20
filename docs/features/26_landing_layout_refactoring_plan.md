# 랜딩 헤더 및 푸터 공용화(Refactoring) 구현 계획서

본 문서는 홈페이지 메인 랜딩 페이지와 FAQ 등 비로그인 정보성 화면 간의 헤더/푸터 코드 중복을 제거하고, 디자인 일관성 및 향후 유지보수성을 극대화하기 위해 공용 레이아웃 조각(Fragment)으로 리팩토링하는 계획을 정리한 문서입니다.

---

## 1. 개요 및 리팩토링 목적

- **현 상태 및 문제점**:
  - 현재 대시보드 내부(회원 전용) 영역은 공용화가 잘 되어 있으나, 외부 노출용 랜딩 헤더와 푸터 코드는 `landing.html`과 `faq.html`에 각각 수백 줄씩 물리적으로 중복 작성되어 있습니다.
  - 이로 인해 로고 문구나 메뉴 링크 하나를 수정하더라도 모든 파일을 찾아가 수동으로 동기화해야 하므로 실수의 여지가 크고 파편화가 발생합니다.
- **개선 목표**:
  - Thymeleaf의 `th:fragment` 속성을 활용해 비회원 공용 헤더(`landingHeader`)와 공용 푸터(`landingFooter`)를 정의합니다.
  - 개별 화면 파일에서는 공용 조각을 호출하는 한 줄의 코드만 남겨 가독성을 높이고 **DRY(Don't Repeat Yourself) 원칙**을 엄격히 수립합니다.

---

## 2. 세부 리팩토링 방안

### 2.1. 공용 템플릿 정의

#### [fragments.html](file:///c:/workdir/space-cap/pixel-link/src/main/resources/templates/fragments.html)
- 비회원 전용으로 렌더링될 상단 헤더 조각(`landingHeader`)과 하단 푸터 조각(`landingFooter`)을 추가 정의합니다.
- 언어 선택(switchLanguage) 및 로그인/로그아웃 조건부 렌더링 로직을 조각 내부로 안전하게 흡수합니다.

```html
    <!-- 3. 비회원/랜딩용 공용 헤더 조각 -->
    <header th:fragment="landingHeader">
        <div class="logo-container">
            <a th:href="@{/}" style="text-decoration: none; display: flex; align-items: center; gap: 10px;">
                <span class="logo">Pixel-Link</span>
                <span class="logo-badge" th:text="#{dashboard.logo_badge}">Premium Linker</span>
            </a>
        </div>
        
        <div class="header-action">
            <span th:text="#{dashboard.lang_select}">Language</span>
            <select class="select-user" onchange="switchLanguage(this.value)" style="margin-right: 15px;">
                <option value="en" th:selected="${#locale.language == 'en'}">English</option>
                <option value="ko" th:selected="${#locale.language == 'ko'}">한국어</option>
                <option value="ja" th:selected="${#locale.language == 'ja'}">日本語</option>
            </select>

            <th:block th:if="${session.user == null}">
                <a th:href="@{/app/login}" class="btn-login-header" th:text="#{landing.btn_login}">로그인</a>
                <a th:href="@{/app/signup}" class="btn-signup-header" th:text="#{landing.btn_signup}">회원가입</a>
            </th:block>
            <th:block th:if="${session.user != null}">
                <a th:href="@{/app/dashboard}" class="btn-signup-header" th:text="#{dashboard.title}">대시보드 보기</a>
                <a th:href="@{/logout}" class="btn-login-header" th:text="#{landing.btn_logout}">로그아웃</a>
            </th:block>
        </div>
    </header>

    <!-- 4. 비회원/랜딩용 공용 푸터 조각 -->
    <footer th:fragment="landingFooter" style="background: #0f172a; color: #475569; padding: 40px 20px; text-align: center; border-top: 1px solid #1e293b; font-size: 0.9rem;">
        <div style="margin-bottom: 15px; display: flex; justify-content: center; gap: 20px;">
            <a th:href="@{/info/faq}" style="color: #64748b; text-decoration: none; font-weight: 600; transition: color 0.2s;" onmouseover="this.style.color='#94a3b8'" onmouseout="this.style.color='#64748b'" th:text="#{menu.faq}">FAQ / 고객지원</a>
        </div>
        <p style="margin: 0; font-size: 0.85rem;">&copy; 2026 Pixel-Link. All rights reserved.</p>
    </footer>
```

### 2.2. 개별 웹 페이지 적용 및 단순화

#### [landing.html](file:///c:/workdir/space-cap/pixel-link/src/main/resources/templates/landing.html)
- 하드코딩된 `<header>` 및 `<footer>` 마크업 코드를 걷어내고 프래그먼트 호출 코드로 대체합니다.
  - `<header th:replace="~{fragments :: landingHeader}"></header>`
  - `<footer th:replace="~{fragments :: landingFooter}"></footer>`

#### [faq.html](file:///c:/workdir/space-cap/pixel-link/src/main/resources/templates/faq.html)
- 비로그인 영역의 헤더와 푸터 코드를 동일하게 호출 코드로 교체합니다.
  - `<header th:replace="~{fragments :: landingHeader}"></header>`
  - `<footer th:replace="~{fragments :: landingFooter}"></footer>`

---

## 3. 검증 계획

### 3.1. 화면 구조 및 스타일 깨짐 여부 체크
- 리팩토링 배포 후, 메인 랜딩 페이지(`http://localhost:8090/`)와 FAQ 페이지(`http://localhost:8090/info/faq`)에 각각 접속합니다.
- 상단 로고, 언어 선택, 로그인/회원가입 버튼 및 하단 푸터 링크들의 정렬상태가 이전과 완전히 동일하게(깨짐 없이) 출력되는지 육안 및 브라우저 개발자 도구(F12)로 확인합니다.

### 3.2. 동작 제어 확인
- 언어 변경 셀렉터 변경 시 템플릿의 언어 리소스가 정상적으로 즉시 갱신되어 전환되는지 테스트합니다.
- 로그인 전과 후 세션 상태에 따라 헤더 우측의 세션 버튼들([로그인 / 회원가입] <-> [대시보드 보기 / 로그아웃])이 동적으로 스위칭되는지 검증합니다.
