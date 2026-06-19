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
