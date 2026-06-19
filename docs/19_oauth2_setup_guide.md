# 🔑 OAuth2 소셜 로그인 통합 연동 및 자격증명 발급 가이드

본 문서는 Pixel-Link 서비스에 연동되어 있는 **4대 소셜 로그인 제공자(Google, Facebook, Naver, Kakao)**의 실제 API 자격증명(Client ID 및 Client Secret)을 개발자 센터에서 정식 발급받고, 서비스 환경에 탑재하는 방법을 정리한 운영 매뉴얼입니다. 후임 개발자 및 인프라 운영자는 본 가이드라인에 따라 설정해 주시기 바랍니다.

---

## 🛠️ 공통 연동 아키텍처 및 리다이렉트 명세

소셜 로그인 완료 후 사용자 인증 코드를 돌려받을 **승인된 리다이렉트 URI (Authorized Redirect URI)**는 Spring Security OAuth2 Client 규격에 맞추어 각각 다음과 같이 설정되어 있습니다.

| 소셜 공급자 | 로컬 개발 환경 Redirect URI (Port: 8090) | 상용 운영 환경 Redirect URI (HTTPS 필수) |
| :--- | :--- | :--- |
| **Google** | `http://localhost:8090/login/oauth2/code/google` | `https://[Domain]/login/oauth2/code/google` |
| **Facebook** | `http://localhost:8090/login/oauth2/code/facebook` | `https://[Domain]/login/oauth2/code/facebook` |
| **Naver** | `http://localhost:8090/login/oauth2/code/naver` | `https://[Domain]/login/oauth2/code/naver` |
| **Kakao** | `http://localhost:8090/login/oauth2/code/kakao` | `https://[Domain]/login/oauth2/code/kakao` |

---

## 1. 🌐 Google OAuth2 설정 가이드

1. **Google Cloud Console 접속**:
   * [Google Cloud Console](https://console.cloud.google.com/)에 개발용 대표 계정으로 로그인합니다.
2. **프로젝트 생성**:
   * 상단 프로젝트 선택 메뉴 ➔ **[새 프로젝트]** 생성 (예: `Pixel-Link`)
3. **OAuth 동의 화면 구성**:
   * 왼쪽 메뉴 ➔ **[API 및 서비스] ➔ [OAuth 동의 화면]** 클릭.
   * User Type을 **[외부 (External)]**로 선택 후 생성.
   * 앱 이름, 사용자 지원 이메일 등 필수 정보를 작성하고 저장합니다.
4. **OAuth 클라이언트 ID 발급**:
   * 왼쪽 메뉴 ➔ **[사용자 인증 정보] ➔ [사용자 인증 정보 만들기] ➔ [OAuth 클라이언트 ID]** 클릭.
   * **애플리케이션 유형**: `웹 애플리케이션` 선택.
   * **승인된 리디렉션 URI**:
     * `http://localhost:8090/login/oauth2/code/google` 등록.
5. **자격증명 메모**:
   * 발급 완료 창에 노출되는 `클라이언트 ID` 및 `클라이언트 보안 비밀번호`를 안전한 비밀 보관소에 복사합니다.

---

## 2. 🔵 Facebook (Meta) OAuth2 설정 가이드

1. **Meta for Developers 접속**:
   * [Meta for Developers Portal](https://developers.facebook.com/)에 개발 계정으로 로그인합니다.
2. **앱 생성**:
   * **[내 앱] ➔ [앱 만들기]** 클릭.
   * 앱 유형으로 **[소셜 로그인 연동 / 소비자]** 관련 옵션을 선택하여 생성합니다.
3. **Facebook 로그인 제품 추가**:
   * 대시보드 화면 ➔ **[제품 추가] ➔ [Facebook 로그인] ➔ [설정]** 클릭.
   * 플랫폼으로 **[웹 (Web)]** 선택 후, 사이트 URL에 `http://localhost:8090/` 입력.
4. **리디렉션 URI 등록**:
   * 왼쪽 메뉴 ➔ **[Facebook 로그인] ➔ [설정]** 클릭.
   * **클라이언트 OAuth 설정** 섹션 ➔ **[승인된 OAuth 리디렉션 URI]**에 다음 주소 추가:
     * `http://localhost:8090/login/oauth2/code/facebook`
5. **자격증명 확인**:
   * 왼쪽 메뉴 ➔ **[설정] ➔ [기본 설정]** 클릭.
   * **[앱 ID]** (Client ID 역할) 및 **[앱 시크릿 코드]** (Client Secret 역할)를 획득합니다.

---

## 3. 🟢 Naver OAuth2 설정 가이드 (Custom Provider)

네이버는 국내 전용 규격이므로 `application.yml` 파일 내에 커스텀 API 엔드포인트 명세가 수동 기재되어 있습니다.

1. **Naver Developers 접속**:
   * [Naver Developers](https://developers.naver.com/) 포털에 접속합니다.
2. **애플리케이션 등록**:
   * 상단 메뉴 **[Application] ➔ [애플리케이션 등록]** 클릭.
   * **애플리케이션 이름**: `Pixel-Link` 작성.
   * **사용 API**: `네이버 로그인` 선택 (필수 제공 항목으로 `이메일 주소`, `별명`, `프로필 사진` 선택).
3. **환경 설정 및 URI 등록**:
   * 서비스 환경 선택 ➔ **[PC 웹]** 추가.
   * **서비스 URL**: `http://localhost:8090/`
   * **네이버 로그인 Callback URL**:
     * `http://localhost:8090/login/oauth2/code/naver`
4. **자격증명 확인**:
   * **[내 애플리케이션] ➔ [개요]** 탭에서 `Client ID` 및 `Client Secret` 값을 메모합니다.

---

## 🟡 4. Kakao OAuth2 설정 가이드 (Custom Provider)

카카오 역시 국내 전용 규격이므로 커스텀 API 명세가 `application.yml`에 수동 등록되어 있습니다.

1. **Kakao Developers 접속**:
   * [Kakao Developers](https://developers.kakao.com/)에 로그인합니다.
2. **애플리케이션 추가**:
   * **[내 애플리케이션] ➔ [애플리케이션 추가하기]** 클릭 (앱 이름, 사업자명 기재).
3. **카카오 로그인 기능 활성화**:
   * 왼쪽 메뉴 **[제품 설정] ➔ [카카오 로그인]** 클릭 ➔ 상태를 **[ON]**으로 변경.
   * **Redirect URI 등록**:
     * `http://localhost:8090/login/oauth2/code/kakao`
4. **동의 항목 설정**:
   * 왼쪽 메뉴 **[사용자 동의항목]** ➔ **[프로필 정보(닉네임/프로필 사진)]** 및 **[카카오계정(이메일)]**을 필수 또는 선택 동의로 설정합니다.
5. **자격증명 및 시크릿 키 확인**:
   * 왼쪽 메뉴 **[앱 키]** ➔ **[REST API 키]** (이 값이 `client-id` 가 됩니다).
   * 왼쪽 메뉴 **[보안]** ➔ Client Secret 섹션에서 **[코드 발급]** 클릭하여 활성화한 뒤 값을 획득합니다 (이 값이 `client-secret` 이 됩니다).

---

## 🔒 5. 상용 환경 배포 및 구동 방법 (보안 권장사항)

**🚨 절대 소스코드(`application.yml`) 내부에 Client Secret 비밀키를 텍스트 형태로 하드코딩하여 Git에 커밋하지 마십시오.** 유출 시 해킹 피해를 입을 수 있습니다.

본 프로젝트의 OAuth2 설정은 OS 환경 변수가 주입되면 해당 값으로 우선 오버라이딩되도록 설계되어 있습니다. 배포 시 하단 명령어 또는 컨테이너 설정에 따라 환경 변수를 주입하십시오.

### 5.1. 로컬 환경 수동 기동 (PowerShell)
```powershell
# 1. 4대 소셜 로그인 환경 변수 세팅
$env:SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID="구글_ID"
$env:SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET="구글_SECRET"

$env:SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_FACEBOOK_CLIENT_ID="페이스북_ID"
$env:SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_FACEBOOK_CLIENT_SECRET="페이스북_SECRET"

$env:SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_NAVER_CLIENT_ID="네이버_ID"
$env:SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_NAVER_CLIENT_SECRET="네이버_SECRET"

$env:SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KAKAO_CLIENT_ID="카카오_REST_API키"
$env:SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KAKAO_CLIENT_SECRET="카카오_보안코드"

# 2. 서버 실행
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

### 5.2. Docker / Linux 상용 서버 기동 (Shell)
```bash
export SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID="구글_ID"
export SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET="구글_SECRET"
# ... 타 소셜 키 동일 export 지정 ...

nohup java -jar -Dspring.profiles.active=prod pixel-link-0.0.1-SNAPSHOT.jar > /dev/null 2>&1 &
```
