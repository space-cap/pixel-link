# Pixel-Link 환경 구성 및 실행 가이드

본 문서는 Pixel-Link 프로젝트의 로컬 개발 환경 구성 방법과 상용 서버(Production)로 빌드 및 배포하는 방법을 기술합니다.

---

## 1. 사전 요구 사양 (Prerequisites)

* **Java SDK**: Java 17 또는 21 (추천: Azul Zulu JDK 17 LTS)
* **Build Tool**: Gradle 8.x (프로젝트 래퍼 `./gradlew` 포함)
* **Local Database**: SQLite 3 (별도 설치 불필요, JDBC 드라이버 내장형 파일 DB로 가동)
* **Production Database**: PostgreSQL 14 이상

---

## 2. 프로젝트 초기 셋업 및 로컬 실행

### 2.1. 로컬 환경 설정 파일
로컬 개발 시에는 SQLite 데이터베이스를 사용하여 파일을 자동으로 생성하고 스키마 초기화 쿼리를 매 실행 시마다 실행합니다.
* **적용 프로필**: `local`
* **설정 파일**: `src/main/resources/application-local.yml`

### 2.2. 로컬 실행 방법 (명령어)

프로젝트 루트 폴더로 이동한 뒤, 아래 명령어를 실행하여 로컬 스프링 부트 서버를 가동합니다.

#### Windows (PowerShell)
```powershell
./gradlew bootRun --args='--spring.profiles.active=local'
```

#### Linux / macOS
```bash
chmod +x gradlew
./gradlew bootRun --args='--spring.profiles.active=local'
```

서버가 가동되면 콘솔에 아래와 같은 로그가 남으며, 로컬 폴더에 `pixel-link-local.db` 파일이 자동 생성됩니다.
```
INFO  com.pixellink.PixelLinkApplication - Starting PixelLinkApplication using Java...
INFO  o.s.b.w.embedded.tomcat.TomcatWebServer - Tomcat initialized with port 8080 (http)
INFO  com.pixellink.PixelLinkApplication - Started PixelLinkApplication in 2.34 seconds
```

포트 `8080`을 통해 다음 주소에 접속할 수 있습니다:
* 대시보드 화면: `http://localhost:8080/`
* API 엔드포인트: `http://localhost:8080/api/links`

---

## 3. 상용 환경(Production) 배포

상용 서버 배포 시에는 빌드하여 실행 가능한 단일 `.jar` 파일을 생성한 뒤, PostgreSQL 접속 정보를 환경변수로 전달하여 구동합니다.

### 3.1. 빌드 패키징 (JAR 생성)
테스트를 수행하고 실행형 아카이브 파일을 빌드합니다.

#### Windows
```powershell
./gradlew clean build
```

#### Linux / macOS
```bash
./gradlew clean build
```

빌드가 성공하면 `build/libs/pixel-link-0.0.1-SNAPSHOT.jar` 파일이 생성됩니다.

### 3.2. 상용 서버 실행 방법
상용 서버 실행 시에는 `prod` 프로필을 활성화하고 PostgreSQL 연결 세부 정보를 시스템 환경 변수로 입력해 주어야 합니다.

#### 실행 명령어 예시
```bash
java -jar build/libs/pixel-link-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --PROD_DB_URL=jdbc:postgresql://your-db-host:5432/pixellink \
  --PROD_DB_USERNAME=your_username \
  --PROD_DB_PASSWORD=your_password
```

또는 서버의 운영체제 환경 변수에 설정한 후 실행합니다.
```bash
export PROD_DB_URL="jdbc:postgresql://your-db-host:5432/pixellink"
export PROD_DB_USERNAME="your_username"
export PROD_DB_PASSWORD="your_password"

java -jar build/libs/pixel-link-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

---

## 4. 데이터베이스 초기 스키마 관리 규칙

* **로컬 개발**: `src/main/resources/db/schema-sqlite.sql` 파일에 테이블 변경 사항을 항상 최신화합니다. 로컬 실행 시 스프링이 매번 구동 시점에 미생성 테이블을 자동 이식합니다.
* **상용 배포**: `src/main/resources/db/schema-postgresql.sql` 파일에 상용 환경에 맞는 DDL 구문을 최신화하고 데이터베이스 인스턴스에 적용합니다. (이후 고도화 단계에서 DB 마이그레이션 도구인 Flyway 도입 예정)
