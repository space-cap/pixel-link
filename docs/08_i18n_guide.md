# Pixel-Link 다국어(i18n) 지원 및 확장 가이드

본 문서는 Pixel-Link 서비스의 글로벌 다국어(Internationalization, i18n) 지원 구조와 신규 언어 팩을 추가하는 방법을 설명합니다.

---

## 1. 다국어 지원 아키텍처

스프링 부트(Spring Boot) 및 타임리프(Thymeleaf) 표준 다국어 스펙을 적용하여 구축되었습니다.

* **쿠키 기반 언어 감지 및 보존 (`CookieLocaleResolver`)**:
  * 유저의 브라우저 언어 기본 설정을 최초 언어로 세팅합니다.
  * 유저가 언어를 직접 변경하면 `lang` 이름의 쿠키에 설정값이 암묵적으로 저장되어, 브라우저를 껐다 켜거나 재접속해도 **30일간 언어 선택 정보가 유지**됩니다.
* **파라미터 기반 전환 (`LocaleChangeInterceptor`)**:
  * URL 뒤에 `?lang=ko`, `?lang=en`, `?lang=ja` 등의 쿼리 파라미터를 붙여 접속하는 즉시 해당 세션의 로케일이 동적으로 전환됩니다.
* **타임리프 템플릿 주입 (`#{key}`)**:
  * 대시보드 화면 및 리다이렉트 대기 화면의 모든 하드코딩 텍스트를 제거하고, properties 파일에 저장된 번들 값을 호출합니다.
  * JavaScript 내의 Toast 알림 및 Confirm 메시지 역시 타임리프의 인라인 렌더링 기능(`/*[[#{key}]]*/`)을 이용해 완벽히 동적 현지화 번역을 수행합니다.

---

## 2. 신규 언어 추가 방법 (예시: 중국어 - `zh`)

시스템 아키텍처가 완전한 모듈형 번들 구조로 분리되어 있으므로, 백엔드 Java 코드나 HTML 구조를 단 한 줄도 수정하지 않고 **새로운 파일 한 개만 추가**하는 것으로 다국어를 확장할 수 있습니다.

### Step 1. 새로운 언어 팩 properties 파일 생성
`src/main/resources/` 경로에 `messages_[언어코드].properties` 규칙에 맞춰 파일을 생성합니다.
* 예: 중국어 간체의 경우 ➡️ `src/main/resources/messages_zh.properties` 생성

### Step 2. 다국어 번역 키-값 작성
생성한 파일에 기존 영어(`messages.properties`) 번들을 참고하여 중국어 번역 쌍을 채워 넣습니다.
```properties
# messages_zh.properties 예시
dashboard.title=Pixel-Link 管理控制台
dashboard.logo_badge=高级链接器
dashboard.lang_select=切换语言
dashboard.user_switch=切换账户
...
```

### Step 3. 대시보드 헤더의 언어 셀렉트 박스에 옵션 추가
[src/main/resources/templates/dashboard.html](file:///h:/lee/pixel-link/src/main/resources/templates/dashboard.html)의 상단 언어 선택 `<select>` 태그 내부에 새로운 언어 옵션을 한 줄만 추가합니다.
```html
<select class="select-user" onchange="switchLanguage(this.value)">
    <option value="en" th:selected="${#locale.language == 'en'}">English</option>
    <option value="ko" th:selected="${#locale.language == 'ko'}">한국어</option>
    <option value="ja" th:selected="${#locale.language == 'ja'}">日本語</option>
    <!-- 신규 추가 -->
    <option value="zh" th:selected="${#locale.language == 'zh'}">简体中文</option> 
</select>
```

위의 3단계 설정만 마치면 새로운 국가 사용자를 위한 번역 패키지가 즉시 반영되고 동작합니다.
