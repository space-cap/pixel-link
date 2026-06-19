package com.pixellink.controller;

import com.pixellink.dto.LinkCreateRequest;
import com.pixellink.dto.LinkResponse;
import com.pixellink.dto.SessionUser;
import com.pixellink.service.LinkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/links")
public class LinkApiController {

    @Autowired
    private LinkService linkService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createLink(
            @Valid @RequestBody LinkCreateRequest request,
            @AuthenticationPrincipal SessionUser sessionUser,
            HttpServletRequest httpServletRequest) {
        
        String userId = (sessionUser != null) ? sessionUser.getId() : null;
        String baseUrl = getBaseUrl(httpServletRequest);
        LinkResponse linkResponse = linkService.createLink(request, userId, baseUrl);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", linkResponse);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteLink(
            @PathVariable("id") String id,
            @AuthenticationPrincipal SessionUser sessionUser) {
        
        if (sessionUser == null) {
            throw new SecurityException("로그인이 필요한 서비스입니다.");
        }
        String userId = sessionUser.getId();
        linkService.deleteLink(id, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        
        Map<String, Object> data = new HashMap<>();
        data.put("message", "링크가 성공적으로 삭제되었습니다.");
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<Map<String, Object>> getLinkStats(
            @PathVariable("id") String id,
            @AuthenticationPrincipal SessionUser sessionUser) {
        
        if (sessionUser == null) {
            throw new SecurityException("로그인이 필요한 서비스입니다.");
        }
        String userId = sessionUser.getId();
        Map<String, Object> stats = linkService.getLinkStats(id, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", stats);

        return ResponseEntity.ok(response);
    }

    // --- 예외 처리 핸들러 (REST 규격화) ---

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException e) {
        return buildErrorResponse("LIMIT_EXCEEDED", e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return buildErrorResponse("INVALID_INPUT", e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurity(SecurityException e) {
        return buildErrorResponse("UNAUTHORIZED", e.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "입력값이 올바르지 않습니다.";
        return buildErrorResponse("INVALID_INPUT", message, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        return buildErrorResponse("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(String code, String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);

        Map<String, Object> error = new HashMap<>();
        error.put("code", code);
        error.put("message", message);
        response.put("error", error);

        return ResponseEntity.status(status).body(response);
    }

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();

        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);

        if (("http".equals(scheme) && serverPort != 80) || ("https".equals(scheme) && serverPort != 443)) {
            url.append(":").append(serverPort);
        }
        return url.toString();
    }

    @PostMapping("/admin/settings")
    public ResponseEntity<Map<String, Object>> updateSetting(
            @RequestParam("key") String key,
            @RequestParam("value") String value,
            @AuthenticationPrincipal SessionUser sessionUser) {
        
        if (sessionUser == null || !"admin".equals(sessionUser.getId())) {
            throw new SecurityException("관리자 권한이 없습니다.");
        }

        linkService.updateSystemSetting(key, value);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        Map<String, Object> data = new HashMap<>();
        data.put("message", "설정이 성공적으로 변경되었습니다.");
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/ad-click")
    public ResponseEntity<Map<String, Object>> recordAdClick(
            @PathVariable("id") String id,
            HttpServletRequest request) {
        
        String clientIp = getClientIp(request);
        linkService.recordAdClick(id, clientIp);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        Map<String, Object> data = new HashMap<>();
        data.put("message", "광고 클릭 로그 및 70% 수익금이 성공적으로 수집/적재되었습니다.");
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Map<String, Object>> withdrawSettlements(
            @RequestParam("bankName") String bankName,
            @RequestParam("accountNumber") String accountNumber,
            @RequestParam("accountHolder") String accountHolder,
            @AuthenticationPrincipal SessionUser sessionUser) {
        
        if (sessionUser == null) {
            throw new SecurityException("로그인이 필요한 서비스입니다.");
        }
        String userId = sessionUser.getId();
        linkService.withdrawSettlements(userId, bankName, accountNumber, accountHolder);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        Map<String, Object> data = new HashMap<>();
        data.put("message", "출금 신청 처리가 완결되었습니다. 은행 영업일 기준 3일 이내에 송금됩니다.");
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/payments/confirm")
    public ResponseEntity<Map<String, Object>> confirmPayment(
            @PathVariable("id") String id,
            @RequestParam("amount") int amount,
            HttpServletRequest request) {
        
        String clientIp = getClientIp(request);
        linkService.confirmPayment(id, clientIp, amount);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        Map<String, Object> data = new HashMap<>();
        data.put("message", "페이월 콘텐츠 접근 결제가 완료되었습니다.");
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}

