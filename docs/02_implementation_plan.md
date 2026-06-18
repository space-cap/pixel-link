# Pixel-Link 전체 상용화 로드맵 및 1단계 구현 계획 (Spring Boot + SQLite/PostgreSQL)

본 계획서는 '숏 URL 리다이렉션 백엔드 파이프라인'을 중심으로 **1단계(마케팅 픽셀)부터 4단계(페이월 콘텐츠 잠금)**까지 수익화 비즈니스를 유기적으로 확장할 수 있도록 설계된 전체 아키텍처 및 1단계 구현 계획입니다.

개발 편의성을 극대화하기 위해 로컬 개발 단계에서는 **SQLite**를 사용하고, 상용 환경에서는 대용량 트래픽 및 JSON 데이터 제어가 강력한 **PostgreSQL**을 사용하도록 데이터베이스 이중화 전략을 적용합니다.

---

## 🧱 전체 비즈니스 & 시스템 아키텍처 로드맵

시스템은 각 단계가 독립적인 모듈처럼 작동하며 기존 파이프라인을 훼손하지 않고 확장될 수 있도록 데이터베이스와 라우팅 엔진을 사전에 설계합니다.

```
[ 4단계: 페이월 콘텐츠 잠금 (수수료) ]  --> PG사 연동 + 세션/토큰 검증 인터셉터
       ▲
[ 3단계: 광고 포함형 숏링크 (광고수익) ] --> 타이머/애드센스 노출 + 정산(Settlement) MyBatis 배치 쿼리
       ▲
[ 2단계: 스마트 라우팅 & 딥링크 (구독) ] --> User-Agent 분석 미들웨어 + PostgreSQL JSONB 라우팅 분기
       ▲
[ 1단계: 마케팅 픽셀 & 기본 단축 (SaaS) ] --> [현재 작업] Thymeleaf 기반 초고속 픽셀 주입 및 리다이렉트 엔진
```

### 1단계: 파이프라인 기초 공사 (마케팅 픽셀 + 기본 단축)
* **목표**: 요청이 들어오면 중간 페이지(Intermediary Page)를 거쳐 목적지로 리다이렉트하는 코어 엔진 구축.
* **수익화**: 마케터 대상 월 구독형 SaaS 출시. 페이스북/구글 픽셀 삽입 지원.

### 2단계: 라우팅 로직 확장 (스마트 라우팅 & 딥링크)
* **목표**: 리다이렉트 파이프라인에 동적 조건문(If-Else) 라우팅 규칙 적용.
* **수익화**: B2B 상위 요금제 신설. 모바일 접속 시 쿠팡/네이버쇼핑 앱 등으로 자동 이동시키는 앱링크(App Link)/딥링크 지원 및 OS/시간별 타겟팅 분기.

### 3단계: 중간 페이지 비즈니스 다각화 (광고 숏링크)
* **목표**: 리다이렉트 대기 화면(중간 페이지)에 광고 노출(애드센스 등) 및 5초 타이머 추가.
* **수익화**: 트래픽 유발 유저에게 광고 수익 쉐어. 링크 클릭/광고 노출 로그를 수집하여 포인트를 정산하는 정산 DB 테이블 및 배치 엔진 추가.

### 4단계: 최종 진화 (페이월 & 콘텐츠 잠금)
* **목표**: 중간 페이지에 결제 모듈(토스페이먼츠 등) 결합.
* **수익화**: 유저가 유료 결제를 완료한 세션/토큰을 검증해야만 최종 목적지(PDF, 프리미엄 링크 등)로 리다이렉트 처리. 수수료(3~5%) 비즈니스 모델 작동.

---

## 💾 확장형 데이터베이스 설계 (Stage 1 ~ 4 대비)

후속 단계에서 데이터베이스 스키마가 깨지거나 마이그레이션 오류가 발생하지 않도록, 초기 단계부터 확장 가능한 필드를 고려하여 설계합니다.

```mermaid
erDiagram
    User ||--o{ Link : "owns"
    Link ||--o{ RouteRule : "has dynamic routing"
    Link ||--o{ ClickLog : "tracks clicks"
    User ||--o{ Settlement : "has earnings"

    User {
        string id PK
        string email
        string subscriptionTier "FREE | STARTER | PREMIUM | ENTERPRISE"
        datetime subscriptionEndsAt
    }

    Link {
        string id PK
        string userId FK
        string shortCode UNIQUE
        string defaultTargetUrl
        string title "SEO 제목"
        string description "SEO 설명"
        string ogImage "SEO 이미지"
        string fbPixelId
        string gaTrackingId
        string customScript
        boolean isAdEnabled "3단계 광고 활성화 여부"
        int adTimerSeconds "3단계 대기 타이머"
        boolean isPaywalled "4단계 결제형 잠금 여부"
        int price "4단계 결제 금액"
        int clicksCount
        datetime createdAt
    }

    RouteRule {
        string id PK
        string linkId FK
        string ruleType "OS | DEVICE | TIME | COUNTRY"
        string ruleValue "iOS | Android | Desktop"
        string targetUrl "조건 매치시 이동할 URL"
    }

    ClickLog {
        string id PK
        string linkId FK
        string userAgent
        string ipHash
        string referrer
        string deviceType
        string osType
        boolean isAdClicked "3단계 광고 클릭 여부"
        boolean isConverted "4단계 결제 완료 여부"
        datetime timestamp
    }

    Settlement {
        string id PK
        string userId FK
        int amount "정산 금액"
        string status "PENDING | COMPLETED"
        datetime settledAt
    }
```

---

## 🛠️ 1단계 상용화 구현 상세 계획 (Spring Boot 기반)

### 1. 기술 스택 요약
* **Framework**: Spring Boot 3.x (Java 17 또는 21)
* **Template Engine**: Thymeleaf (초고속 SSR 및 SEO 최적화)
* **ORM/Mapper**: MyBatis + SQL Mapper XML
* **Database**: 
  * 로컬 개발 환경: SQLite (파일 데이터베이스)
  * 상용 배포 환경: PostgreSQL (대용량 및 JSONB 최적화)
* **Build Tool**: Gradle (Kotlin 또는 Groovy DSL)

### 2. 프로젝트 디렉토리 구조
```
h:\lee\pixel-link\
├── docs/
│   └── implementation_plan.md
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── pixellink/
        │           ├── PixelLinkApplication.java
        │           ├── config/
        │           │   ├── DatabaseConfig.java
        │           │   └── WebConfig.java
        │           ├── controller/
        │           │   ├── DashboardController.java (대시보드 페이지 뷰)
        │           │   ├── LinkApiController.java (CRUD REST API)
        │           │   └── RedirectionController.java (/[code] 리다이렉트 핸들러)
        │           ├── dto/
        │           │   ├── LinkCreateRequest.java
        │           │   └── LinkResponse.java
        │           ├── mapper/
        │           │   ├── LinkMapper.java
        │           │   └── ClickLogMapper.java
        │           ├── model/
        │           │   ├── Link.java
        │           │   ├── ClickLog.java
        │           │   └── User.java
        │           └── service/
        │               ├── LinkService.java
        │               └── RedirectionService.java
        └── resources/
            ├── application.yml (공통 설정 및 MyBatis 설정)
            ├── application-local.yml (SQLite 데이터소스 및 DDL 경로 지정)
            ├── application-prod.yml (PostgreSQL 데이터소스 지정)
            ├── db/
            │   ├── schema-sqlite.sql (SQLite DDL)
            │   └── schema-postgresql.sql (PostgreSQL DDL)
            ├── mapper/
            │   ├── LinkMapper.xml
            │   └── ClickLogMapper.xml
            ├── static/
            │   ├── css/
            │   │   └── global.css (프리미엄 다크테마 스타일링)
            │   └── js/
            │       └── main.js
            └── templates/
                ├── dashboard.html (관리자 대시보드 화면)
                └── redirect.html (초고속 마케팅 픽셀 실행 및 대기 화면)
```

---

## 💻 핵심 컴포넌트 설계

### 1) DB 스키마 분리 초기화 (`resources/db/`)
로컬 실행 시 SQLite용 DDL이 구동되도록 스프링 데이터베이스 초기화(Spring SQL Init)를 연동합니다.

#### [NEW] [schema-sqlite.sql](file:///h:/lee/pixel-link/src/main/resources/db/schema-sqlite.sql)
```sql
CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    email TEXT NOT NULL,
    subscription_tier TEXT DEFAULT 'FREE',
    subscription_ends_at TEXT
);

CREATE TABLE IF NOT EXISTS links (
    id TEXT PRIMARY KEY,
    user_id TEXT,
    short_code TEXT UNIQUE NOT NULL,
    default_target_url TEXT NOT NULL,
    title TEXT,
    description TEXT,
    og_image TEXT,
    fb_pixel_id TEXT,
    ga_tracking_id TEXT,
    custom_script TEXT,
    is_ad_enabled INTEGER DEFAULT 0,
    ad_timer_seconds INTEGER DEFAULT 1,
    is_paywalled INTEGER DEFAULT 0,
    price INTEGER DEFAULT 0,
    clicks_count INTEGER DEFAULT 0,
    created_at TEXT DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY(user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS click_logs (
    id TEXT PRIMARY KEY,
    link_id TEXT,
    user_agent TEXT,
    ip_hash TEXT,
    referrer TEXT,
    device_type TEXT,
    os_type TEXT,
    is_ad_clicked INTEGER DEFAULT 0,
    is_converted INTEGER DEFAULT 0,
    timestamp TEXT DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY(link_id) REFERENCES links(id)
);
```

#### [NEW] [schema-postgresql.sql](file:///h:/lee/pixel-link/src/main/resources/db/schema-postgresql.sql)
```sql
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(50) PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    subscription_tier VARCHAR(50) DEFAULT 'FREE',
    subscription_ends_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS links (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) REFERENCES users(id),
    short_code VARCHAR(100) UNIQUE NOT NULL,
    default_target_url TEXT NOT NULL,
    title VARCHAR(255),
    description TEXT,
    og_image TEXT,
    fb_pixel_id VARCHAR(50),
    ga_tracking_id VARCHAR(50),
    custom_script TEXT,
    is_ad_enabled BOOLEAN DEFAULT FALSE,
    ad_timer_seconds INTEGER DEFAULT 1,
    is_paywalled BOOLEAN DEFAULT FALSE,
    price INTEGER DEFAULT 0,
    clicks_count INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS click_logs (
    id VARCHAR(50) PRIMARY KEY,
    link_id VARCHAR(50) REFERENCES links(id),
    user_agent TEXT,
    ip_hash VARCHAR(64),
    referrer TEXT,
    device_type VARCHAR(50),
    os_type VARCHAR(50),
    is_ad_clicked BOOLEAN DEFAULT FALSE,
    is_converted BOOLEAN DEFAULT FALSE,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

### 2) 데이터소스 설정 (`resources/application.yml` 외)

#### [NEW] [application.yml](file:///h:/lee/pixel-link/src/main/resources/application.yml)
```yaml
spring:
  profiles:
    active: local # 기본값 로컬 개발 환경
  sql:
    init:
      mode: always

mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.pixellink.model
  configuration:
    map-underscore-to-camel-case: true
```

#### [NEW] [application-local.yml](file:///h:/lee/pixel-link/src/main/resources/application-local.yml)
```yaml
spring:
  datasource:
    driver-class-name: org.sqlite.JDBC
    url: jdbc:sqlite:pixel-link-local.db
  sql:
    init:
      schema-locations: classpath:db/schema-sqlite.sql
```

#### [NEW] [application-prod.yml](file:///h:/lee/pixel-link/src/main/resources/application-prod.yml)
```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: ${PROD_DB_URL}
    username: ${PROD_DB_USERNAME}
    password: ${PROD_DB_PASSWORD}
  sql:
    init:
      schema-locations: classpath:db/schema-postgresql.sql
```

---

### 3) 초고속 리다이렉션 화면 설계 (`templates/redirect.html`)
Thymeleaf로 작성되는 중간 연결 페이지는 CSS와 폰트를 인라인에 가깝게 최소화하여 극대화된 첫 로딩 성능을 냅니다.

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:text="${link.title}">연결 중...</title>
    <meta name="description" th:content="${link.description}">
    <meta property="og:title" th:content="${link.title}">
    <meta property="og:description" th:content="${link.description}">
    <meta property="og:image" th:content="${link.ogImage}">
    
    <!-- 1. Facebook Pixel Code 삽입 -->
    <script th:if="${link.fbPixelId != null && !#strings.isEmpty(link.fbPixelId)}" th:inline="javascript">
        !function(f,b,e,v,n,t,s)
        {if(f.fbq)return;n=f.fbq=function(){n.callMethod?
        n.callMethod.apply(n,arguments):n.queue.push(arguments)};
        if(!f._fbq)f._fbq=n;n.push=n;n.loaded=!0;n.version='2.0';
        n.queue=[];t=b.createElement(e);t.async=!0;
        t.src=v;s=b.getElementsByTagName(e)[0];
        s.parentNode.insertBefore(t,s)}(window, document,'script',
        'https://connect.facebook.net/en_US/fbevents.js');
        fbq('init', /*[[${link.fbPixelId}]]*/ '');
        fbq('track', 'PageView');
    </script>
    
    <!-- 2. Google Analytics Tag (gtag.js) 삽입 -->
    <script th:if="${link.gaTrackingId != null && !#strings.isEmpty(link.gaTrackingId)}" th:src="@{'https://www.googletagmanager.com/gtag/js?id=' + ${link.gaTrackingId}}" async></script>
    <script th:if="${link.gaTrackingId != null && !#strings.isEmpty(link.gaTrackingId)}" th:inline="javascript">
        window.dataLayer = window.dataLayer || [];
        function gtag(){dataLayer.push(arguments);}
        gtag('js', new Date());
        gtag('config', /*[[${link.gaTrackingId}]]*/ '');
    </script>

    <!-- 3. Custom Script 삽입 -->
    <script th:if="${link.customScript != null && !#strings.isEmpty(link.customScript)}" th:utext="${link.customScript}"></script>

    <style>
        body {
            background-color: #0b0f19;
            color: #f3f4f6;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            display: flex;
            align-items: center;
            justify-content: center;
            height: 100vh;
            margin: 0;
            overflow: hidden;
        }
        .container {
            text-align: center;
        }
        .spinner {
            width: 50px;
            height: 50px;
            border: 3px solid rgba(255, 255, 255, 0.1);
            border-radius: 50%;
            border-top-color: #3b82f6;
            animation: spin 1s ease-in-out infinite;
            margin: 0 auto 20px auto;
        }
        @keyframes spin {
            to { transform: rotate(360deg); }
        }
        h1 {
            font-size: 1.25rem;
            font-weight: 500;
            color: #9ca3af;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="spinner"></div>
        <h1>안전한 목적지로 연결 중입니다...</h1>
    </div>

    <script th:inline="javascript">
        // 픽셀 스크립트가 실행될 최소 대기 시간을 확보한 후 리다이렉트 실행
        const targetUrl = /*[[${link.defaultTargetUrl}]]*/ 'https://example.com';
        const delay = /*[[${link.adTimerSeconds * 1000}]]*/ 1000;
        
        setTimeout(() => {
            window.location.href = targetUrl;
        }, delay);
    </script>
</body>
</html>
```

---

## 🔍 검증 및 모니터링 계획

1. **Gradle 빌드 및 컴파일 검증**:
   * `./gradlew build` 명령을 실행하여 의존성 충돌과 컴파일 오류 유무 검증.
2. **로컬 SQLite 구동 테스트**:
   * `local` 프로필로 앱 실행 후 로컬 폴더에 `pixel-link-local.db`가 정상 생성되고, `schema-sqlite.sql` 스키마가 정상 이식되는지 검증.
3. **픽셀 로드 및 리다이렉션 검증**:
   * 등록된 코드로 접속 시 메타 데이터가 올바르게 헤더에 로드되는지 Curl / DevTools 검증.
   * 로딩 화면이 연출된 후 지정된 지연시간 뒤 목적지 URL로 리다이렉트되는지 브라우저에서 직접 테스트.
