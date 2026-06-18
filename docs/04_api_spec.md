# Pixel-Link REST API 규격서

본 문서는 Pixel-Link 서비스의 백엔드와 대시보드 화면 간 통신 및 외부 통합을 위한 REST API 규격서입니다. 모든 API 통신은 JSON 형식으로 이루어지며, 상태 코드 및 에러 규격을 표준화합니다.

---

## 1. 공통 규격

### 1.1. 기본 URL (Base URL)
* 로컬 환경: `http://localhost:8080/api`
* 상용 환경: `https://api.pixel-link.com/api`

### 1.2. 공통 응답 포맷 (Common Response Format)

#### 성공 응답 (Success)
HTTP 상태 코드 `200 OK` 또는 `201 Created`와 함께 아래 바디가 전달됩니다.
```json
{
  "success": true,
  "data": { ... } // 응답 데이터 본문
}
```

#### 실패 응답 (Error)
HTTP 상태 코드 `4xx` 또는 `5xx`와 함께 에러 원인이 전달됩니다.
```json
{
  "success": false,
  "error": {
    "code": "INVALID_INPUT", // 에러 코드
    "message": "목적지 URL 형식이 올바르지 않습니다." // 에러 메시지
  }
}
```

---

## 2. API 상세 목록

### 2.1. 단축 링크 생성 (`POST /links`)
새로운 단축 링크를 등록합니다.

* **요청 헤더**: `Content-Type: application/json`
* **요청 바디 (Request Body)**:
```json
{
  "defaultTargetUrl": "https://example.com/products/123",
  "shortCode": "my-promo", // 선택 사항 (미지정 시 6자리 랜덤 생성)
  "title": "여름 신상품 특별 할인", // 선택 사항 (SEO 프리뷰용)
  "description": "올 여름을 사로잡을 특별 세일! 지금 바로 구경하세요.", // 선택 사항
  "ogImage": "https://example.com/images/promo.jpg", // 선택 사항
  "fbPixelId": "1234567890", // 선택 사항 (페이스북 픽셀 ID)
  "gaTrackingId": "G-ABC123XYZ", // 선택 사항 (Google Analytics ID)
  "customScript": "<script>console.log('pixel loaded');</script>", // 선택 사항
  "adTimerSeconds": 1 // 선택 사항 (기본값 1초)
}
```

* **응답 바디 (Response Body)**:
  * HTTP Status: `201 Created`
```json
{
  "success": true,
  "data": {
    "id": "link_uuid_12345",
    "shortCode": "my-promo",
    "shortenedUrl": "https://pixel-link.com/my-promo",
    "defaultTargetUrl": "https://example.com/products/123",
    "title": "여름 신상품 특별 할인",
    "description": "올 여름을 사로잡을 특별 세일! 지금 바로 구경하세요.",
    "ogImage": "https://example.com/images/promo.jpg",
    "fbPixelId": "1234567890",
    "gaTrackingId": "G-ABC123XYZ",
    "customScript": "<script>console.log('pixel loaded');</script>",
    "isAdEnabled": false,
    "adTimerSeconds": 1,
    "isPaywalled": false,
    "price": 0,
    "clicksCount": 0,
    "createdAt": "2026-06-18T18:30:00+09:00"
  }
}
```

---

### 2.2. 단축 링크 전체 목록 조회 (`GET /links`)
현재 로그인한 사용자가 생성한 전체 단축 링크 리스트를 조회합니다.

* **요청 파라미터**: 없음 (차후 페이지네이션 `page`, `size` 추가 예정)
* **응답 바디 (Response Body)**:
  * HTTP Status: `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": "link_uuid_12345",
      "shortCode": "my-promo",
      "shortenedUrl": "https://pixel-link.com/my-promo",
      "defaultTargetUrl": "https://example.com/products/123",
      "clicksCount": 420,
      "createdAt": "2026-06-18T18:30:00+09:00"
    },
    {
      "id": "link_uuid_67890",
      "shortCode": "custom-sale",
      "shortenedUrl": "https://pixel-link.com/custom-sale",
      "defaultTargetUrl": "https://anothershop.com",
      "clicksCount": 12,
      "createdAt": "2026-06-17T15:20:00+09:00"
    }
  ]
}
```

---

### 2.3. 단축 링크 단일 상세 조회 (`GET /links/{id}`)
특정 단축 링크의 상세 설정 정보를 조회합니다.

* **응답 바디 (Response Body)**:
  * HTTP Status: `200 OK`
```json
{
  "success": true,
  "data": {
    "id": "link_uuid_12345",
    "shortCode": "my-promo",
    "shortenedUrl": "https://pixel-link.com/my-promo",
    "defaultTargetUrl": "https://example.com/products/123",
    "title": "여름 신상품 특별 할인",
    "description": "올 여름을 사로잡을 특별 세일! 지금 바로 구경하세요.",
    "ogImage": "https://example.com/images/promo.jpg",
    "fbPixelId": "1234567890",
    "gaTrackingId": "G-ABC123XYZ",
    "customScript": "<script>console.log('pixel loaded');</script>",
    "isAdEnabled": false,
    "adTimerSeconds": 1,
    "isPaywalled": false,
    "price": 0,
    "clicksCount": 420,
    "createdAt": "2026-06-18T18:30:00+09:00"
  }
}
```

---

### 2.4. 단축 링크 수정 (`PATCH /links/{id}`)
단축 링크의 일부 설정값(목적지 URL, 픽셀 ID, SEO 설정 등)을 동적으로 수정합니다.

* **요청 바디 (Request Body)**: (수정할 필드만 입력 가능)
```json
{
  "defaultTargetUrl": "https://example.com/products/new-url",
  "fbPixelId": "9999999999",
  "title": "수정된 타이틀"
}
```

* **응답 바디 (Response Body)**:
  * HTTP Status: `200 OK`
```json
{
  "success": true,
  "data": {
    "id": "link_uuid_12345",
    "shortCode": "my-promo",
    "defaultTargetUrl": "https://example.com/products/new-url",
    "title": "수정된 타이틀",
    "fbPixelId": "9999999999",
    "clicksCount": 420,
    "createdAt": "2026-06-18T18:30:00+09:00"
  }
}
```

---

### 2.5. 단축 링크 삭제 (`DELETE /links/{id}`)
단축 링크 및 그와 연결된 통계 로그를 삭제합니다.

* **응답 바디 (Response Body)**:
  * HTTP Status: `200 OK`
```json
{
  "success": true,
  "data": {
    "message": "링크가 성공적으로 삭제되었습니다."
  }
}
```

---

### 2.6. 단축 링크별 상세 클릭 통계 조회 (`GET /links/{id}/stats`)
링크 접속자들의 기기, OS, 최근 유입 동향 등을 종합적으로 수집해 대시보드에 뿌려주기 위한 API입니다. (2~3단계 분석 고도화 연계)

* **응답 바디 (Response Body)**:
  * HTTP Status: `200 OK`
```json
{
  "success": true,
  "data": {
    "totalClicks": 420,
    "devices": {
      "MOBILE": 310,
      "DESKTOP": 100,
      "TABLET": 10
    },
    "operatingSystems": {
      "iOS": 200,
      "Android": 110,
      "Windows": 80,
      "macOS": 30
    },
    "referrers": {
      "instagram.com": 250,
      "facebook.com": 100,
      "direct": 50,
      "naver.com": 20
    },
    "recentDailyClicks": [
      { "date": "2026-06-12", "clicks": 45 },
      { "date": "2026-06-13", "clicks": 50 },
      { "date": "2026-06-14", "clicks": 75 },
      { "date": "2026-06-15", "clicks": 60 },
      { "date": "2026-06-16", "clicks": 90 },
      { "date": "2026-06-17", "clicks": 100 }
    ]
  }
}
```

---

## 3. 예외 및 에러 처리 (Errors)

비즈니스 검증 오류 발생 시 다음과 같은 표준 에러 코드 형식을 사용합니다.

| HTTP 상태 코드 | 에러 코드 (code) | 상황 및 설명 |
| :--- | :--- | :--- |
| `400 Bad Request` | `INVALID_INPUT` | 필수 인자 누락, URL 형식 위반, customSlug 중복 등 |
| `401 Unauthorized`| `UNAUTHORIZED` | 로그인 정보 누락 또는 토큰 만료 |
| `404 Not Found`   | `LINK_NOT_FOUND` | 요청한 링크 ID에 매칭되는 데이터가 DB에 없음 |
| `409 Conflict`    | `SLUG_DUPLICATED`| 사용자가 직접 요청한 단축 customSlug가 이미 존재함 |
| `500 Server Error`| `INTERNAL_ERROR` | 데이터베이스 연결 유실 또는 처리 중 예기치 못한 내부 서버 오류 |
