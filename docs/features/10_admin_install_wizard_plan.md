# 최초 관리자(ADMIN) 설치 마법사 및 감사 로그 설계서

본 문서는 최초 시스템 구동 시 어드민 계정을 동적으로 생성하는 **설치 마법사(Install Wizard)** 기능과, 보안 무결성 확보를 위한 **시스템 감사 로그(Audit Logs)**의 설계 및 구현 방안을 정리한 문서입니다. 

후임 개발자분들은 본 문서를 참고하여 유지보수 및 고도화를 진행해 주시기 바랍니다.

---

## 1. 아키텍처 및 설계 의도

최초 관리자 생성 여부를 판별하기 위해 전체 사용자 목록이나 감사 로그 전체를 매번 조회하는 것은 트래픽이 집중될 때 심각한 DB 성능 병목을 초래할 수 있습니다. 

이를 해결하기 위해 **조회 성능을 위한 상태값(State)**과 **보안 추적을 위한 이벤트 로그(Event Log)**를 분리하는 **하이브리드(Hybrid) 설계**를 채택했습니다.

1. **상태값 관리 (`system_settings`)**:
   - `is_installed = 'true'` 상태를 단일 레코드로 보관합니다.
   - `/app/install` 진입 시 이 설정값 하나만 인덱스 스캔(혹은 기본키 조회)하므로, 전체 유저 수나 로그 수와 무관하게 항상 `O(1)`의 최고 속도로 설치 여부를 판별합니다.
2. **이력 관리 (`system_audit_logs`)**:
   - 실제로 설치를 실행한 관리자 ID, IP 주소, 브라우저 정보(User-Agent), 설치 시간 등을 상세하게 기록합니다.
   - 향후 출금 승인, 환경설정 변경 등 주요 보안 이벤트 로깅에도 통합 재사용될 수 있도록 공통 감사 로그 스키마로 설계했습니다.

---

## 2. 데이터베이스 스키마 정의

### 2.1. 로컬 환경 (SQLite) - `schema-sqlite.sql`
```sql
-- system_audit_logs 테이블 정의
CREATE TABLE IF NOT EXISTS system_audit_logs (
    id TEXT PRIMARY KEY,
    event_type TEXT NOT NULL,       -- 이벤트 구분 (예: 'SYSTEM_INSTALL')
    actor_id TEXT,                  -- 행위자 ID (관리자 ID)
    ip_address TEXT,                -- 요청 IP
    user_agent TEXT,                -- 브라우저 정보
    created_at TEXT DEFAULT (datetime('now', 'localtime')) -- 발생 시간
);

-- 초기 설치 플래그 설정 (기본값: false)
INSERT OR IGNORE INTO system_settings (setting_key, setting_value, description)
VALUES ('is_installed', 'false', '최초 시스템 설치 여부 (true/false)');
```

### 2.2. 상용 운영 환경 (PostgreSQL) - `schema-postgresql.sql`
```sql
-- system_audit_logs 테이블 정의
CREATE TABLE IF NOT EXISTS system_audit_logs (
    id VARCHAR(50) PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    actor_id VARCHAR(50),
    ip_address VARCHAR(50),
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 초기 설치 플래그 설정 (기본값: false)
INSERT INTO system_settings (setting_key, setting_value, description)
VALUES ('is_installed', 'false', '최초 시스템 설치 여부 (true/false)')
ON CONFLICT (setting_key) DO NOTHING;
```

---

## 3. 핵심 비즈니스 흐름 및 로직

### 3.1. GET `/app/install` (설치 화면 진입)
1. `systemSettingMapper.findByKey("is_installed")`를 실행해 값을 조회합니다.
2. 값이 존재하고 `"true"` 문자열과 일치할 경우:
   - 이미 설치가 완료된 시스템이므로 즉시 홈 화면(`/`)으로 **강제 리다이렉트**하여 노출을 원천 차단합니다.
3. 값이 `"false"`이거나 설정이 존재하지 않을 경우:
   - 설치 화면인 `install.html` 템플릿을 사용자 브라우저에 렌더링합니다.

### 3.2. POST `/app/install/process` (관리자 생성 처리)
1. 중복 요청 방지를 위해 다시 한번 `is_installed` 값을 확인합니다.
2. 입력받은 데이터로 `User` 객체를 빌드하여 저장합니다:
   - 비밀번호는 스프링 시큐리티의 `PasswordEncoder`를 사용하여 안전하게 해싱(`bcrypt`)한 뒤 저장합니다.
   - 역할(`role`)은 `"ADMIN"`으로 하드코딩 부여하고, 구독 등급(`subscriptionTier`)은 `"PREMIUM"`으로 설정합니다.
3. `SystemAuditLog` 객체를 빌드하여 감사 로그를 DB에 인서트합니다:
   - `id`: UUID 생성 적용.
   - `eventType`: `"SYSTEM_INSTALL"`.
   - `actorId`: 생성된 관리자 ID.
   - `ipAddress`: `HttpServletRequest.getRemoteAddr()`.
   - `userAgent`: `request.getHeader("User-Agent")`.
4. `system_settings` 테이블의 `is_installed` 값을 `"true"`로 업데이트합니다.
5. 가입 성공 완료 파라미터를 담아 로그인 페이지 `/app/login?installed=true`로 리다이렉트합니다.

---

## 4. 후임자를 위한 확장 가이드
본 설계의 감사 로그 테이블(`system_audit_logs`)은 범용 감사 로그로 설계되어 있습니다. 향후 아래와 같은 기능을 추가할 때 재활용하십시오:

1. **설정값 변경 추적**:
   - `AdminController`에서 시스템 설정을 변경할 때, `event_type = 'SETTING_CHANGE'`로 로그를 인서트합니다.
2. **정산금 출금 승인 추적**:
   - 관리자가 마케터의 출금 신청을 승인할 때, `event_type = 'SETTLEMENT_APPROVE'`, `actor_id = {admin_id}`, `details = {정산ID: xxx, 금액: xxx}` 형태로 기록을 남기십시오.
3. **사용자 권한 강제 조율**:
   - 특정 유저를 강제 차단하거나 등급을 변경할 때 `event_type = 'USER_ROLE_UPDATE'`로 감사 로그를 남겨 보안 투명성을 유지해 주시기 바랍니다.
