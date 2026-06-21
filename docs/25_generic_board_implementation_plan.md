# 📋 공통 게시판 (Generic Board) 아키텍처 구축 계획서

본 문서는 서비스 내에 공지사항, 자유게시판, 1:1 문의, 제휴 문의 등 다양한 형태의 게시판 요구사항을 단 하나의 공통 소스코드와 데이터 테이블로 효율적으로 소화할 수 있는 **공통 게시판 (Generic Board) 아키텍처**의 구현 계획을 정리한 문서입니다.

---

## 💡 게시판 아키텍처 설계 사상에 대한 의견
사용자님의 피드백을 수용하여 다음 3대 아키텍처 원칙을 바탕으로 설계했습니다.
1. **비회원 글쓰기 및 전용 폼 전면 지원**: 로그인하지 않은 예비 광고주나 익명 유저가 1:1 문의 및 제휴 제안을 원활히 보낼 수 있도록 비회원 글쓰기 폼을 제공하고, 비밀글의 경우 **[비회원 비밀번호]** 대조를 통해 열람/수정/삭제를 제한합니다.
2. **로컬 디스크 파일 업로드 기능 즉시 이식**: 첨부파일 관리를 위한 전용 테이블(`board_attachments`)을 신설하고, 다중 파일 업로드 및 다운로드 기능을 초기 버전부터 함께 빌드하여 비즈니스 완성도를 높입니다.
3. **댓글/관리자 답변 테이블의 통합 설계**: 자유게시판의 댓글과 1:1 문의 및 제휴 문의에 대한 관리자의 공식 피드백을 단일 댓글 테이블(`board_comments`)로 일원화하여 상태 변경(`status='RESOLVED'`)과 연동합니다.

---

## 1. 데이터베이스(DB) 스키마 설계

### 1.1. `board_articles` (게시글 테이블)

| 컬럼명 | 데이터 타입 | 제약 조건 | 설명 |
| :--- | :--- | :--- | :--- |
| `id` | VARCHAR(50) | PRIMARY KEY | 게시글 고유 식별 ID (UUID 등) |
| `board_type` | VARCHAR(50) | NOT NULL | 게시판 구분 (`NOTICE`, `FREE`, `QNA`, `PARTNERSHIP` 등) |
| `title` | VARCHAR(255) | NOT NULL | 게시글 제목 |
| `content` | TEXT | NOT NULL | 게시글 본문 내용 |
| `author_id` | VARCHAR(50) | NULL | 작성자 ID (`users.id` 외래키, 비회원 작성 시 NULL 허용) |
| `author_name` | VARCHAR(100) | NOT NULL | 작성자 이름 또는 닉네임 |
| `is_secret` | BOOLEAN/INT | DEFAULT FALSE | 비밀글 여부 (1:1 문의용, 본인 및 관리자만 조회 가능) |
| `password` | VARCHAR(255) | NULL | 비회원 작성글 수정/삭제용 암호화된 비밀번호 |
| `status` | VARCHAR(50) | DEFAULT 'OPEN' | 처리 상태 (`OPEN` (일반), `RESOLVED` (답변완료), `PENDING` (제휴검토중)) |
| `view_count` | INTEGER | DEFAULT 0 | 조회수 |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 최초 작성일시 |
| `updated_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 최종 수정일시 |

### 1.2. `board_comments` (댓글 및 관리자 답변 테이블)

| 컬럼명 | 데이터 타입 | 제약 조건 | 설명 |
| :--- | :--- | :--- | :--- |
| `id` | VARCHAR(50) | PRIMARY KEY | 댓글 고유 식별 ID |
| `article_id` | VARCHAR(50) | REFERENCES board_articles(id) | 대상 게시글 ID (외래키, cascade delete) |
| `author_id` | VARCHAR(50) | NULL | 작성자 ID (비회원 허용 시 NULL) |
| `author_name` | VARCHAR(100) | NOT NULL | 작성자 노출 이름 |
| `content` | TEXT | NOT NULL | 댓글 및 답변 내용 |
| `is_admin_reply` | BOOLEAN/INT | DEFAULT FALSE | 관리자 공식 답변 여부 (1:1 문의 답변 시 강조 표시용) |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 작성일시 |

### 1.3. `board_attachments` (첨부파일 테이블) [NEW]

| 컬럼명 | 데이터 타입 | 제약 조건 | 설명 |
| :--- | :--- | :--- | :--- |
| `id` | VARCHAR(50) | PRIMARY KEY | 첨부파일 고유 식별 ID |
| `article_id` | VARCHAR(50) | REFERENCES board_articles(id) | 대상 게시글 ID (외래키, cascade delete) |
| `original_filename` | VARCHAR(255) | NOT NULL | 사용자가 업로드한 원래 파일명 |
| `stored_filename` | VARCHAR(255) | NOT NULL | 디스크에 중복 방지 저장된 파일명 (UUID) |
| `file_path` | VARCHAR(500) | NOT NULL | 실제 디스크 저장 경로 |
| `file_size` | BIGINT | NOT NULL | 파일 크기 (Bytes) |
| `file_type` | VARCHAR(100) | NOT NULL | 파일 MIME 타입 (예: `image/png`, `application/pdf`) |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 업로드 일시 |

---

## 2. 권한 및 보안 설계 (Security & Authorization)

게시판의 유형(`board_type`)과 로그인 세션에 따라 상세 열람 및 글쓰기 권한을 통제합니다.

### 2.1. 비회원 비밀글 조회 및 가이드
- 비회원이 `is_secret=true`(비밀글)로 설정하여 작성한 문의는 상세 조회 API 호출 시 **사용자 비밀번호 검증 모달**을 띄웁니다.
- 비밀번호 검증이 통과되거나, 로그인한 유저가 해당 글의 `author_id`와 동일하거나, 혹은 로그인 유저의 권한이 `ADMIN`인 경우에만 게시글 상세 데이터와 첨부파일 정보를 노출시킵니다.
- 비회원의 비밀번호는 **`BCryptPasswordEncoder`**를 사용하여 암호화(Hash)한 뒤 DB에 안전하게 보존합니다.

### 2.2. 로컬 디스크 파일 업로드 처리 프로세스
- 파일 업로드의 루트 경로(예: `/home/ubuntu/pixel-link-uploads` 혹은 로컬 임시 폴더)를 `application.properties` 설정 변수로 제어합니다.
- 서버 측에서는 `MultipartFile`을 수신받아 파일의 확장자를 체크하고, UUID 파일명으로 안전하게 디스크에 기록하여 웹 취약점(웹쉘 업로드 공격 등)을 사전에 차단합니다.
- 첨부파일 다운로드 API(`/api/board/attachments/{id}/download`)를 구현하여 글 조회 권한이 있는 사용자만 파일을 안전하게 다운로드받을 수 있게 통제합니다.

---

## 3. 구현 예정 컴포넌트 목록

1. **도메인 모델 (Model)**: `BoardArticle`, `BoardComment`, `BoardAttachment`
2. **MyBatis 매퍼 (Mapper)**:
   - `BoardArticleMapper` (`insert`, `findById`, `findAllPaged`, `updateStatus`, `delete`)
   - `BoardCommentMapper` (`insert`, `findByArticleId`, `delete`)
   - `BoardAttachmentMapper` (`insert`, `findByArticleId`, `findById`, `delete`)
3. **서비스 레이어 (Service)**: `BoardService` (게시글/댓글 등록, 파일 저장, 비회원 비밀번호 대조, 권한 검증 구현)
4. **컨트롤러 (Controller)**:
   - `BoardController` (게시글 목록/상세 뷰 서빙, 비회원 비밀번호 확인 폼 서빙)
   - `BoardApiController` (비동기 글쓰기/수정/삭제, 파일 업로드/다운로드, 댓글 등록 API 지원)
5. **사용자 인터페이스 (View)**:
   - `board/list.html` (공지사항/자유게시판 리스트 및 검색, 페이징 지원)
   - `board/detail.html` (상세 본문, 첨부파일 다운로드 영역, 댓글/관리자 답변 렌더링)
   - `board/create.html` (글쓰기 폼 - 비회원 가입 옵션 및 파일 업로드 필드 지원)

---

## 4. 검증 계획 (Verification Plan)

### 4.1. 단위 및 통합 테스트 시나리오
1. **공지사항 조회/쓰기 권한 분기 검증**:
   - 일반 회원 및 비회원이 공지사항 쓰기를 시도하면 HTTP 403 Forbidden 권한 에러가 발생하는지 확인.
   - 어드민 계정으로 로그인 시에만 작성 폼이 보이고 글쓰기가 정상 완료되는지 검증.
2. **비회원 비밀글 보안 차단 검증**:
   - 비회원으로 작성한 1:1 비밀글에 대해, 로그인하지 않은 외부 유저가 다이렉트 URL 주소로 상세 조회를 요청했을 때 조회되지 않고 비밀번호 입력 창으로 안내되는지 검증.
   - 정상 비밀번호를 입력했을 때만 200 OK와 함께 본문 및 첨부파일 목록이 노출되는지 검증.
3. **파일 업로드/다운로드 예외 차단**:
   - 업로드된 파일이 실제 지정한 OCI 서버 경로에 고유 UUID 명칭으로 보관되는지 검증.
   - 다운로드 요청 시 글 상세 조회 권한이 있는 유저에게만 스트림 다운로드를 제공하고 권한이 없으면 차단되는지 검증.
