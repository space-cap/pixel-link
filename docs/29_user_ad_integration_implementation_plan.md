# 마케터 개인 광고 코드 삽입 모델 구현 계획서 (ADR-001 연동)

본 문서는 플랫폼의 애드센스 정지 리스크를 방지하고 어뷰징 위험을 분산하기 위해, 마케터가 본인의 **Google Publisher ID** 및 **Ad Slot ID**를 연동하여 단축 링크 대기 화면에 직접 개인 광고를 송출할 수 있도록 하는 기능의 구현 계획서입니다.

---

## 1. 개요 및 설계 방향
* **하이브리드 바인딩**: 마케터가 매번 링크를 생성할 때마다 광고 코드를 입력하는 번거로움을 덜기 위해 **'회원 설정에 기본 광고 코드 저장'** 기능을 지원합니다.
* **유연한 커스텀**: 링크를 생성할 때는 기본 설정을 그대로 따르거나(Default), 특정 링크만 광고를 끄거나, 혹은 다른 전용 광고 코드로 자유롭게 변경(Override)할 수 있도록 설계합니다.
* **독립적 데이터 보존**: 링크가 생성되는 시점에 사용자의 광고 코드를 복사하여 `links` 테이블에 물리적으로 저장합니다. 이로 인해 마케터가 추후 프로필에서 기본 코드를 수정하더라도 기존에 이미 생성되어 유포된 단축 링크의 광고 타겟은 변하지 않고 유지됩니다.

---

## 2. 데이터베이스 스키마 변경 (DDL)

### 2.1. `users` 테이블 신규 컬럼 추가
회원의 기본 광고 설정값을 저장합니다.
```sql
ALTER TABLE users ADD COLUMN default_ad_publisher_id VARCHAR(100) DEFAULT NULL;
ALTER TABLE users ADD COLUMN default_ad_slot_id VARCHAR(100) DEFAULT NULL;
```
* 적용 파일:
  - [schema-sqlite.sql](file:///c:/workdir/space-cap/pixel-link/src/main/resources/db/schema-sqlite.sql)
  - [schema-postgresql.sql](file:///c:/workdir/space-cap/pixel-link/src/main/resources/db/schema-postgresql.sql)

### 2.2. `links` 테이블 신규 컬럼 추가
단축 링크가 최종적으로 렌더링할 광고 설정값을 저장합니다.
```sql
ALTER TABLE links ADD COLUMN ad_publisher_id VARCHAR(100) DEFAULT NULL;
ALTER TABLE links ADD COLUMN ad_slot_id VARCHAR(100) DEFAULT NULL;
```
* 적용 파일:
  - [schema-sqlite.sql](file:///c:/workdir/space-cap/pixel-link/src/main/resources/db/schema-sqlite.sql)
  - [schema-postgresql.sql](file:///c:/workdir/space-cap/pixel-link/src/main/resources/db/schema-postgresql.sql)

---

## 3. 백엔드 구현 계획 (Java & MyBatis)

### 3.1. 도메인 모델 클래스 확장
* `User.java`: `defaultAdPublisherId`, `defaultAdSlotId` 필드 및 Getter/Setter 추가.
* `Link.java`: `adPublisherId`, `adSlotId` 필드 및 Getter/Setter 추가.

### 3.2. MyBatis 매퍼 XML 및 인터페이스 보완
* `UserMapper.xml` 및 `LinkMapper.xml`:
  - `resultMap`에 신규 컬럼 매핑 추가.
  - 사용자 등록/수정(`updateUser`) 쿼리에 신규 컬럼 업데이트 추가.
  - 링크 등록(`insertLink`) 및 조회(`getLinkById`, `getLinkByShortCode`) 쿼리에 신규 컬럼 추가.

### 3.3. 회원 정보 관리 기능 확장 (Controller / Service)
* 마케터가 회원 정보 페이지에서 자신의 기본 광고 코드를 입력하여 저장할 수 있도록 사용자 정보 업데이트 API(`/api/users/profile` 등)를 구현 또는 보완합니다.

### 3.4. 단축 링크 생성 로직 고도화 (`LinkApiController.java`)
* 링크 생성 요청 DTO에 `adPublisherId` 및 `adSlotId` 추가.
* 링크 생성 시:
  - 광고 활성화(`adEnabled = true`) 상태인데 광고 코드가 입력되지 않았거나 "기본값 사용"으로 전송된 경우, 현재 로그인된 `SessionUser` 객체로부터 사용자의 `defaultAdPublisherId` 및 `defaultAdSlotId`를 읽어와 `links` 객체에 바인딩 후 삽입합니다.

### 3.5. 리다이렉트 대기 화면 광고 주입 (`RedirectionController.java` & `redirect.html`)
* 단축 코드 리다이렉트 처리 시, 해당 링크에 저장된 `ad_publisher_id`와 `ad_slot_id`가 존재하는지 검사합니다.
* 값이 존재한다면, 템플릿 모델(Model)에 해당 정보를 실어 보냅니다.
* [redirect.html](file:///c:/workdir/space-cap/pixel-link/src/main/resources/templates/redirect.html)에서는 Thymeleaf 마크업을 통해 구글 애드센스 공식 스크립트를 동적으로 렌더링합니다:
  ```html
  <script async src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=[[${adPublisherId}]]" crossorigin="anonymous"></script>
  <!-- 동적 렌더링 광고 배너 -->
  <ins class="adsbygoogle"
       style="display:block; text-align:center;"
       data-ad-layout="in-article"
       data-ad-format="fluid"
       th:attr="data-ad-client=${adPublisherId}, data-ad-slot=${adSlotId}"></ins>
  <script>
       (adsbygoogle = window.adsbygoogle || []).push({});
  </script>
  ```

---

## 4. 프론트엔드 UI 변경 계획 (Thymeleaf & CSS)

### 4.1. 회원 대시보드 내 프로필 설정 영역 수정
* 대시보드 혹은 회원 정보 수정 메뉴 하단에 **"수익화 구글 광고 설정 (기본값)"** 섹션 신설.
* 입력창 구성:
  * Google Publisher ID (`pub-xxxxxxxxxxxxxx` 형식 검증)
  * Default Ad Slot ID (숫자 형식 검증)
  * *가이드 링크*: 구글 애드센스 대시보드에서 내 퍼블리셔 ID 및 광고 슬롯 번호를 확인하는 방법을 팝업 또는 툴팁 형태로 안내.

### 4.2. 단축 링크 생성 화면 개편 (`create.html`)
* '수익화 설정' 토글을 켰을 때:
  * **"내 프로필의 기본 광고 코드 사용"** 체크박스를 기본 활성화 상태로 제공.
  * 체크를 해제하면, 해당 링크에만 다른 코드를 넣을 수 있는 **전용 퍼블리셔 ID 및 광고 슬롯 ID 입력창**이 부드러운 아코디언 애니메이션으로 펼쳐지며 노출되도록 자바스크립트 및 CSS 보완.

---

## 5. 검증 계획 (Verification Plan)

### 5.1. 자동화 단위/통합 테스트
* `LinkServiceTest.java`: 링크 생성 시 사용자의 기본 광고 설정값이 링크 레코드에 제대로 바인딩되어 데이터베이스에 무결하게 저장되는지 검증하는 JUnit 테스트 코드 작성.

### 5.2. 수동 검증 및 시나리오 테스트
1. **시나리오 1: 기본 광고 코드 등록 및 기본값 생성**
   - 회원 정보 페이지에서 `pub-default` 및 `slot-111`을 기본값으로 등록.
   - 단축 링크 생성 페이지에서 '기본 광고 코드 사용' 상태로 링크 생성.
   - 생성된 링크 대기 화면 접속 시 `pub-default`와 `slot-111`이 구글 스크립트에 정상 반영되어 렌더링되는지 확인.
2. **시나리오 2: 개별 링크 광고 코드 재정의 (Override)**
   - 동일한 회원 상태에서, 링크 생성 시 체크박스를 해제하고 `pub-custom` 및 `slot-222`로 입력하여 링크 생성.
   - 해당 링크 접속 시 `pub-custom` 및 `slot-222` 코드가 배너 영역에 렌더링되는지 확인.
3. **시나리오 3: 광고 없는 일반 단축 링크 생성**
   - 수익화 옵션을 비활성화하여 링크 생성.
   - 해당 링크 접속 시 광고 스크립트 및 배너 영역이 깔끔하게 생략된 채 본래의 타이머 및 목적지 리다이렉트 흐름만 작동하는지 확인.
