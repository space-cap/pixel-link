# 21단계: 출금 및 정산 승인 관리자 백오피스 개발 계획서 (Admin Settlement Approval Dashboard & API)

본 계획서는 마케터가 출금을 신청할 때 실제 송금에 필요한 은행 정보(은행명, 계좌번호, 예금주명)를 수집하고, 관리자가 이 내역을 조회하여 출금을 승인(`COMPLETED`) 또는 반려(`REJECTED`)할 수 있는 관리자 정산 승인 백오피스 기능을 구현하기 위한 상세 계획입니다.

---

## 1. 정산 데이터 모델 및 상태 흐름 설계

### 1.1. 출금 신청 시 시나리오
- 유저가 출금을 요청하면, 현재까지의 정산 가능한 모든 금액(최소 10,000원 이상)만큼 **음수 정산 레코드**가 `PENDING` 상태로 생성되며, 수집된 은행 정보가 함께 기록됩니다.
- 기존의 개별 양수 수익금(PENDING)들은 하나의 출금 요청으로 합산 처리됨을 나타내기 위해 즉시 `COMPLETED`로 변경됩니다.

### 1.2. 출금 승인/반려 시나리오
- **승인(Approve) 시**:
  - 음수 정산 레코드의 상태가 `PENDING`에서 `COMPLETED`로 변경되어 확정됩니다. (유저의 정산금 잔액 0원 확정)
- **반려(Reject) 시**:
  - 음수 정산 레코드의 상태가 `REJECTED`로 변경됩니다.
  - 잔액 합산 쿼리(`sumAmountByUserId`)는 `REJECTED` 상태인 정산을 합산에서 제외하도록 개선하여, **반려 즉시 유저의 정산금 잔액이 자동으로 복구**되는 복원력을 확보합니다.

---

## 2. 세부 구현 대상 파일 및 변경 사항

### 2.1. 데이터베이스 스키마 확장 (Database Schema)
- **`src/main/resources/db/schema-sqlite.sql`** 및 **`schema-postgresql.sql`**
  - `settlements` 테이블에 출금용 계좌 정보를 담을 컬럼 3개 추가:
    - `bank_name` TEXT (은행명)
    - `account_number` TEXT (계좌번호)
    - `account_holder` TEXT (예금주명)

### 2.2. 모델 클래스 수정 (Java Model)
- **`src/main/java/com/pixellink/model/Settlement.java`**
  - 새 필드 추가 및 Getter/Setter 작성:
    - `private String bankName;`
    - `private String accountNumber;`
    - `private String accountHolder;`

### 2.3. 데이터 액세스 레이어 수정 (MyBatis Mapper)
- **`src/main/java/com/pixellink/mapper/SettlementMapper.java`**
  - 단건 상태 변경 메서드 선언:
    - `void updateStatus(@Param("id") String id, @Param("status") String status);`
- **`src/main/resources/mapper/SettlementMapper.xml`**
  - `insert` 쿼리에 `bank_name`, `account_number`, `account_holder` 바인딩 추가.
  - `sumAmountByUserId` 조회 쿼리에 `status != 'REJECTED'` 조건 추가 (반려된 정산금 합산 배제).

### 2.4. 비즈니스 서비스 레이어 수정 (Service Layer)
- **`src/main/java/com/pixellink/service/LinkService.java`**
  - `withdrawSettlements` 메서드 시그니처 변경:
    - `public void withdrawSettlements(String userId, String bankName, String accountNumber, String accountHolder)`
  - 출금 비즈니스 로직 고도화:
    1. `sumAmountByUserId(userId)`로 현재 출금 가능 잔액 계산.
    2. 잔액이 10,000원 미만일 경우 예외 발생.
    3. 기존 해당 유저의 모든 `PENDING` 상태 수익 정산건들을 `COMPLETED`로 일괄 업데이트.
    4. 상태가 `PENDING`이고 금액이 `-balance`인 역정산 레코드를 생성하여 계좌 정보와 함께 저장.

### 2.5. 컨트롤러 및 API 레이어 수정 (Controllers & API)
- **`src/main/java/com/pixellink/controller/LinkApiController.java`**
  - `@PostMapping("/withdraw")` API 변경:
    - 요청 바디 또는 파라미터로 `bankName`, `accountNumber`, `accountHolder`를 바인딩받아 `linkService.withdrawSettlements`에 전달.
- **`src/main/java/com/pixellink/controller/AdminController.java`**
  - 어드민 전용 정산 목록 조회 맵핑 추가:
    - `@GetMapping("/settlements")` -> 대기 및 전체 정산 신청 내역을 조회해 `admin/settlements` 뷰 반환.
  - 비동기 승인/반려 API 추가:
    - `@PostMapping("/settlements/{id}/approve")` -> 해당 정산 ID 상태를 `COMPLETED`로 변경 및 성공 JSON 반환.
    - `@PostMapping("/settlements/{id}/reject")` -> 해당 정산 ID 상태를 `REJECTED`로 변경 및 성공 JSON 반환.

### 2.6. 화면 템플릿 구현 및 수정 (View Templates)
- **`src/main/resources/templates/monetization.html`**
  - 출금 신청 버튼 클릭 시 단순 confirm 대신, 미려한 다크 모드 글래스모피즘 입력 팝업(Modal)이 뜨도록 개선.
  - 은행명 선택(주요 은행 드롭다운), 계좌번호 입력, 예금주명 입력을 수집하여 `/api/links/withdraw`로 POST 요청을 보내도록 JS 개편.
- **`src/main/resources/templates/fragments.html`**
  - 관리자용 사이드바 메뉴 중 **정산/출금 승인** 링크의 잠금(`opacity: 0.5`, `pointer-events: none`)을 해제하고 `/app/admin/settlements` 경로 매핑 적용.
- **`src/main/resources/templates/admin/dashboard.html`**
  - '정산 신청 대기 건수' 카드뷰를 클릭하면 정산 관리 페이지(`/app/admin/settlements`)로 바로 이동하도록 클릭 링크 바인딩 (`cursor: pointer` 및 `onclick="location.href=..."` 적용).
- **`src/main/resources/templates/admin/settlements.html` [NEW]**
  - 관리자가 PENDING 상태인 정산 신청 내역을 모니터링하고 `[승인]` 및 `[반려]` 처리할 수 있는 반응형 백오피스 웹 뷰 구현.
  - 모노톤 SVG 아이콘 스타일 및 럭셔리 모노톤 테마 유지.

---

## 3. 검증 계획 (Verification Plan)

### 3.1. 자동화 테스트 (Automated Tests)
- `AdminControllerTest.java`에 다음 통합 테스트 작성 및 전체 빌드 검증:
  - `viewSettlements_AsAdmin_ReturnsView` (관리자 권한으로 정산 승인 페이지 정상 접근 확인)
  - `approveSettlement_AsAdmin_Success` (관리자가 출금 신청을 최종 승인 시 상태가 COMPLETED로 전환되는지 검증)
  - `rejectSettlement_AsAdmin_Success` (관리자가 반려 시 상태가 REJECTED로 바뀌고, 사용자의 잔액이 복원되는지 검증)
  - `viewSettlements_AsRegularUser_Forbidden` (일반 사용자의 접근 차단 403 검증)

### 3.2. 수동 검증 (Manual Verification)
1. `admin` 계정으로 접속하여 사이드바 또는 대시보드 카드뷰를 통해 **정산/출금 승인** 메뉴로 정상 이동하는지 확인합니다.
2. `test-user` 계정으로 접속해 정산 페이지(`/app/monetization`)에서 **출금 신청**을 눌러 은행 정보(예: 신한은행, 110-123-456789, 홍길동)를 기입하여 신청합니다.
3. `admin` 정산 승인 대기 목록에 `test-user`의 신청 정보가 정교하게 노출되는지 확인합니다.
4. `[승인]` 및 `[반려]` 처리를 교차 테스트하고, 사용자의 정산금 잔액 증감이 논리적으로 부합하는지 새로고침하여 확인합니다.
