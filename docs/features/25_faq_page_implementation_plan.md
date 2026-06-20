# FAQ (자주 묻는 질문) 페이지 구현 계획서

본 문서는 사용자가 서비스 이용에 관한 자주 묻는 질문들을 카테고리별로 모바일/데스크톱 대응형 아코디언 인터페이스를 통해 확인할 수 있는 FAQ 페이지의 구현 계획을 정리한 문서입니다.

---

## 1. 개요 및 요구사항

- **비로그인 유저 접근성**: 로그인하지 않은 일반 방문자도 FAQ를 확인할 수 있어야 하므로, 시스템 충돌이 없는 공용 경로인 `/info/faq`로 주소를 할당합니다.
- **동적 레이아웃 분기**:
  - **비회원**: 사이드바 없이 랜딩 페이지의 메인 헤더 및 하단 요소를 노출하여 가입 전환을 유도합니다.
  - **회원**: 기존 대시보드 구조에 맞춰 사이드바 및 대시보드 헤더를 노출하여 일관된 대시보드 사용성을 유지합니다.
- **카테고리 한정**: 유저의 요청에 따라 **'일반(General)'**, **'단축 링크(Short Links)'** 2가지 카테고리로 질문 데이터셋을 한정하여 구성합니다.
- **다국어(i18n) 지원**: 영어(EN) 및 한국어(KO) 다국어 메시지 리소스 파일에 질문과 답변을 정의하여 실시간 다국어 전환을 지원합니다.

---

## 2. 세부 변경 사항

### 2.1. 보안 필터 및 컨트롤러

#### [WebSecurityConfig.java](file:///c:/workdir/space-cap/pixel-link/src/main/java/com/pixellink/config/WebSecurityConfig.java)
- 보안 검증을 통과하도록 `.requestMatchers(...)` 허용 경로(`permitAll()`)에 `/info/faq`를 등록합니다.

#### [DashboardController.java](file:///c:/workdir/space-cap/pixel-link/src/main/java/com/pixellink/controller/DashboardController.java)
- `/info/faq` 엔드포인트를 매핑하여 세션 사용자 정보를 조회합니다.
- 세션 사용자 정보가 존재하는 경우 모델(Model)에 바인딩하여 뷰 템플릿에서 회원 레이아웃이 적용되도록 처리합니다.

```java
    @GetMapping("/info/faq")
    public String showFaq(HttpServletRequest request, Model model) {
        SessionUser sessionUser = (SessionUser) httpSession.getAttribute("user");
        if (sessionUser != null) {
            User user = userMapper.findById(sessionUser.getId());
            model.addAttribute("currentUser", user);
            model.addAttribute("currentUserId", sessionUser.getId());
        }
        return "faq";
    }
```

### 2.2. 다국어 리소스 파일 (i18n)

#### [messages_ko.properties](file:///c:/workdir/space-cap/pixel-link/src/main/resources/messages_ko.properties)
- **일반 (General)** 및 **단축 링크 (Short Links)** 관련 한국어 데이터를 정의합니다.
  - *일반 Q1*: 회원가입은 어떻게 하나요? / 회원가입 페이지에서 이름, 아이디, 비밀번호를 입력하여 간편하게 가입할 수 있습니다.
  - *일반 Q2*: 비밀번호를 잊어버렸어요. 어떻게 찾나요? / 비밀번호 분실 시 고객센터 문의 혹은 관리자 승인 절차를 이용해 주시기 바랍니다.
  - *단축 링크 Q1*: 비회원으로 생성한 링크는 언제 만료되나요? / 관리자 설정 기간(기본 30일)이 지나면 자동으로 만료되며, 회원으로 생성 시 무제한 보관됩니다.
  - *단축 링크 Q2*: 맞춤 숏코드를 설정하고 싶어요. / 회원 로그인 후 대시보드의 '고급 설정' 기능을 통해 자신만의 맞춤 숏코드를 발급받으실 수 있습니다.

#### [messages_en.properties](file:///c:/workdir/space-cap/pixel-link/src/main/resources/messages_en.properties)
- 영문 버전 FAQ 번역 데이터 세트를 동일 키로 정의합니다.

### 2.3. 화면 설계 및 레이아웃 (UI)

#### [faq.html](file:///c:/workdir/space-cap/pixel-link/src/main/resources/templates/faq.html)
- Thymeleaf `th:if="${currentUser != null}"` 조건 분기로 대시보드 조각(`fragments :: sidebar`, `fragments :: header`)을 가져오거나, 없을 경우 랜딩 헤더를 직접 노출합니다.
- 모던하고 깔끔한 UI를 제공하기 위해 다음 요소를 배치합니다:
  - **카테고리 탭(일반 / 단축 링크)**
  - **접이식(Accordion) 애니메이션 질문 리스트**: 클릭 시 CSS `max-height`와 `transition`을 활용해 자연스럽게 확장 및 축소되는 구조
  - CSS 다크모드 및 모던 글라스모피즘 스타일 코드 이식

#### [landing.html](file:///c:/workdir/space-cap/pixel-link/src/main/resources/templates/landing.html)
- 메인 홈페이지 상단 내비게이션 바에 `/info/faq`로 연결되는 링크를 추가합니다.

#### [fragments.html](file:///c:/workdir/space-cap/pixel-link/src/main/resources/templates/fragments.html)
- 로그인 유저가 사용하는 대시보드 내부 사이드바 하단 메뉴 리스트에 `/info/faq`로 진입할 수 있는 "고객지원 / FAQ" 바로가기를 추가합니다.

---

## 3. 검증 계획

### 3.1. 비로그인 접근 검증
- 브라우저 쿠키와 세션이 없는 시크릿 탭에서 `http://localhost:8090/info/faq`에 직접 접근하여, 사이드바가 노출되지 않고 상단 내비게이션(랜딩 페이지용 헤더)만 깔끔하게 나오는 것을 확인합니다.
- 메인 화면의 상단 메뉴에서 FAQ 클릭 시 리다이렉트나 차단 없이 정상 진입되는지 검증합니다.

### 3.2. 로그인 상태 접근 검증
- 회원 계정으로 로그인한 상태에서 사이드바의 FAQ 링크를 눌렀을 때, 대시보드 사이드바와 대시보드 상단 바가 있는 전체 레이아웃 내에서 FAQ 탭들이 깔끔하게 매핑 렌더링되는지 검증합니다.

### 3.3. 동적 아코디언 및 다국어 검증
- 질문 제목을 클릭했을 때 답변 카드가 부드러운 애니메이션 효과와 함께 열리고 닫히는지 확인합니다.
- 헤더의 KO / EN 국기 및 셀렉터를 통해 언어 변경 시, 정적으로 주입된 다국어 FAQ 리소스 텍스트가 실시간 전환되는지 최종 확인합니다.
