# 관리자 시스템 설정(System Settings) 제어 기능 개발 계획

본 계획서는 서비스 운영진(관리자)이 소스코드 수정 없이도 비회원 단축 URL 만료 기간, 클릭당 광고 적립 단가, 최소 출금 가능 금액 및 요금제 가격 설정을 실시간으로 제어할 수 있도록 **시스템 설정 관리 백오피스 화면 및 동적 로직**을 구현하기 위한 상세 계획입니다.

## User Review Required

> [!IMPORTANT]
> **하드코딩 로직의 동적 설정값 전환**
> - **광고 클릭 적립금**: 기존 `LinkService.java` 내 `recordAdClick`에서 `70원`으로 고정되었던 값을 DB의 `ad_reward_per_click` 설정값으로 조회하여 반영합니다.
> - **최소 출금 제한**: `withdrawSettlements`에서 `10,000원` 하드코딩으로 검사하던 부분을 DB의 `min_withdrawal_amount` 설정값을 읽어와 유연하게 검증하도록 수정합니다.
> - **배치 업데이트 API**: 여러 개의 설정값을 한 번에 저장할 수 있도록 Batch Update 컨트롤러 또는 다중 단건 업데이트 처리 흐름을 설계합니다.

---

## Proposed Changes

### [Database & Model Config]
#### [MODIFY] [schema-sqlite.sql](file:///h:/lee/pixel-link/src/main/resources/db/schema-sqlite.sql)
#### [MODIFY] [schema-postgresql.sql](file:///h:/lee/pixel-link/src/main/resources/db/schema-postgresql.sql)
- `system_settings` 테이블에 실시간 조율이 필요한 4가지 신규 글로벌 설정값 삽입 DML 추가:
  - `ad_reward_per_click`: `'70'` (광고 클릭당 적립 단가)
  - `min_withdrawal_amount`: `'10000'` (최소 출금 신청 금액)
  - `starter_monthly_fee`: `'9900'` (스타터 요금제 가격)
  - `premium_monthly_fee`: `'19900'` (프리미엄 요금제 가격)

---

### [Data Access Layer]
#### [MODIFY] [SystemSettingMapper.java](file:///h:/lee/pixel-link/src/main/java/com/pixellink/mapper/SystemSettingMapper.java)
- 기존 단건 업데이트 기능 외에, 전체 설정 목록 조회를 위한 메서드 추가:
  - `List<SystemSetting> findAll();`

#### [MODIFY] [SystemSettingMapper.xml](file:///h:/lee/pixel-link/src/main/resources/mapper/SystemSettingMapper.xml)
- `system_settings` 테이블의 전체 목록을 가져오는 `findAll` SELECT 쿼리 매핑 추가.

---

### [Service Layer]
#### [MODIFY] [LinkService.java](file:///h:/lee/pixel-link/src/main/java/com/pixellink/service/LinkService.java)
- **`recordAdClick` 고도화**:
  - `getSystemSetting("ad_reward_per_click", "70")` 값을 조회(정수 파싱)하여 클릭 적립금 세팅.
- **`withdrawSettlements` 고도화**:
  - `getSystemSetting("min_withdrawal_amount", "10000")` 값을 조회(정수 파싱)하여 최소 출금 신청 제한 조건 검사.
- **설정 일괄 수정 비즈니스 로직**:
  - `public void updateSystemSettings(Map<String, String> settings)` 신설하여 Map 형태로 들어온 설정 키-값을 반복 조회하여 검증 및 일괄 갱신.

---

### [Controllers & API Layer]
#### [MODIFY] [AdminController.java](file:///h:/lee/pixel-link/src/main/java/com/pixellink/controller/AdminController.java)
- 어드민 설정 뷰 맵핑 추가:
  - `@GetMapping("/settings")` -> 전체 시스템 설정 목록을 조회해 `admin/settings` 뷰로 주입.
- 일괄 설정 업데이트 API 추가:
  - `@PostMapping("/settings/update")` -> `@RequestParam` 혹은 `@RequestBody`로 설정값 Map을 받아 `linkService.updateSystemSettings` 호출 후 리다이렉트 또는 JSON 반환.

---

### [View Templates & Assets]
#### [MODIFY] [fragments.html](file:///h:/lee/pixel-link/src/main/resources/templates/fragments.html)
- 관리자용 사이드바 메뉴 중 **시스템 설정** 링크의 비활성화 상태(`opacity: 0.5`, `pointer-events: none`)를 걷어내고 `/app/admin/settings` 경로 매핑.

#### [NEW] [settings.html](file:///h:/lee/pixel-link/src/main/resources/templates/admin/settings.html)
- 럭셔리 다크 글래스모피즘 테마의 시스템 설정 관리 페이지 구현.
- 각 설정 항목별 입력 필드(숫자 형식 제어, 설명 표시) 제공.
- 실시간 정합성 체크(예: 광고 단가는 0원 이상, 최소 출금액은 1,000원 이상 등) 자바스크립트 및 백엔드 예외 처리 탑재.

---

## Verification Plan

### Automated Tests
- `AdminControllerTest.java`에 시스템 설정 통합 테스트 추가:
  - `viewSettings_AsAdmin_ReturnsSettingsPage` (어드민 권한 접속 시 설정 목록이 조회되는지 검증)
  - `updateSettings_AsAdmin_Success` (설정값 일괄 갱신 시 DB에 정상 반영되는지 검증)
  - `updateSettings_InvalidValues_ReturnsError` (설정값이 유효하지 않은 포맷이나 음수일 때 예외 응답 검증)
- `LinkServiceTest.java` (또는 통합 테스트)에 비즈니스 검증 연동 테스트 추가:
  - 변경된 광고 클릭 적립금이 실제 `Settlement` 테이블 저장 시 동적으로 달라지는지 검증.
  - 변경된 최소 출금 한도에 따라 출금 신청 시 예외 처리가 가변적으로 동작하는지 검증.

### Manual Verification
1. `admin` 세션으로 로그인 후 사이드바의 **[시스템 설정]** 메뉴로 정상 접근되는지 확인합니다.
2. 각 입력값(예: 광고 적립금 150원, 최소 출금액 5,000원)을 수정한 뒤 `[설정 저장]` 버튼을 누릅니다.
3. 데이터베이스 혹은 화면 새로고침 시 바뀐 값으로 유지되는지 검증합니다.
4. `test-user` 세션으로 넘어가 광고 리다이렉션 클릭 테스트를 수행하여, `settlements` 테이블에 쌓이는 수익금이 기존 70원이 아닌 150원으로 계산되었는지 확인합니다.
5. 최소 출금액을 5,000원으로 수정한 상태에서 `test-user`가 5,000원 이상 잔액일 때 출금이 성공적으로 통과되는지 확인합니다.
