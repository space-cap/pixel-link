# Pixel-Link 데이터베이스 상세 명세서

본 문서는 Pixel-Link 서비스의 1단계부터 4단계까지의 비즈니스 확장을 고려하여 설계된 데이터베이스 물리 스키마 명세서입니다. 
로컬 환경(SQLite) 및 상용 환경(PostgreSQL)의 100% 호환을 보장하기 위해 표준 컬럼 타입 명칭 및 역할을 정의합니다.

---

## 1. 전체 ERD 관계 요약

* **users** (회원) - 구독 등급 및 상용 서비스 권한 관리
  * `1:N` 관계 ➡️ **links** (단축 링크)
  * `1:N` 관계 ➡️ **settlements** (수익 정산 내역)
* **links** (단축 링크) - 단축 URL 설정 및 메타 정보
  * `1:N` 관계 ➡️ **route_rules** (기기/OS별 스마트 라우팅 규칙)
  * `1:N` 관계 ➡️ **click_logs** (접속 및 클릭 트래킹 로그)

---

## 2. 테이블 상세 명세

### 2.1. `users` (회원 테이블)
시스템 사용자의 계정 및 유료 구독 권한을 관리합니다. (1단계 SaaS 비즈니스 및 3~4단계 정산 모델의 주체)

| 컬럼명 | SQLite 타입 | PostgreSQL 타입 | Null 여부 | 제약조건 | 설명 | 비즈니스 단계 |
| :--- | :--- | :--- | :---: | :--- | :--- | :---: |
| `id` | TEXT | VARCHAR(50) | N.N | PRIMARY KEY | 회원 고유 식별자 (UUID 등) | 1단계 |
| `email` | TEXT | VARCHAR(255) | N.N | UNIQUE | 로그인 및 알림용 이메일 주소 | 1단계 |
| `subscription_tier` | TEXT | VARCHAR(50) | N.N | DEFAULT 'FREE' | 구독 티어 (`FREE`, `STARTER`, `PREMIUM`) | 1단계 (구독형) |
| `subscription_ends_at` | TEXT | TIMESTAMP WITH TZ | Null | | 유료 구독 만료 일시 | 1단계 (구독형) |

* **인덱스**: `idx_users_email` (email 검색 속도 최적화)

---

### 2.2. `links` (단축 링크 테이블)
단축 URL 정보와 각 링크에 심어질 메타 태그, 추적 픽셀 설정 및 3~4단계 수익 설정 정보를 저장합니다.

| 컬럼명 | SQLite 타입 | PostgreSQL 타입 | Null 여부 | 제약조건 | 설명 | 비즈니스 단계 |
| :--- | :--- | :--- | :---: | :--- | :--- | :---: |
| `id` | TEXT | VARCHAR(50) | N.N | PRIMARY KEY | 링크 고유 식별자 (UUID 등) | 1단계 |
| `user_id` | TEXT | VARCHAR(50) | N.N | FOREIGN KEY | 링크 생성 회원 ID (users.id 참조) | 1단계 |
| `short_code` | TEXT | VARCHAR(100) | N.N | UNIQUE | 단축 주소의 고유 슬러그 (예: `s/abcde`) | 1단계 |
| `default_target_url` | TEXT | TEXT | N.N | | 매칭 조건이 없을 때 이동할 기본 목적지 URL | 1단계 |
| `title` | TEXT | VARCHAR(255) | Null | | SNS 공유 미리보기용 SEO 제목 | 1단계 |
| `description` | TEXT | TEXT | Null | | SNS 공유 미리보기용 SEO 설명 | 1단계 |
| `og_image` | TEXT | TEXT | Null | | SNS 공유 미리보기용 대표 이미지 URL | 1단계 |
| `fb_pixel_id` | TEXT | VARCHAR(50) | Null | | 페이스북 픽셀 ID (Facebook Pixel) | 1단계 |
| `ga_tracking_id` | TEXT | VARCHAR(50) | Null | | 구글 애널리틱스 측정 ID (GA4) | 1단계 |
| `custom_script` | TEXT | TEXT | Null | | 사용자 정의 추가 트래킹 스크립트 | 1단계 |
| `is_ad_enabled` | INTEGER | BOOLEAN | N.N | DEFAULT FALSE | 중간 페이지 광고 노출 활성화 여부 | 3단계 (광고형) |
| `ad_timer_seconds` | INTEGER | INTEGER | N.N | DEFAULT 1 | 리다이렉트 대기 시간 (광고 노출 시간) | 3단계 (광고형) |
| `is_paywalled` | INTEGER | BOOLEAN | N.N | DEFAULT FALSE | 결제 잠금(페이월) 활성화 여부 | 4단계 (결제형) |
| `price` | INTEGER | INTEGER | N.N | DEFAULT 0 | 페이월 잠금 해제 결제 금액 (원) | 4단계 (결제형) |
| `clicks_count` | INTEGER | INTEGER | N.N | DEFAULT 0 | 단순 누적 클릭 수 (대시보드 노출용) | 1단계 |
| `created_at` | TEXT | TIMESTAMP WITH TZ | N.N | DEFAULT Current | 링크 생성 일시 | 1단계 |

* **인덱스**: `idx_links_short_code` (단축 링크 조회가 주 작업이므로 반드시 인덱스 설정)

---

### 2.3. `route_rules` (스마트 라우팅 규칙 테이블)
접속 환경 분석 결과(OS, 기기 등)에 따라 기본 목적지 대신 특정 앱링크 또는 다른 목적지 URL로 보낼 분기 규칙을 지정합니다.

| 컬럼명 | SQLite 타입 | PostgreSQL 타입 | Null 여부 | 제약조건 | 설명 | 비즈니스 단계 |
| :--- | :--- | :--- | :---: | :--- | :--- | :---: |
| `id` | TEXT | VARCHAR(50) | N.N | PRIMARY KEY | 규칙 고유 식별자 | 2단계 |
| `link_id` | TEXT | VARCHAR(50) | N.N | FOREIGN KEY | 소속 링크 ID (links.id 참조) | 2단계 |
| `rule_type` | TEXT | VARCHAR(50) | N.N | | 규칙 분류 기준 (`OS`, `DEVICE`, `TIME`, `COUNTRY`) | 2단계 (스마트) |
| `rule_value` | TEXT | VARCHAR(255) | N.N | | 세부 분류 매치 조건 (`iOS`, `Android`, `Desktop` 등) | 2단계 (스마트) |
| `target_url` | TEXT | TEXT | N.N | | 조건에 부합할 경우 이동시킬 타겟 URL (앱 딥링크 포함) | 2단계 (스마트) |

* **인덱스**: `idx_route_rules_link_id` (동일 링크 하위 규칙 일괄 조회 최적화)

---

### 2.4. `click_logs` (접속 로그 테이블)
링크 클릭이 일어날 때마다 발생하는 상세 접속 정보(User-Agent, IP, 레퍼러 등)를 수집하여 통계 및 광고 부정 클릭 방지, 결제 전환 분석에 활용합니다.

| 컬럼명 | SQLite 타입 | PostgreSQL 타입 | Null 여부 | 제약조건 | 설명 | 비즈니스 단계 |
| :--- | :--- | :--- | :---: | :--- | :--- | :---: |
| `id` | TEXT | VARCHAR(50) | N.N | PRIMARY KEY | 로그 고유 식별자 | 1단계 |
| `link_id` | TEXT | VARCHAR(50) | N.N | FOREIGN KEY | 클릭된 링크 ID (links.id 참조) | 1단계 |
| `user_agent` | TEXT | TEXT | Null | | 접속자의 브라우저 정보 (User-Agent) | 1단계 |
| `ip_hash` | TEXT | VARCHAR(64) | N.N | | 접속자 IP 주소의 단방향 해시값 (개인정보 보호용) | 1단계 |
| `referrer` | TEXT | TEXT | Null | | 이전 유입 경로 (Referer URL) | 1단계 |
| `device_type` | TEXT | VARCHAR(50) | Null | | 기기 종류 (`MOBILE`, `DESKTOP`, `TABLET`) | 2단계 |
| `os_type` | TEXT | VARCHAR(50) | Null | | 운영체제 종류 (`iOS`, `Android`, `Windows`, `macOS`) | 2단계 |
| `is_ad_clicked` | INTEGER | BOOLEAN | N.N | DEFAULT FALSE | 중간 페이지 광고 영역 클릭 여부 | 3단계 (광고형) |
| `is_converted` | INTEGER | BOOLEAN | N.N | DEFAULT FALSE | 페이월 결제 및 최종 링크 도달 성공 여부 | 4단계 (결제형) |
| `timestamp` | TEXT | TIMESTAMP WITH TZ | N.N | DEFAULT Current | 클릭 발생 일시 | 1단계 |

* **인덱스**: 
  * `idx_click_logs_link_id` (통계 집계용)
  * `idx_click_logs_timestamp` (최근 클릭 추이 통계용)

---

### 2.5. `settlements` (수익 정산 테이블)
광고형 단축 링크(3단계)를 통해 발생한 유저별 광고 적립금 및 수익금 환급 상태를 관리합니다.

| 컬럼명 | SQLite 타입 | PostgreSQL 타입 | Null 여부 | 제약조건 | 설명 | 비즈니스 단계 |
| :--- | :--- | :--- | :---: | :--- | :--- | :---: |
| `id` | TEXT | VARCHAR(50) | N.N | PRIMARY KEY | 정산 내역 고유 식별자 | 3단계 |
| `user_id` | TEXT | VARCHAR(50) | N.N | FOREIGN KEY | 정산 대상 유저 ID (users.id 참조) | 3단계 |
| `amount` | INTEGER | INTEGER | N.N | | 정산/지급액 (원) | 3단계 (광고형) |
| `status` | TEXT | VARCHAR(50) | N.N | DEFAULT 'PENDING' | 정산 지급 상태 (`PENDING`, `COMPLETED`, `REJECTED`) | 3단계 (광고형) |
| `settled_at` | TEXT | TIMESTAMP WITH TZ | Null | | 지급/처리 완료 일시 | 3단계 (광고형) |
| `created_at` | TEXT | TIMESTAMP WITH TZ | N.N | DEFAULT Current | 정산 신청/발생 일시 | 3단계 (광고형) |

---

## 3. 이중 데이터베이스(SQLite / PostgreSQL) 지원 전략

1. **테이블 생성 스크립트 분리**:
   * 프로젝트의 `src/main/resources/db/` 경로 하위에 `schema-sqlite.sql`과 `schema-postgresql.sql`을 따로 보관하여, Active Profile에 맞추어 애플리케이션 시작 시점에 DDL 스크립트가 자동 수행되도록 합니다.
2. **Boolean / DateTime 표현 차이 처리**:
   * Boolean 처리: SQLite는 BOOLEAN 타입을 INTEGER(0 또는 1)로 변환해 저장합니다. MyBatis XML Mapper 파일 내부에서 TypeHandler를 사용하거나 MyBatis 설정인 `mapUnderscoreToCamelCase`를 켜두면, Java 객체의 `boolean` 필드와 SQLite의 `INTEGER`/PostgreSQL의 `BOOLEAN` 타입 간 매핑이 자동으로 원활하게 처리됩니다.
   * DateTime 처리: SQLite의 기본 타임스탬프(`datetime('now', 'localtime')`)와 PostgreSQL의 타임스탬프(`CURRENT_TIMESTAMP`)가 다릅니다. MyBatis 쿼리 내에서 SQL을 직접 작성할 때는 ANSI 표준에 맞추거나 프로필 분기를 사용합니다.
