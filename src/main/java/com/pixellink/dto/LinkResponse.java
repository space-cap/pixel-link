package com.pixellink.dto;

import com.pixellink.model.Link;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LinkResponse {
    private String id;
    private String userId;
    private String shortCode;
    private String shortenedUrl;
    private String defaultTargetUrl;
    private String title;
    private String description;
    private String ogImage;
    private String fbPixelId;
    private String gaTrackingId;
    private String customScript;
    private boolean isAdEnabled;
    private int adTimerSeconds;
    private boolean isPaywalled;
    private int price;
    private int clicksCount;
    private LocalDateTime createdAt;
    private LocalDateTime expiredAt;
    private java.util.List<RouteRuleResponse> routeRules;

    @Data
    public static class RouteRuleResponse {
        private String id;
        private String ruleType;
        private String ruleValue;
        private String targetUrl;
    }

    public static LinkResponse from(Link link, String baseUrl) {
        LinkResponse response = new LinkResponse();
        response.setId(link.getId());
        response.setUserId(link.getUserId());
        response.setShortCode(link.getShortCode());
        // BaseUrl 끝부분 슬래시 처리 예방
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        response.setShortenedUrl(normalizedBaseUrl + "/" + link.getShortCode());
        response.setDefaultTargetUrl(link.getDefaultTargetUrl());
        response.setTitle(link.getTitle());
        response.setDescription(link.getDescription());
        response.setOgImage(link.getOgImage());
        response.setFbPixelId(link.getFbPixelId());
        response.setGaTrackingId(link.getGaTrackingId());
        response.setCustomScript(link.getCustomScript());
        response.setAdEnabled(link.isAdEnabled());
        response.setAdTimerSeconds(link.getAdTimerSeconds());
        response.setPaywalled(link.isPaywalled());
        response.setPrice(link.getPrice());
        response.setClicksCount(link.getClicksCount());
        response.setCreatedAt(link.getCreatedAt());
        response.setExpiredAt(link.getExpiredAt());
        return response;
    }

    public static LinkResponse from(Link link, java.util.List<com.pixellink.model.RouteRule> rules, String baseUrl) {
        LinkResponse response = from(link, baseUrl);
        if (rules != null) {
            response.setRouteRules(rules.stream().map(r -> {
                RouteRuleResponse rRes = new RouteRuleResponse();
                rRes.setId(r.getId());
                rRes.setRuleType(r.getRuleType());
                rRes.setRuleValue(r.getRuleValue());
                rRes.setTargetUrl(r.getTargetUrl());
                return rRes;
            }).collect(java.util.stream.Collectors.toList()));
        }
        return response;
    }
}

