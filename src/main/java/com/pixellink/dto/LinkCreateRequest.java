package com.pixellink.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LinkCreateRequest {

    @NotBlank(message = "목적지 URL은 필수 입력 항목입니다.")
    @Pattern(regexp = "^(https?://)?[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(:[0-9]+)?(/.*)?$", message = "올바른 URL 형식이어야 합니다.")
    private String defaultTargetUrl;

    @Size(max = 50, message = "맞춤형 주소는 50자 이하여야 합니다.")
    @Pattern(regexp = "^[a-zA-Z0-9_-]*$", message = "맞춤형 주소는 영문, 숫자, 하이픈(-), 언더바(_)만 가능합니다.")
    private String shortCode;

    @Size(max = 100, message = "SEO 제목은 100자 이하여야 합니다.")
    private String title;

    private String description;

    private String ogImage;

    @Size(max = 50, message = "페이스북 픽셀 ID는 50자 이하여야 합니다.")
    private String fbPixelId;

    @Size(max = 50, message = "구글 GA ID는 50자 이하여야 합니다.")
    private String gaTrackingId;

    private String customScript;

    private boolean adEnabled;
    private int adTimerSeconds = 1;
    private boolean paywalled;
    private int price;
    private java.util.List<RouteRuleRequest> routeRules;

    @lombok.Data
    public static class RouteRuleRequest {
        private String ruleType;
        private String ruleValue;
        private String targetUrl;
    }
}

