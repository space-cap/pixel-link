# 📋 게시판별 맞춤형 댓글 권한 정책 동적 제어 구현 계획서

본 문서는 공지사항, 자유게시판, 1:1 문의, 제휴제안 등 각 게시판의 고유한 운영 목적에 맞춰 **댓글 작성 권한을 관리자 설정 화면에서 동적으로 활성화 및 통제**할 수 있도록 하는 시스템 구축 계획을 정리한 문서입니다.

---

## 1. 개요 및 정책 정의

게시판별로 상이한 소통 성격(단방향 공지, 양방향 자유 토론, 1:1 비밀 상담 등)에 맞게 관리자가 직접 댓글 쓰기 정책을 다음과 같이 5단계로 조율할 수 있도록 구성합니다.

| 정책 코드 (Policy) | 설명 | 주로 사용되는 게시판 |
| :--- | :--- | :--- |
| `ALL` | 비회원(닉네임 입력)과 로그인 회원 모두 댓글 작성 가능 | 자유게시판 (FREE) 기본값 |
| `MEMBER_ONLY` | 로그인한 일반 유저(회원) 이상만 댓글 작성 가능 | 자유게시판 (FREE) 권장 |
| `OWNER_AND_ADMIN` | 관리자(ADMIN)와 해당 문의글을 작성한 **본인**만 댓글 작성 가능 | 1:1 문의 (QNA), 제휴제안 (PARTNERSHIP) 기본값 |
| `ADMIN_ONLY` | 관리자(ADMIN) 계정으로 로그인한 상태에서만 댓글 작성 가능 | 공지사항 (NOTICE) 기본값 |
| `DISABLED` | 댓글 기능 자체를 비활성화 (댓글 내역 및 등록 폼 전체 감춤) | 공지사항 (NOTICE) 권장 |

---

## 2. 데이터베이스(DB) 스키마 초기 설정

관리자가 수정한 댓글 설정 정책을 영속적으로 보관하기 위해 기존 `system_settings` 테이블을 활용합니다. 시스템 초기 가동 시 기본 정책이 반영될 수 있도록 DDL 스키마 파일에 초기 데이터를 인서트합니다.

### 2.1. 신규 설정 키 목록
* `board_notice_comment_policy` (기본값: `ADMIN_ONLY`)
* `board_free_comment_policy` (기본값: `ALL`)
* `board_qna_comment_policy` (기본값: `OWNER_AND_ADMIN`)
* `board_partnership_comment_policy` (기본값: `OWNER_AND_ADMIN`)

### 2.2. DDL 수정 대상 파일
* [schema-sqlite.sql](file:///c:/workdir/space-cap/pixel-link/src/main/resources/db/schema-sqlite.sql)
* [schema-postgresql.sql](file:///c:/workdir/space-cap/pixel-link/src/main/resources/db/schema-postgresql.sql)

---

## 3. 구현 예정 컴포넌트 목록

### 3.1. 관리자 설정 화면 UI 반영 ([settings.html](file:///c:/workdir/space-cap/pixel-link/src/main/resources/templates/admin/settings.html))
* 시스템 설정 페이지 하단에 **"게시판별 댓글 권한 설정"** 필드셋 추가.
* 공지사항, 자유게시판, 1:1 문의, 제휴제안 각각의 드롭다운 `<select>` 폼(ALL, MEMBER_ONLY, OWNER_AND_ADMIN, ADMIN_ONLY, DISABLED) 구성.
* [LinkService.java](file:///c:/workdir/space-cap/pixel-link/src/main/java/com/pixellink/service/LinkService.java)의 `updateSystemSettings`가 신규 설정값 4종을 정상적으로 읽어와 DB에 저장할 수 있도록 매핑 보완.

### 3.2. 웹 컨트롤러 비즈니스 정보 바인딩 ([BoardController.java](file:///c:/workdir/space-cap/pixel-link/src/main/java/com/pixellink/controller/BoardController.java))
* 상세 조회(`/board/detail/{id}`) 호출 시, 게시글의 `boardType`에 연결된 댓글 정책을 조회하여 Model에 바인딩.
* 현재 사용자의 세션 정보(또는 비회원 비밀글 인증 여부)를 대조하여, 실제 댓글 작성 권한이 충족되는지 여부를 판단하는 불리언 플래그 `canComment`를 계산해 전달.

### 3.3. API 컨트롤러 백엔드 보안 강화 ([BoardApiController.java](file:///c:/workdir/space-cap/pixel-link/src/main/java/com/pixellink/controller/BoardApiController.java))
* 댓글 등록 API(`POST /api/board/comments`) 호출 시, 대상 게시글 정보를 조회하여 설정된 댓글 권한 정책을 비교.
* 권한 조건을 충족하지 않은 악의적인 API 직접 요청(우회 등록 시도)은 `HTTP 403 Forbidden` 상태 코드로 완전히 통제하여 보안을 유지.

### 3.4. Thymeleaf 뷰 템플릿 연동 ([detail.html](file:///c:/workdir/space-cap/pixel-link/src/main/resources/templates/board/detail.html))
* `commentPolicy`가 `DISABLED`인 경우, 하단 댓글 섹션 전체를 렌더링에서 제거.
* `canComment`가 `false`이고 `commentPolicy`가 `DISABLED`가 아닌 상태일 경우:
  - 댓글 등록용 텍스트 필드를 비활성화하거나 감추고, 정책에 부합하는 안내 메시지(예: *"로그인한 회원만 댓글을 달 수 있습니다."*, *"본인 및 관리자만 댓글 작성이 가능합니다."*)를 적합하게 노출하여 안내.

---

## 4. 검증 계획

### 4.1. 자동화 테스트
* `./gradlew test`를 수행하여 리팩토링 및 DDL 초기값 주입에 따른 백엔드 API 빌드 및 컴파일 무결성을 보장.

### 4.2. 수동 기능 검증 시나리오
1. **자유게시판 권한 통제**:
   - 관리자 설정에서 자유게시판을 `MEMBER_ONLY`로 변경 후, 로그아웃 상태에서 상세 조회 시 댓글 입력창이 보이지 않고 "회원 전용" 안내 문구가 표시되는지 검증.
2. **1:1 문의 CS 흐름**:
   - `OWNER_AND_ADMIN` 조건 하에서, 글 작성자(회원 본인 혹은 비회원 비밀번호 인증을 거친 자) 및 관리자 어드민 계정으로 들어갔을 때만 정상적으로 댓글 창이 열리는지 검증.
   - 글의 소유자가 아닌 제3의 일반 사용자로 상세 접근 시 댓글 창 노출이 차단되는지 확인.
3. **공지사항 통제**:
   - 기본 설정 `ADMIN_ONLY` 하에서 어드민이 아닐 시 댓글 입력이 차단되는지 확인.
   - 설정에서 `DISABLED`로 토글 시 공지사항 하단에 아예 댓글 목록 및 댓글 영역 자체가 완전히 사라지는지 확인.
