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
