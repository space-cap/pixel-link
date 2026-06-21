# ☁️ 오라클 클라우드(OCI) 환경에서 SQLite 계속 사용하는 방법 가이드

아직 PostgreSQL을 세팅하지 않으셨고, 오라클 클라우드 VM에 계속 **SQLite**를 사용하여 서비스를 안정적으로 구동시키고 싶으실 때 적용할 수 있는 가장 간편하고 확실한 가이드입니다! 😊

---

## ⚠️ 라이브(OCI VM) 환경에서 SQLite 사용 시 가장 중요한 주의점

**"재배포 시 데이터베이스 파일 휘발(삭제) 방지"**
* 로컬 개발 환경처럼 `jdbc:sqlite:pixel-link-local.db`와 같이 상대 경로로 프로젝트 내부 디렉토리에 파일이 생성되도록 설정해 두면, **GitHub Actions로 새로 빌드된 JAR 파일을 업로드하여 재배포하고 재실행할 때마다 DB 파일이 삭제되거나 덮어씌워져 모든 데이터가 날아갑니다!**
* 따라서, 라이브 서버에서는 **애플리케이션 배포 경로와 완전히 분리된 VM 시스템 내부의 고정 절대 경로**에 SQLite `.db` 파일을 위치시켜야 합니다.

---

## 🛠️ 구체적인 조치 방안 (3가지 방법 중 택 1)

가장 추천해 드리는 방법은 **환경 변수 주입 방식(방법 1)**입니다. 소스 코드를 단 한 줄도 고치지 않고 즉시 적용할 수 있습니다.

### 방법 1. 배포/구동 스크립트에 환경 변수 주입 (가장 권장 ⭐⭐⭐)

GitHub Actions에서 배포한 뒤 OCI VM에서 스프링 부트를 실행하는 스크립트(또는 systemd 등록 서비스)에서 **환경 변수(`SPRING_DATASOURCE_URL`)를 주입**하여 데이터베이스 파일 경로를 지정합니다.

1. VM 서버 내에 DB 전용 폴더를 생성합니다. (예: `/home/ubuntu/pixel-link-db`)
   ```bash
   mkdir -p /home/ubuntu/pixel-link-db
   ```
2. JAR 파일을 백그라운드로 실행할 때 다음과 같이 환경 변수와 함께 구동합니다.
   ```bash
   # OCI VM 내 실행 명령어 예시
   nohup java -jar \
     -Dspring.datasource.url=jdbc:sqlite:/home/ubuntu/pixel-link-db/pixel-link-live.db \
     -Dspring.sql.init.schema-locations=classpath:db/schema-sqlite.sql \
     pixel-link.jar > app.log 2>&1 &
   ```
   *(이렇게 구동하면, JAR 파일을 수백 번 지우고 새로 배포해도 `/home/ubuntu/pixel-link-db/pixel-link-live.db` 경로는 지워지지 않고 보존됩니다!)*

---

### 방법 2. 라이브 전용 프로파일 설정 추가 (`application-prod.yml`)

프로젝트 소스 코드 내에 라이브 서버 전용 SQLite 연결 설정을 추가하는 방법입니다.

1. `src/main/resources/application-prod.yml` 파일을 새로 생성하고 아래 내용을 입력합니다.
   ```yaml
   spring:
     datasource:
       driver-class-name: org.sqlite.JDBC
       # OCI VM 내부의 안전한 절대 경로 지정
       url: jdbc:sqlite:/home/ubuntu/pixel-link-db/pixel-link-live.db
     sql:
       init:
         schema-locations: classpath:db/schema-sqlite.sql
   ```
2. GitHub Actions 배포 후 구동할 때 `prod` 프로파일로 실행하도록 구동 명령어에 프로파일 파라미터를 추가합니다.
   ```bash
   java -jar -Dspring.profiles.active=prod pixel-link.jar
   ```

---

### 방법 3. 깃허브 액션 Workflow 파일(YML)에서 환경변수 제어

GitHub Actions 워크플로우를 통하여 빌드하거나 배포 시점에 환경변수 값을 세팅해서 실행 스크립트로 전달하는 방법입니다.
GitHub Actions `.github/workflows/deploy.yml` 내 구동 스크립트 실행 태스크에 환경 변수를 주입합니다.

```yaml
- name: Start Spring Boot Application
  run: |
    ssh -i private_key.pem ubuntu@pl3.kr "nohup java -jar -Dspring.datasource.url=jdbc:sqlite:/home/ubuntu/pixel-link-db/pixel-link-live.db pixel-link.jar > /dev/null 2>&1 &"
```

---

## 💡 요약 및 추천 로드맵

1. VM 서버에 접속하여 안전하게 보존할 DB 폴더를 만듭니다 (`mkdir -p /home/ubuntu/pixel-link-db`).
2. 배포 스크립트 실행 명령어에 `-Dspring.datasource.url=jdbc:sqlite:/home/ubuntu/pixel-link-db/pixel-link-live.db` 옵션을 붙여 줍니다.
3. 이 방식으로 구동하면 라이브 서버에서도 에러 없이 안전하게 SQLite 기반으로 서비스를 정상 론칭할 수 있습니다!
