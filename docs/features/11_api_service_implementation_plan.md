# 11_api_service_implementation_plan.md - 단축 URL 생성 API 서비스 및 API Key 인증 도입 구현 계획서

본 문서는 외부 시스템(CRM, 알림톡 발송 서비스 등)과의 단축 URL 연동을 위해 유저별 API Key를 발급 및 관리하고, 인증 헤더(`X-API-KEY`)를 기반으로 단축 링크를 동적 생성하는 API 엔드포인트를 구축하는 변경 계획서입니다.

---

## 1. User Review Required

> [!IMPORTANT]
> **API Key 인증 방식 및 보안 정책**
> - API 호출 시 HTTP Header에 `X-API-KEY` 명칭으로 API Key를 전달합니다.
> - 키 생성 시 보안 및 식별을 위해 접두어(Prefix) `pxl_live_` 뒤에 32자리의 난수 토큰을 붙여 발급합니다.
> - 본 MVP 구현 단계에서는 유저당 1:N으로 여러 개의 키를 생성할 수 있게 하며, 구분 명칭(예: "개발 서버용", "운영 서버용")을 부여해 관리할 수 있게 설계합니다.

> [!WARNING]
> **Rate Limiting (호출 제한) 범위**
> - 이번 단계에서는 시스템의 빠른 개발과 복잡도 완화를 위해 **Rate Limiting을 설계 대상에서 제외**하고 다음 고도화 단계(12단계)의 독립 태스크로 정의합니다.

---

## 2. Open Questions

> [!NOTE]
> **API Key 노출 보안 정책**
> - 최초 키 생성 시에만 유저에게 평문 API Key를 1회 노출하고 그 이후에는 마스킹(예: `pxl_live_abcd...1234`)하여 보여줄 것인지, 아니면 상시 조회가 가능하게 할 것인지에 대해 후임과 운영팀의 보안 기조를 정립해야 합니다.
> - 본 계획서에서는 편의성을 우선하여 대시보드에서 키 목록 조회 시 상시 전체 조회가 가능하도록 평문 노출 형태로 구현을 진행한 뒤, 상용 배포 전 복구 불가능한 단방향 암호화(SHA-256) 저장 방식으로 고도화하도록 제안합니다.

---

## 3. Proposed Changes

### 3.1. Database Layer (DDL 변경)

로컬 SQLite와 상용 PostgreSQL 환경 양쪽 모두 호환되도록 DDL 스크립트를 작성합니다.

#### [MODIFY] [schema-sqlite.sql](file:///h:/lee/pixel-link/src/main/resources/db/schema-sqlite.sql)
파일 최하단에 `api_keys` 테이블 생성 구문을 추가합니다.
```sql
CREATE TABLE IF NOT EXISTS api_keys (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    api_key TEXT UNIQUE NOT NULL,
    name TEXT,
    is_active INTEGER DEFAULT 1,
    created_at TEXT DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY(user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_api_keys_token ON api_keys(api_key);
```

#### [MODIFY] [schema-postgresql.sql](file:///h:/lee/pixel-link/src/main/resources/db/schema-postgresql.sql)
파일 최하단에 `api_keys` 테이블 생성 구문을 추가합니다.
```sql
CREATE TABLE IF NOT EXISTS api_keys (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL REFERENCES users(id),
    api_key VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_api_keys_token ON api_keys(api_key);
```

---

### 3.2. Model / Mapper Layer (MyBatis 연동)

#### [NEW] [ApiKey.java](file:///h:/lee/pixel-link/src/main/java/com/pixellink/model/ApiKey.java)
API Key 테이블과 매핑되는 도메인 모델 엔티티입니다.
```java
package com.pixellink.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {
    private String id;
    private String userId;
    private String apiKey;
    private String name;
    private boolean isActive;
    private String createdAt;
}
```

#### [NEW] [ApiKeyMapper.java](file:///h:/lee/pixel-link/src/main/java/com/pixellink/mapper/ApiKeyMapper.java)
MyBatis용 매퍼 인터페이스입니다.
```java
package com.pixellink.mapper;

import com.pixellink.model.ApiKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ApiKeyMapper {
    void insert(ApiKey apiKey);
    ApiKey findByApiKey(@Param("apiKey") String apiKey);
    List<ApiKey> findByUserId(@Param("userId") String userId);
    void updateActiveStatus(@Param("id") String id, @Param("isActive") boolean isActive);
    void deleteById(@Param("id") String id);
}
```

#### [NEW] [ApiKeyMapper.xml](file:///h:/lee/pixel-link/src/main/resources/mapper/ApiKeyMapper.xml)
SQLite 및 PostgreSQL에 공통으로 대응되는 SQL XML 파일입니다.
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.pixellink.mapper.ApiKeyMapper">

    <insert id="insert">
        INSERT INTO api_keys (id, user_id, api_key, name, is_active)
        VALUES #{id}, #{userId}, #{apiKey}, #{name}, #{isActive}
    </insert>

    <select id="findByApiKey" resultType="com.pixellink.model.ApiKey">
        SELECT id, user_id AS userId, api_key AS apiKey, name, is_active AS isActive, created_at AS createdAt
        FROM api_keys
        WHERE api_key = #{apiKey} AND is_active = 1
    </select>

    <select id="findByUserId" resultType="com.pixellink.model.ApiKey">
        SELECT id, user_id AS userId, api_key AS apiKey, name, is_active AS isActive, created_at AS createdAt
        FROM api_keys
        WHERE user_id = #{userId}
        ORDER BY created_at DESC
    </select>

    <update id="updateActiveStatus">
        UPDATE api_keys
        SET is_active = #{isActive}
        WHERE id = #{id}
    </update>

    <delete id="deleteById">
        DELETE FROM api_keys
        WHERE id = #{id}
    </delete>

</mapper>
```

---

### 3.3. Service / Security Layer (인증 필터 및 키 생성 비즈니스 로직)

#### [NEW] [ApiKeyService.java](file:///h:/lee/pixel-link/src/main/java/com/pixellink/service/ApiKeyService.java)
API Key 생성 및 검증을 담당하는 비즈니스 서비스 클래스입니다.
```java
package com.pixellink.service;

import com.pixellink.mapper.ApiKeyMapper;
import com.pixellink.model.ApiKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyMapper apiKeyMapper;

    public ApiKey generateApiKey(String userId, String name) {
        String id = UUID.randomUUID().toString();
        // 접두사를 포함한 고유 토큰 생성
        String rawKey = "pxl_live_" + UUID.randomUUID().toString().replace("-", "");
        
        ApiKey apiKey = ApiKey.builder()
                .id(id)
                .userId(userId)
                .apiKey(rawKey)
                .name(name)
                .isActive(true)
                .build();
        
        apiKeyMapper.insert(apiKey);
        return apiKey;
    }

    public ApiKey validateKey(String rawKey) {
        if (rawKey == null || !rawKey.startsWith("pxl_live_")) {
            return null;
        }
        return apiKeyMapper.findByApiKey(rawKey);
    }

    public List<ApiKey> getApiKeysByUserId(String userId) {
        return apiKeyMapper.findByUserId(userId);
    }

    public void revokeKey(String id) {
        apiKeyMapper.deleteById(id);
    }
}
```

#### [NEW] [ApiKeyAuthInterceptor.java](file:///h:/lee/pixel-link/src/main/java/com/pixellink/config/ApiKeyAuthInterceptor.java)
외부 API 컨트롤러 진입 전 `X-API-KEY` 헤더를 검사하는 스프링 웹 MVC 인터셉터입니다.
```java
package com.pixellink.config;

import com.pixellink.model.ApiKey;
import com.pixellink.service.ApiKeyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthInterceptor implements HandlerInterceptor {

    private final ApiKeyService apiKeyService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String apiKeyHeader = request.getHeader("X-API-KEY");

        if (apiKeyHeader == null || apiKeyHeader.trim().isEmpty()) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing X-API-KEY header");
            return false;
        }

        ApiKey apiKey = apiKeyService.validateKey(apiKeyHeader);
        if (apiKey == null) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid or inactive API Key");
            return false;
        }

        // 후속 컨트롤러에서 활용할 수 있도록 Request 어트리뷰트에 인증된 userId 설정
        request.setAttribute("authenticatedUserId", apiKey.getUserId());
        return true;
    }
}
```

#### [MODIFY] [WebMvcConfig.java](file:///h:/lee/pixel-link/src/main/java/com/pixellink/config/WebMvcConfig.java)
작성한 `ApiKeyAuthInterceptor`를 `/api/v1/links/**` 경로에만 바인딩 처리합니다.
```java
package com.pixellink.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final ApiKeyAuthInterceptor apiKeyAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiKeyAuthInterceptor)
                .addPathPatterns("/api/v1/links/**");
    }
}
```

---

### 3.4. DTO 및 Controller Layer

#### [NEW] [ExternalLinkRequest.java](file:///h:/lee/pixel-link/src/main/java/com/pixellink/dto/ExternalLinkRequest.java)
API URL 생성 요청에 활용할 DTO입니다.
```java
package com.pixellink.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExternalLinkRequest {
    @NotBlank(message = "Original target URL is required")
    private String defaultTargetUrl;
    
    private String title;
    private String description;
    
    // 스마트 라우팅 및 수익형/결제형 옵션
    private boolean isAdEnabled;
    private int adTimerSeconds;
    private boolean isPaywalled;
    private int price;
}
```

#### [NEW] [ExternalLinkResponse.java](file:///h:/lee/pixel-link/src/main/java/com/pixellink/dto/ExternalLinkResponse.java)
생성된 단축 URL 정보를 제공하는 응답 DTO입니다.
```java
package com.pixellink.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExternalLinkResponse {
    private String code;
    private String shortUrl;
    private String originalUrl;
    private boolean isAdEnabled;
    private boolean isPaywalled;
    private String createdAt;
}
```

#### [NEW] [ExternalLinkApiController.java](file:///h:/lee/pixel-link/src/main/java/com/pixellink/controller/ExternalLinkApiController.java)
인증 필터를 통과한 외부 연동 단축 API 컨트롤러입니다.
```java
package com.pixellink.controller;

import com.pixellink.dto.ExternalLinkRequest;
import com.pixellink.dto.ExternalLinkResponse;
import com.pixellink.model.Link;
import com.pixellink.service.LinkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/links")
@RequiredArgsConstructor
public class ExternalLinkApiController {

    private final LinkService linkService;

    @PostMapping
    public ResponseEntity<ExternalLinkResponse> createShortUrl(
            @RequestAttribute("authenticatedUserId") String userId,
            @Valid @RequestBody ExternalLinkRequest request,
            HttpServletRequest httpServletRequest) {

        // 도메인 URL 획득용 BaseUrl 처리
        String baseUrl = httpServletRequest.getScheme() + "://" + httpServletRequest.getServerName();
        if (httpServletRequest.getServerPort() != 80 && httpServletRequest.getServerPort() != 443) {
            baseUrl += ":" + httpServletRequest.getServerPort();
        }

        // Link 객체 생성 및 서비스 호출
        Link link = Link.builder()
                .userId(userId)
                .defaultTargetUrl(request.getDefaultTargetUrl())
                .title(request.getTitle())
                .description(request.getDescription())
                .isAdEnabled(request.isAdEnabled())
                .adTimerSeconds(request.getAdTimerSeconds())
                .isPaywalled(request.isPaywalled())
                .price(request.getPrice())
                .build();

        Link savedLink = linkService.createLink(link);

        ExternalLinkResponse response = ExternalLinkResponse.builder()
                .code(savedLink.getShortCode())
                .shortUrl(baseUrl + "/" + savedLink.getShortCode())
                .originalUrl(savedLink.getDefaultTargetUrl())
                .isAdEnabled(savedLink.isAdEnabled())
                .isPaywalled(savedLink.isPaywalled())
                .createdAt(savedLink.getCreatedAt())
                .build();

        return ResponseEntity.ok(response);
    }
}
```

#### [NEW] [ApiKeyDashboardController.java](file:///h:/lee/pixel-link/src/main/java/com/pixellink/controller/ApiKeyDashboardController.java)
대시보드 화면상에서 API Key를 신규 등록하고 무효화(Revoke)하는 내부 관리용 컨트롤러입니다.
```java
package com.pixellink.controller;

import com.pixellink.model.ApiKey;
import com.pixellink.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard/keys")
@RequiredArgsConstructor
public class ApiKeyDashboardController {

    private final ApiKeyService apiKeyService;

    @Override
    public ResponseEntity<List<ApiKey>> getMyKeys(@RequestParam("userId") String userId) {
        return ResponseEntity.ok(apiKeyService.getApiKeysByUserId(userId));
    }

    @PostMapping
    public ResponseEntity<ApiKey> createKey(@RequestBody Map<String, String> body) {
        String userId = body.get("userId");
        String name = body.getOrDefault("name", "API Key");
        ApiKey key = apiKeyService.generateApiKey(userId, name);
        return ResponseEntity.ok(key);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokeKey(@PathVariable("id") String id) {
        apiKeyService.revokeKey(id);
        return ResponseEntity.noContent().build();
    }
}
```

---

### 3.5. UI/UX Layer (대시보드 내 관리 UI 추가)

#### [MODIFY] [dashboard.html](file:///h:/lee/pixel-link/src/main/resources/templates/dashboard.html)
대시보드 내 API Key 관리 탭 및 테이블 컴포넌트 추가
- **주요 추가 UI**:
  - API Key 생성/삭제 제어 UI 및 클립보드 복사 모션 효과 적용

---

## 4. Verification Plan

### 4.1. Automated Tests (자동 검증)
- **`ExternalLinkApiControllerTest.java` 구현**
  - `@SpringBootTest` 환경에서 MockMvc를 통한 API 호출 시나리오 검증:
    1. 헤더 없이 호출 시 -> `401 Unauthorized` 반환 확인
    2. 유효하지 않은 헤더로 호출 시 -> `401 Unauthorized` 반환 확인
    3. 유효한 API Key 헤더와 요청 바디 전송 시 -> `200 OK` 및 정상적인 `shortUrl`과 생성된 `code` 응답 확인

### 4.2. Manual Verification (수동 검증 시나리오)
- **PowerShell 기반 테스트 훅 작성**
  - `/scratch/test_api_flow.ps1`을 작성하여 `Invoke-RestMethod` 명령으로 단축 URL 생성 API 작동 확인.
