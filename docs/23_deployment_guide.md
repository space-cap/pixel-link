# Oracle Cloud (Ubuntu 24.04 LTS + Java 21) 실서버 배포 가이드

본 문서는 오라클 클라우드 프리 티어(Oracle Cloud Free Tier) 우분투 24 서버에 **Pixel-Link** 애플리케이션을 빌드하여 배포하고, 무중단 백그라운드 구동 및 Nginx 리버스 프록시 설정을 마치는 실전 배포 가이드라인입니다.

---

## 🗺️ 전체 배포 아키텍처 흐름

```mermaid
graph TD
    User([사용자 브라우저]) -->|HTTPS:443| Nginx[Nginx Web Server]
    Nginx -->|Reverse Proxy:8090| SpringBoot[Spring Boot App (Port 8090)]
    SpringBoot -->|MyBatis/SQL| SQLite[(SQLite 파일 DB)]
```

---

## 🛠️ Step 1. Oracle Cloud Network (수신 포트) 개방

오라클 클라우드는 자체 서브넷 방화벽(Security List)과 우분투 내부 방화벽(`iptables`)이 이중으로 동작하므로 외부 접속을 허용하려면 수신(Ingress) 포트를 반드시 열어주어야 합니다.

### 1) Oracle Cloud Console 설정
1. Oracle Cloud 웹 콘솔 로그인 후 **인스턴스 상세 페이지** 진입.
2. 하단 **기본 VCN(Virtual Cloud Network)** 링크 클릭 -> **보안 목록(Security Lists)** 선택.
3. **수신 규칙 추가(Add Ingress Rules)** 클릭:
   * **소스 CIDR**: `0.0.0.0/0` (모든 IP 허용)
   * **IP 프로토콜**: `TCP`
   * **대상 포트 범위**: `80, 443, 8090` (설정 및 리버스 프록시 서비스용)
   * `수신 규칙 추가` 버튼 클릭하여 저장.

### 2) Ubuntu 내부 방화벽 해제 (중요 ⚠️)
오라클 우분투 이미지는 기본적으로 `iptables` 규칙이 매우 강력하게 막혀 있어, 단순 UFW 개방만으로는 접속이 되지 않는 경우가 많습니다. 우분투 서버에 SSH 터미널 접속 후 다음 명령을 수행합니다:

```bash
# 기본 수신 패킷 차단 규칙을 모두 허용으로 수정
sudo iptables -P INPUT ACCEPT
sudo iptables -P FORWARD ACCEPT
sudo iptables -P OUTPUT ACCEPT

# 현재 iptables 규칙 초기화
sudo iptables -F

# 방화벽 설정 영구 저장 (재부팅 시 유지)
sudo apt-get install -y iptables-persistent
sudo netfilter-persistent save
```

---

## 📦 Step 2. 로컬 빌드 및 서버 파일 전송 (Deploying Jar)

### 1) 로컬 PC에서 실행 가능한 JAR 빌드
작업공간 루트(회사 컴퓨터 터미널)에서 아래 명령을 실행하여 JAR 파일을 빌드합니다:

```bash
# clean 및 빌드 수행
./gradlew clean bootJar
```
* **결과물 위치**: `build/libs/pixel-link-0.0.1-SNAPSHOT.jar`

### 2) SCP 명령어로 서버에 파일 업로드
로컬 터미널에서 아래 예시 명령어(오라클 SSH Key 경로 및 서버 IP 지정)를 실행하여 JAR 파일을 원격 서버의 홈 디렉토리로 안전하게 전송합니다:

```bash
# scp -i [인증키경로] [JAR파일] ubuntu@[오라클서버IP]:/home/ubuntu/
scp -i ~/keys/oracle-free-key.key build/libs/pixel-link-0.0.1-SNAPSHOT.jar ubuntu@123.456.78.90:/home/ubuntu/pixel-link.jar
```

---

## ⚙️ Step 3. Systemd 서비스 등록 (백그라운드 영구 구동)

SSH 세션이 종료되거나 OS가 불시에 재부팅되더라도 애플리케이션이 백그라운드에서 상시 구동 및 자동 재시작이 가능하도록 `systemd` 서비스 유닛을 작성합니다.

### 1) 서비스 파일 생성
```bash
sudo nano /etc/systemd/system/pixellink.service
```

### 2) 아래 내용을 파일에 붙여넣기 (Ctrl + O 저장, Ctrl + X 종료)
```ini
[Unit]
Description=Pixel Link Spring Boot Application
After=network.target

[Service]
User=ubuntu
# 애플리케이션 구동 홈 설정
WorkingDirectory=/home/ubuntu
# UTF-8 문자셋 옵션을 넣어 실행
ExecStart=/usr/bin/java -Dfile.encoding=UTF-8 -jar /home/ubuntu/pixel-link.jar --spring.profiles.active=local
SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### 3) 서비스 활성화 및 구동
```bash
# systemd 데몬 리로드
sudo systemctl daemon-reload

# 부팅 시 자동 시작 등록
sudo systemctl enable pixellink

# 서비스 실행 및 상태 확인
sudo systemctl start pixellink
sudo systemctl status pixellink
```
* **로그 확인 명령어**: `journalctl -u pixellink.service -f`

---

## 🔒 Step 4. Nginx 리버스 프록시 & SSL (HTTPS) 적용 (강력 추천 💡)

8090 포트로 외부에서 직접 접근하게 하기보단, 포트 없이 `https://yourdomain.com` 형태로 접속을 유입받아 내부의 8090 포트로 토스해주는 **Nginx**를 프론트에 띄우는 것이 정석입니다.

### 1) Nginx 설치
```bash
sudo apt update
sudo apt install -y nginx
```

### 2) 도메인 A 레코드 설정
* 가비아, 후이즈 등 도메인 등록처에서 보유하신 도메인의 **A 레코드 IP**로 오라클 클라우드의 **퍼블릭 IP**를 지정합니다.

### 3) Nginx 설정 파일 생성 및 바인딩
```bash
sudo nano /etc/nginx/sites-available/pixellink
```
* **내용 작성**:
```nginx
server {
    listen 80;
    server_name yourdomain.com; # 소유하신 도메인 기재

    location / {
        proxy_pass http://localhost:8090;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 4) 설정 활성화 및 Nginx 재구동
```bash
# 설정 심볼릭 링크 생성
sudo ln -s /etc/nginx/sites-available/pixellink /etc/nginx/sites-enabled/

# 기본 디폴트 설정 삭제 (충돌 예방)
sudo rm /etc/nginx/sites-enabled/default

# 문법 검사 및 재시작
sudo nginx -t
sudo systemctl restart nginx
```

### 5) Certbot을 이용한 Let's Encrypt 무료 SSL 인증서 발급
```bash
sudo apt install -y certbot python3-certbot-nginx

# HTTPS 인증서 발급 및 Nginx 자동 갱신 설정 적용
sudo certbot --nginx -d yourdomain.com
```
* 설정 과정 중 가이드에 따라 HTTP 트래픽을 HTTPS로 리다이렉트 처리하는 옵션(2: Redirect)을 선택합니다.
* 완료되면 브라우저에서 `https://yourdomain.com` 접속 시 자물쇠 마크와 함께 안전하게 접속이 가능합니다!
