# 기업용 일반 회원가입 및 ID/비밀번호 로그인 기능 개발 계획

본 계획서는 소셜 계정이 없는 기업(Corporate) 사용자들을 위해 **아이디/비밀번호 기반의 일반 회원가입 및 로그인 체계**를 구축하는 방안을 기술합니다. 
개인은 편리하게 소셜 로그인(OAuth2)을 이용하고, 기업은 안전하게 자체 계정을 생성하여 대시보드 기능을 이용할 수 있도록 하이브리드 인증 방식을 지원합니다.

---

## User Review Required

> [!IMPORTANT]
> **기업 회원가입 및 필수 입력 정보 사양**
> - **필수 입력**: 이름(회사명/담당자명), 아이디(ID), 비밀번호 (암호화 저장)
> - **선택 입력**: 이메일, 휴대폰 번호
> - **필수 약관 동의**: 서비스 이용약관 및 개인정보 처리방침 동의 체크박스
> - **보안 조치**: 비밀번호는 스프링 시큐리티 권장 사양인 `BCryptPasswordEncoder`로 암호화하여 DB에 저장합니다.
> - **인증 체계 통합**: 로그인 후 세션 바인딩 방식은 기존 OAuth2와 동일하게 `SessionUser` 객체를 HTTP 세션 `"user"` 속성에 저장하므로, 대시보드와 비즈니스 컨트롤러 코드의 수정 없이 완벽히 호환됩니다.

---

## Proposed Changes

### [Database Configurations]

#### [MODIFY] [schema-sqlite.sql](file:///h:/lee/pixel-link/src/main/resources/db/schema-sqlite.sql)
- `users` 테이블에 기업 회원 및 일반 로그인을 위한 컬럼 추가:
  - `password TEXT` (암호화된 비밀번호, 소셜 유저는 null)
  - `name TEXT` (사용자명/회사명)
  - `phone TEXT` (휴대폰 번호, 선택)
  - `terms_agreed INTEGER DEFAULT 0` (약관 동의 여부, 1: 동의)
  - `role TEXT DEFAULT 'USER'` (권한 컬럼 추가 반영 확인)
- 기존 `email TEXT UNIQUE NOT NULL` 제약 조건을 `email TEXT`로 변경하여 이메일을 입력하지 않는 기업 사용자도 가입할 수 있도록 개선합니다. (SQLite의 경우 다중 NULL값은 UNIQUE 제약에 걸리지 않지만, 필수 조건 `NOT NULL`을 제거합니다)

#### [MODIFY] [schema-postgresql.sql](file:///h:/lee/pixel-link/src/main/resources/db/schema-postgresql.sql)
- `users` 테이블 정의에 동일한 컬럼 및 제약조건 완화 반영:
  - `password VARCHAR(255)`
  - `name VARCHAR(100)`
  - `phone VARCHAR(50)`
  - `terms_agreed BOOLEAN DEFAULT FALSE`
  - `role VARCHAR(50) DEFAULT 'USER'` (기본 컬럼 추가)
  - `email VARCHAR(255)` (NULL 허용으로 변경)
- 기존에 구동 중인 운영 DB 테이블 구조 변경을 위해 안전하게 컬럼을 추가/변경하는 ALTER TABLE 스크립트를 스키마 하단에 작성합니다.

---

### [Backend Models & Mappers]

#### [MODIFY] [User.java](file:///h:/lee/pixel-link/src/main/java/com/pixellink/model/User.java)
- 새롭게 추가된 데이터베이스 컬럼에 상응하는 자바 멤버 변수 추가:
  - `private String password;`
  - `private String name;`
  - `private String phone;`
  - `private boolean termsAgreed;`
  - `private String role;` (존재 유무 확인 후 바인딩)

#### [MODIFY] [SessionUser.java](file:///h:/lee/pixel-link/src/main/java/com/pixellink/dto/SessionUser.java)
- 세션 보관 시 필요한 사용자명(`name`) 필드 추가 반영 및 생성자 수정.

#### [MODIFY] [UserMapper.xml](file:///h:/lee/pixel-link/src/main/resources/mapper/UserMapper.xml)
- `insert` 및 `update` SQL 매핑 구문에 `password`, `name`, `phone`, `terms_agreed` 컬럼 바인딩 추가.

---

### [Security Config & User Details]

#### [MODIFY] [WebSecurityConfig.java](file:///h:/lee/pixel-link/src/main/java/com/pixellink/config/WebSecurityConfig.java)
- `BCryptPasswordEncoder` Bean 등록.
- `SecurityFilterChain` 설정에 `.formLogin(...)` 추가 연동:
  - 로그인 페이지 경로 `/app/login` 지정.
  - 로그인 처리 엔드포인트 `/app/login/process` 설정.
  - 로그인 성공 시 핸들러를 정의하여, 로그인된 유저의 `User` 엔티티 정보를 `SessionUser`로 변환해 HTTP 세션 `"user"` 속성에 저장 후 `/app/dashboard`로 리다이렉트합니다.
  - 로그인 실패 시 `/app/login?error=true` 리다이렉트.
- 비인가 허용 경로 리스트에 회원가입 경로 추가: `/app/signup`, `/app/signup/process`

#### [NEW] [CustomUserDetails.java](file:///h:/lee/pixel-link/src/main/java/com/pixellink/config/CustomUserDetails.java)
- 스프링 시큐리티 `UserDetails` 인터페이스를 구현하며, DB `User` 객체를 감싸 인증 처리를 돕습니다.

#### [NEW] [CustomUserDetailsService.java](file:///h:/lee/pixel-link/src/main/java/com/pixellink/config/CustomUserDetailsService.java)
- `UserDetailsService` 인터페이스를 구현하여, 로그인 시 DB로부터 `UserMapper.findById(username)`를 수행하고 비밀번호 일치 검사를 처리합니다.

---

### [Controllers & View Templates]

#### [MODIFY] [DashboardController.java](file:///h:/lee/pixel-link/src/main/java/com/pixellink/controller/DashboardController.java)
- 일반 회원가입 뷰 및 가입 처리 엔드포인트 추가:
  - `@GetMapping("/app/signup")` -> 회원가입 폼 페이지 렌더링.
  - `@PostMapping("/app/signup/process")` -> 아이디 중복 체크, 비밀번호 해싱, 필수값 검증 후 `User` 생성 및 DB 인서트. 가입 완료 시 로그인 페이지로 리다이렉트.

#### [MODIFY] [login.html](file:///h:/lee/pixel-link/src/main/resources/templates/login.html)
- 기존 소셜 로그인 버튼 상단에 **아이디/비밀번호 일반 로그인 폼** 영역 추가.
- 프리미엄 다크 글래스모피즘 테마의 Input 필드 스타일 설계.
- 가입 유도를 위한 "계정이 없으신가요? [기업 회원가입]" 링크 배치.
- 로그인 오류 시 경고창 렌더링(`th:if="${param.error}"`).

#### [NEW] [signup.html](file:///h:/lee/pixel-link/src/main/resources/templates/signup.html)
- 기업용 회원가입 레이아웃 신설.
- 이름/회사명, 아이디, 비밀번호, 비밀번호 확인, 이메일(선택), 휴대폰번호(선택) 입력 필드 제공.
- 서비스 이용약관 및 개인정보 처리방침 스크롤 박스 및 동의 체크박스 구현.
- 비밀번호 일치 확인 및 필수 동의 체크 여부 클라이언트 측 검증(JavaScript) 적용.

---

## Verification Plan

### Automated Tests
- `SignupAndLoginTest.java` 통합 테스트 신설:
  - 기업 회원가입 API 호출 성공 시 DB 저장값 및 암호화 여부 검증.
  - 중복 아이디 가입 요청 시 에러 응답 검증.
  - 가입한 계정으로 Form Login 시도 시 정상적으로 세션 정보 등록 및 200 OK(대시보드 리다이렉션) 검증.

### Manual Verification
1. `/app/signup`에 접속하여 기업명, 아이디, 패스워드를 입력하고 약관을 체크한 뒤 가입이 잘 완료되는지 확인합니다.
2. 가입 시 아이디 중복 체크 및 비밀번호 확인 일치 조건이 잘 타는지 테스트합니다.
3. `/app/login`에서 방금 가입한 아이디/비밀번호로 로그인하여 세션이 성공적으로 바인딩되고 `/app/dashboard`로 잘 들어가는지 최종 점검합니다.
