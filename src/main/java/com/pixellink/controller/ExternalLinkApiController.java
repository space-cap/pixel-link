package com.pixellink.controller;

import com.pixellink.dto.ExternalLinkRequest;
import com.pixellink.dto.ExternalLinkResponse;
import com.pixellink.dto.LinkCreateRequest;
import com.pixellink.dto.LinkResponse;
import com.pixellink.service.LinkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

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

        String baseUrl = getBaseUrl(httpServletRequest);

        // API용 ExternalLinkRequest -> 내부 서비스용 LinkCreateRequest 변환
        LinkCreateRequest createRequest = new LinkCreateRequest();
        createRequest.setDefaultTargetUrl(request.getDefaultTargetUrl());
        createRequest.setTitle(request.getTitle());
        createRequest.setDescription(request.getDescription());
        createRequest.setAdEnabled(request.isAdEnabled());
        createRequest.setAdTimerSeconds(request.getAdTimerSeconds());
        createRequest.setPaywalled(request.isPaywalled());
        createRequest.setPrice(request.getPrice());
        createRequest.setRouteRules(new ArrayList<>()); // 기본 빈 규칙 설정

        LinkResponse linkResponse = linkService.createLink(createRequest, userId, baseUrl);

        ExternalLinkResponse response = ExternalLinkResponse.builder()
                .code(linkResponse.getShortCode())
                .shortUrl(linkResponse.getShortenedUrl())
                .originalUrl(linkResponse.getDefaultTargetUrl())
                .isAdEnabled(linkResponse.isAdEnabled())
                .isPaywalled(linkResponse.isPaywalled())
                .createdAt(linkResponse.getCreatedAt() != null ? linkResponse.getCreatedAt().toString() : null)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
}
