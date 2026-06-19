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
        // OPTIONS HTTP 메소드(CORS preflight)는 헤더 검증을 생략하고 통과시킵니다.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String apiKeyHeader = request.getHeader("X-API-KEY");

        if (apiKeyHeader == null || apiKeyHeader.trim().isEmpty()) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Missing X-API-KEY header\"}");
            return false;
        }

        ApiKey apiKey = apiKeyService.validateKey(apiKeyHeader);
        if (apiKey == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Invalid or inactive API Key\"}");
            return false;
        }

        // 컨트롤러에서 사용할 수 있게 Request attribute에 userId 저장
        request.setAttribute("authenticatedUserId", apiKey.getUserId());
        return true;
    }
}
