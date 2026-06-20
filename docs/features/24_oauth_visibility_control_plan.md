# 로그인 페이지 소셜 로그인(OAuth2) 버튼 노출 제어 설계서

본 문서는 최고 관리자가 어드민 환경 설정을 통해 로그인 페이지의 소셜 로그인 수단(Google, Facebook, Naver, Kakao)의 활성화 여부를 실시간으로 켜고 끌 수 있는 토글 제어 기능의 설계 및 구현 방안을 정리한 문서입니다.

후임 개발자분들은 본 문서를 참고하여 유지보수 및 고도화를 진행해 주시기 바랍니다.

---

## 1. 아키텍처 및 설계 의도

1. **상태값 영속화 (`system_settings`)**:
   - 4대 소셜 로그인 수단의 활성화 상태를 `oauth_google_enabled`, `oauth_facebook_enabled`, `oauth_naver_enabled`, `oauth_kakao_enabled` 키로 구분하여 저장합니다.
   - 키의 데이터타입 및 포맷은 기존 설정을 재활용하여 문자열 `"true"` 혹은 `"false"`로 매핑합니다.
2. **효율적인 동적 노출 (Thymeleaf)**:
   - 로그인 진입(`GET /app/login`) 시점에 각 설정값을 한 번에 조회하여 모델에 담습니다.
   - 템플릿 엔진 단에서 조건부 렌더링(`th:if`)을 사용해 서버 부하 없이 클라이언트에 필요한 마크업만 전달합니다.
   - 4개 버튼이 모두 비활성화될 시 디바이더("또는 소셜 계정으로 로그인")도 자동 숨김 처리하여 화면의 공백 정렬 완성도를 극대화합니다.

---

## 2. 데이터베이스 설정값 구성

### 2.1. 로컬 환경 (SQLite) - `schema-sqlite.sql`
```sql
-- 소셜 로그인 활성화 여부 초기값 추가 (기본값: true)
INSERT OR IGNORE INTO system_settings (setting_key, setting_value, description) VALUES ('oauth_google_enabled', 'true', 'Google 소셜 로그인 노출 여부 (true/false)');
INSERT OR IGNORE INTO system_settings (setting_key, setting_value, description) VALUES ('oauth_facebook_enabled', 'true', 'Facebook 소셜 로그인 노출 여부 (true/false)');
INSERT OR IGNORE INTO system_settings (setting_key, setting_value, description) VALUES ('oauth_naver_enabled', 'true', 'Naver 소셜 로그인 노출 여부 (true/false)');
INSERT OR IGNORE INTO system_settings (setting_key, setting_value, description) VALUES ('oauth_kakao_enabled', 'true', 'Kakao 소셜 로그인 노출 여부 (true/false)');
```

### 2.2. 상용 운영 환경 (PostgreSQL) - `schema-postgresql.sql`
```sql
-- 소셜 로그인 활성화 여부 초기값 추가 (기본값: true)
INSERT INTO system_settings (setting_key, setting_value, description) VALUES ('oauth_google_enabled', 'true', 'Google 소셜 로그인 노출 여부 (true/false)') ON CONFLICT (setting_key) DO NOTHING;
INSERT INTO system_settings (setting_key, setting_value, description) VALUES ('oauth_facebook_enabled', 'true', 'Facebook 소셜 로그인 노출 여부 (true/false)') ON CONFLICT (setting_key) DO NOTHING;
INSERT INTO system_settings (setting_key, setting_value, description) VALUES ('oauth_naver_enabled', 'true', 'Naver 소셜 로그인 노출 여부 (true/false)') ON CONFLICT (setting_key) DO NOTHING;
INSERT INTO system_settings (setting_key, setting_value, description) VALUES ('oauth_kakao_enabled', 'true', 'Kakao 소셜 로그인 노출 여부 (true/false)') ON CONFLICT (setting_key) DO NOTHING;
```

---

## 3. 핵심 비즈니스 흐름 및 로직

### 3.1. GET `/app/login` (로그인 페이지 진입)
1. `LinkService.getSystemSetting(key, defaultValue)`을 호출하여 4가지 소셜 로그인의 활성화 여부를 조회합니다.
2. 각 결과를 아래의 모델 속성에 매핑하여 전달합니다:
   - `googleEnabled`, `facebookEnabled`, `naverEnabled`, `kakaoEnabled`
3. 템플릿(`login.html`) 내에서 렌더링 조건을 검사하여 동적으로 출력합니다.

### 3.2. POST `/app/admin/settings/update` (어드민 설정 업데이트)
1. HTML 표준 명세 상 체크 해제(OFF) 상태의 체크박스는 폼 전송 파라미터 맵(`settings`)에 **키 자체가 포함되지 않습니다.**
2. 이 누락 문제를 안전하게 처리하기 위해, 백엔드 저장 프로세스 진입 전 아래 보완 로직을 추가합니다:
   ```java
   String[] oauthKeys = {"oauth_google_enabled", "oauth_facebook_enabled", "oauth_naver_enabled", "oauth_kakao_enabled"};
   for (String k : oauthKeys) {
       if (!settings.containsKey(k)) {
           settings.put(k, "false"); // 체크 해제된 스위치는 false로 명시적 저장
       }
   }
   ```
3. 보완된 맵 데이터로 설정을 일괄 업데이트합니다.

---

## 4. UI 구성 가이드

### 4.1. 어드민 설정 토글 디자인 (`admin/settings.html`)
- **설정 그룹명**: `소셜 로그인 노출 설정`
- **구현 방식**: 4대 플랫폼(Google, Facebook, Naver, Kakao)의 활성화 여부를 체크박스로 배치합니다. 
- 스타일 가이드는 라이트 다크 모드 통합용 CSS 규칙에 맞춥니다.

### 4.2. 로그인 페이지 동적 노출 (`login.html`)
- Thymeleaf 문법 `th:if` 를 사용해 마크업 영역을 제어합니다.
- 예시:
  ```html
  <a th:if="${googleEnabled}" href="/oauth2/authorization/google" class="social-btn btn-google">...</a>
  ```
