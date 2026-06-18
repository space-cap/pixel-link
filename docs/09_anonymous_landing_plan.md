# 09_anonymous_landing_plan.md - 비회원 단축 기능 및 마케팅 랜딩 페이지 구현 계획

비회원이 로그인 없이 1초 만에 단축 URL을 만들 수 있는 체험 기능을 제공하고, 이를 통해 자연스럽게 회원가입(무료/유료)으로 유도하는 고품격 랜딩 페이지 및 관리자 설정 기능을 개발합니다.

## 1. 아키텍처 및 요구사항 요약

### 1) 비회원 단축 URL 체험
- 비회원은 로그인이나 가입 없이 랜딩 페이지 상단에서 대상 URL을 입력해 단축 링크를 1초 만에 발급받을 수 있습니다.
- 단, 비회원 링크는 픽셀, 타이틀, 커스텀 슬러그 등록이 제한되며 오직 랜덤 6자리 코드로만 생성됩니다.
- 비회원 링크는 데이터베이스 리소스 절약을 위해 만료 기간(디폴트 30일)이 지정되며, 만료 이후에는 자동으로 파기됩니다.

### 2) 관리자 설정 (만료일 조정)
- 시스템 설정 테이블(`system_settings`)을 신설하여, 비회원 링크의 만료 기간(일)을 데이터베이스 기반으로 저장합니다.
- 관리자(`admin`) 요금제 사용자는 대시보드 UI를 통해 만료일(예: 30일 ➡️ 14일 등)을 실시간으로 수정할 수 있습니다.
- 매일 자정 스프링 스케줄러가 만료일이 지난 링크를 조회하여 일괄 삭제 처리합니다.

### 3) 마케팅 랜딩 페이지 (`landing.html`)
- 사이트의 루트 경로 `/`는 비회원용 고품격 마케팅 랜딩 페이지로 전환됩니다. 기존 회원 대시보드는 `/dashboard` 경로로 분리됩니다.
- 단축 완료 시 화면 전환 없이 복사 기능이 제공되며, 그 즉시 **"가상의 클릭 통계 대시보드 차트"**가 애니메이션으로 움직여 마케터에게 가입 시 제공될 혜택을 시각적으로 어필합니다.
- 하단부에는 등급별 요금제 카드와 혜택 안내, 강력한 가입 유도 CTA 버튼을 배치합니다.

---

## 2. 데이터베이스 스키마 설계

### 1) 시스템 설정 테이블 (`system_settings`)
| 컬럼명 | 타입 | 제약조건 | 설명 |
| :--- | :--- | :--- | :--- |
| `setting_key` | VARCHAR(50) | PRIMARY KEY | 설정 키 (예: `anon_link_expiry_days`) |
| `setting_value` | VARCHAR(255) | NOT NULL | 설정 값 (예: `30`) |
| `description` | VARCHAR(255) | | 설명 |

* **초기 데이터 삽입 SQL**:
  ```sql
  INSERT INTO system_settings (setting_key, setting_value, description) 
  VALUES ('anon_link_expiry_days', '30', '비회원 단축 링크 만료 기간 (일)');
  ```

### 2) 단축 링크 테이블 (`links`) 추가 컬럼
* `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
* `expired_at` TIMESTAMP (비회원용 만료 기한, 회원 생성 건은 `NULL`로 설정)

---

## 3. 백엔드 구성 요소 및 API 스펙

### 1) 새로운 엔티티 및 Mapper
* **`SystemSetting.java`** (Model)
* **`SystemSettingMapper.java` / `SystemSettingMapper.xml`**
  * `findByKey(String key)`: 설정 조회
  * `updateValue(String key, String value)`: 설정 수정

### 2) 단축 링크 생성 로직 변경 (`LinkService.java`)
```java
// pseudo-code
public LinkResponse createLink(LinkCreateRequest request, String userId, String baseUrl) {
    if (userId == null || userId.trim().isEmpty() || "anonymous".equals(userId)) {
        // 비회원 생성 시나리오
        String expireDaysStr = systemSettingMapper.findByKey("anon_link_expiry_days").getValue();
        int days = Integer.parseInt(expireDaysStr);
        
        Link link = new Link();
        link.setId(UUID.randomUUID().toString());
        link.setUserId("anonymous");
        link.setShortCode(generateUniqueShortCode());
        link.setDefaultTargetUrl(request.getDefaultTargetUrl());
        link.setExpiredAt(calculateExpiryDate(days));
        
        linkMapper.insert(link);
        return LinkResponse.from(link, baseUrl);
    } else {
        // 기존 회원 생성 시나리오 (expiredAt = null)
        ...
    }
}
```

### 3) 링크 만료 자동 청소 스케줄러 (`LinkCleanupScheduler.java`)
스프링의 `@Scheduled` 기능을 활성화하여 주기적으로 파기 작업을 수행합니다.
```java
@Component
public class LinkCleanupScheduler {
    @Autowired
    private LinkMapper linkMapper;

    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정
    @Transactional
    public void cleanupExpiredLinks() {
        int deletedCount = linkMapper.deleteExpiredLinks();
        log.info("만료된 비회원 단축 링크 {}개를 정리했습니다.", deletedCount);
    }
}
```

### 4) API 스펙 변경 및 추가
* **[MODIFY] POST `/api/links`**:
  * `userId` 파라미터를 Optional로 변경 (누락 시 비회원 처리)
* **[NEW] POST `/api/admin/settings`**:
  * 관리자 전용 설정 변경 엔드포인트
  * Request Body: `{"key": "anon_link_expiry_days", "value": "14"}`

---

## 4. 프론트엔드 UI/UX 설계

### 1) 랜딩 페이지 (`landing.html`)
* **디자인**: 화이트 백그라운드에 딥 블루 포인트 컬러 조합의 신뢰감을 주는 모던한 레이아웃.
* **1초 단축 체험 섹션**: 
  * 랜딩 첫 화면에 목적지 URL 입력창과 `[단축하기]` 버튼만 직관적으로 제공.
  * 단축 완료 시 카드 UI가 부드러운 트랜지션으로 튀어나오며 `pixel-link.com/A7f3x` 복사 버튼 제공.
* **가상 대시보드 애니메이션**:
  * 단축이 완료되면 그 아래에 **"실시간 통계 기능 미리보기"** 박스가 슬라이드 업 형태로 나타남.
  * 가상의 방문 수 그래프가 차트 애니메이션으로 실시간 상승하는 효과 연출.
  * "방금 만든 링크의 클릭을 실시간으로 추적하고 픽셀을 심고 싶다면? 3초 만에 무료 회원가입하기" CTA 버튼 노출.

### 2) 관리자 설정 UI (`dashboard.html`)
* 관리자(`admin`) 계정으로 전환해 로그인했을 때, 대시보드 우측 상단이나 관리 영역에 "비회원 설정" 카드 추가.
* 설정값 입력 폼과 `[변경하기]` 버튼을 배치하여 실시간으로 DB 값을 업데이트할 수 있도록 구현.

---

## 5. 검증 계획

### 1) 자동화 테스트 (`test_flow.ps1` 확장)
* 비회원 링크 생성 및 리다이렉트가 정상적으로 구동되는지 API 호출 테스트 수행.
* 관리자 권한 API 호출을 통한 만료 설정 변경 후 DB 반영 여부 확인.

### 2) 수동 검증
* 시크릿 브라우저에서 `/` 접속 시 고품격 마케팅 랜딩 페이지 로드 여부 검증.
* 비회원으로 1초 단축 기능을 실행하여 복사 카드 및 맛보기 대시보드가 정상 렌더링되는지 UX 체크.
* 관리자 전환을 실행해 대시보드에서 만료일 변경을 적용하고 설정이 실시간 반영되는지 체크.
