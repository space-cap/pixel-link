# 13_links_pagination_implementation_plan.md - 단축 링크 목록 페이징, 검색 및 메뉴 분리 구현 계획서

본 문서는 대시보드 메인 화면에 퀵뷰 형태로만 최근 링크 목록을 남기고, 모든 단축 링크 이력을 검색/추적할 수 있는 독립된 "링크 관리(My Links)" 페이지를 신설하고 백엔드 MyBatis 페이징 처리(Limit/Offset) 및 **키워드 검색 기능**을 통합 구축하기 위한 구현 계획서입니다.

---

## 1. User Review Required

> [!IMPORTANT]
> **검색 및 페이징 결합 조건**
> - **검색 대상**: 단축 코드(slug), 목적지 URL, SEO 제목(title)을 대상으로 검색 키워드가 포함된 결과를 필터링합니다.
> - **페이징 조건**: 검색어가 적용된 상태(예: `search=promo`)에서도 페이징이 정상 유지되도록 URL 파라미터 전파를 결합 설계합니다. (예: `/dashboard/links?page=2&search=promo&userId=test-user`)
> - **기본 규격**: 1페이지당 **10개** 노출 처리.

> [!TIP]
> **대시보드 메인 퀵뷰(Quick View) 연계**
> - 대시보드 메인 화면(`/dashboard`) 우측에는 최근 생성한 단축 링크 **5개**만 노출시켜 정보 집약도를 높이고 하단에 `[전체 내역 보러가기]` 버튼을 두어 링크 관리 페이지로의 쾌적한 이동을 유도합니다.

---

## 2. Open Questions

> [!NOTE]
> **검색어 초기화 및 실시간 필터링**
> - 검색창 우측에 `✕` (초기화) 버튼을 두어 손쉽게 전체 목록으로 복귀할 수 있게 설계하며, 엔터 키 혹은 검색 돋보기 아이콘을 누르면 검색 조회가 즉시 반영되도록 구현합니다.

---

## 3. Proposed Changes

### 3.1. Database & Mapper Layer (검색 조건절 반영 페이징 쿼리)

#### [MODIFY] [LinkMapper.java](file:///h:/lee/pixel-link/src/main/java/com/pixellink/mapper/LinkMapper.java)
- 페이징 및 검색 필터 조회를 위한 MyBatis 맵퍼 메소드 신설:
  ```java
  List<Link> findByUserIdPaged(
      @Param("userId") String userId, 
      @Param("search") String search, 
      @Param("limit") int limit, 
      @Param("offset") int offset
  );
  ```
- 검색 조건 필터가 적용된 유저별 총 단축 링크 개수 카운트 메소드 신설:
  `int countByUserId(@Param("userId") String userId, @Param("search") String search);`

#### [MODIFY] [LinkMapper.xml](file:///h:/lee/pixel-link/src/main/resources/mapper/LinkMapper.xml)
- `findByUserIdPaged` 쿼리 수정 (SQLite/PostgreSQL 공통 `LIKE` 매칭 조건 동적 SQL 추가)
  ```xml
  <select id="findByUserIdPaged" resultType="com.pixellink.model.Link">
    SELECT * FROM links 
    WHERE user_id = #{userId}
    <if test="search != null and search != ''">
      AND (
        short_code LIKE '%' || #{search} || '%'
        OR default_target_url LIKE '%' || #{search} || '%'
        OR title LIKE '%' || #{search} || '%'
      )
    </if>
    ORDER BY created_at DESC
    LIMIT #{limit} OFFSET #{offset}
  </select>
  ```
- `countByUserId` 쿼리 수정 (검색 필터 반영 count)
  ```xml
  <select id="countByUserId" resultType="int">
    SELECT COUNT(*) FROM links 
    WHERE user_id = #{userId}
    <if test="search != null and search != ''">
      AND (
        short_code LIKE '%' || #{search} || '%'
        OR default_target_url LIKE '%' || #{search} || '%'
        OR title LIKE '%' || #{search} || '%'
      )
    </if>
  </select>
  ```

---

### 3.2. Service & Controller Layer (비즈니스 및 라우팅)

#### [MODIFY] [LinkService.java](file:///h:/lee/pixel-link/src/main/java/com/pixellink/service/LinkService.java)
- 검색 파라미터를 수용하는 페이징 서비스 호출부 구현:
  ```java
  public List<LinkResponse> getLinksByUserIdPaged(String userId, String search, int page, int pageSize, String baseUrl) {
      int offset = (page - 1) * pageSize;
      List<Link> links = linkMapper.findByUserIdPaged(userId, search, pageSize, offset);
      return links.stream().map(l -> LinkResponse.from(l, baseUrl)).collect(Collectors.toList());
  }
  public int getLinkCountByUserId(String userId, String search) {
      return linkMapper.countByUserId(userId, search);
  }
  ```

#### [MODIFY] [DashboardController.java](file:///h:/lee/pixel-link/src/main/java/com/pixellink/controller/DashboardController.java)
- `/dashboard/links` 경로 GET 매핑 신설.
  - 파라미터 `search` (검색어, 기본값 null) 주입받아 매퍼 바인딩.
  - 총 페이지 개수(`totalPages`) 및 시작/끝 페이지네이션 블록 인덱스 계산 후 Model에 주입.
  - 검색어도 뷰 템플릿의 페이이션 및 입력 인풋 창에 바인딩되도록 `searchKeyword` 애트리뷰트 주입.
- `/dashboard` 메인 매핑에서 전체 목록 조회 대신 페이징 1페이지 5건으로 잘라 퀵뷰 형태 데이터 주입.

---

### 3.3. View Templates & CSS Layer

#### [MODIFY] [fragments.html](file:///h:/lee/pixel-link/src/main/resources/templates/fragments.html)
- 사이드바 내에 `🔗 링크 관리` 메뉴 신설 (대시보드와 수익/정산 사이 위치).
- 템플릿 번역 키 `#{menu.links}` 매핑.

#### [NEW] [links.html](file:///h:/lee/pixel-link/src/main/resources/templates/links.html)
- 독립된 전체 단축 링크 리스트 및 모달 통계 조회 뷰.
- **포함 UI**: 검색창(Search Input) 및 검색 버튼, 링크 카드 리스트, 하단 원형 디자인의 페이지 번호 목록 네비게이션 컴포넌트 (검색 파라미터 보존 탑재).

#### [MODIFY] [dashboard.html](file:///h:/lee/pixel-link/src/main/resources/templates/dashboard.html)
- 우측 링크 목록 타이틀을 `최근 생성된 단축 링크 (최근 5개)`로 변경.
- 목록 하단에 링크 관리 뷰 이동 버튼(`[전체 단축 링크 관리 바로가기]`) 추가.

#### [MODIFY] [global.css](file:///h:/lee/pixel-link/src/main/resources/static/css/global.css)
- 검색 컴포넌트 래퍼 및 인풋, 초기화 버튼 스타일 정의.
- 페이지네이션 버튼 및 호버 인터랙션 CSS 토큰 정의.

---

## 4. Verification Plan

### 4.1. Automated Tests (자동 검증)
- `DashboardControllerTest.java`에 `/dashboard/links` 매핑 검증 추가.
  - 검색어 파라미터(`search=test`) 전송 시 필터링 처리 상태 200 OK 여부 확인.
- MyBatis Like 검색 포함 페이징 SELECT 쿼리 동작 여부 단위 테스트 검증.

### 4.2. Manual Verification (수동 검증 시나리오)
- 로컬 서버 기동 상태에서 브라우저 접속 후 다음 흐름 작동 확인:
  1. 단축 링크를 12개 생성 (그 중 3개 링크에 "promo"라는 단어가 포함되도록 제목이나 URL 구성).
  2. 링크 관리 페이지 진입 후 검색창에 "promo" 입력 후 검색 트리거.
  3. 전체 12개가 아닌 "promo"가 들어간 3개 링크만 필터링되어 화면에 출력되는지 확인.
  4. 검색어가 적용된 상태에서 페이징 번호 클릭 시 검색 상태가 보존되어 렌더링되는지 확인.
