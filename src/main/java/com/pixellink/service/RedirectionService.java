package com.pixellink.service;

import com.pixellink.mapper.ClickLogMapper;
import com.pixellink.mapper.LinkMapper;
import com.pixellink.model.ClickLog;
import com.pixellink.model.Link;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class RedirectionService {

    @Autowired
    private LinkMapper linkMapper;

    @Autowired
    private ClickLogMapper clickLogMapper;

    @Autowired
    private com.pixellink.mapper.RouteRuleMapper routeRuleMapper;

    @Transactional
    public Link getLinkAndRecordClick(String shortCode, String userAgent, String clientIp, String referrer) {
        Link link = linkMapper.findByShortCode(shortCode);
        if (link == null) {
            return null;
        }

        // 1. IP 해싱 처리 (06_security.md 개인정보 규정 반영: IP + 일일 날짜 솔트 조합 SHA-256 해시)
        String ipHash = hashIpAddress(clientIp);

        // 2. User-Agent 분석 (기기 및 OS 분류)
        String deviceType = parseDeviceType(userAgent);
        String osType = parseOsType(userAgent);

        // 3. 접속 로그 객체 빌드 및 저장
        ClickLog log = new ClickLog();
        log.setId(UUID.randomUUID().toString());
        log.setLinkId(link.getId());
        log.setUserAgent(userAgent);
        log.setIpHash(ipHash);
        log.setReferrer(parseReferrerHost(referrer));
        log.setDeviceType(deviceType);
        log.setOsType(osType);
        log.setAdClicked(false);
        log.setConverted(false);

        clickLogMapper.insert(log);

        // 4. 단축 링크의 단순 클릭수 즉시 증가
        linkMapper.incrementClicksCount(link.getId());

        // 5. [2단계] 스마트 라우팅 규칙 매칭
        java.util.List<com.pixellink.model.RouteRule> rules = routeRuleMapper.findByLinkId(link.getId());
        if (rules != null && !rules.isEmpty()) {
            for (com.pixellink.model.RouteRule rule : rules) {
                boolean match = false;
                if ("OS".equalsIgnoreCase(rule.getRuleType())) {
                    if (rule.getRuleValue() != null && rule.getRuleValue().equalsIgnoreCase(osType)) {
                        match = true;
                    }
                } else if ("DEVICE".equalsIgnoreCase(rule.getRuleType())) {
                    if (rule.getRuleValue() != null && rule.getRuleValue().equalsIgnoreCase(deviceType)) {
                        match = true;
                    }
                }
                if (match) {
                    link.setDefaultTargetUrl(rule.getTargetUrl());
                    break; // 첫 번째 일치 규칙 우선 적용
                }
            }
        }

        return link;
    }


    private String hashIpAddress(String ip) {
        if (ip == null) ip = "127.0.0.1";
        String salt = LocalDate.now().toString(); // 매일 변경되는 일일 솔트
        String input = ip + salt;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode()); // 대체 폴백
        }
    }

    private String parseDeviceType(String ua) {
        if (ua == null) return "DESKTOP";
        String lowerUa = ua.toLowerCase();
        if (lowerUa.contains("ipad") || lowerUa.contains("tablet")) {
            return "TABLET";
        }
        if (lowerUa.contains("mobi") || lowerUa.contains("mini") || lowerUa.contains("iphone") || lowerUa.contains("android")) {
            return "MOBILE";
        }
        return "DESKTOP";
    }

    private String parseOsType(String ua) {
        if (ua == null) return "UNKNOWN";
        String lowerUa = ua.toLowerCase();
        if (lowerUa.contains("iphone") || lowerUa.contains("ipad") || lowerUa.contains("ipod")) {
            return "iOS";
        }
        if (lowerUa.contains("android")) {
            return "Android";
        }
        if (lowerUa.contains("windows")) {
            return "Windows";
        }
        if (lowerUa.contains("macintosh") || lowerUa.contains("mac os")) {
            return "macOS";
        }
        if (lowerUa.contains("linux")) {
            return "Linux";
        }
        return "UNKNOWN";
    }

    private String parseReferrerHost(String referrer) {
        if (referrer == null || referrer.trim().isEmpty()) {
            return "DIRECT";
        }
        try {
            // URL 호스트명 추출
            java.net.URI uri = new java.net.URI(referrer);
            String host = uri.getHost();
            if (host != null) {
                // www. 제거 처리
                return host.startsWith("www.") ? host.substring(4) : host;
            }
            return referrer;
        } catch (Exception e) {
            return referrer;
        }
    }
}
