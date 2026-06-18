package com.pixellink.service;

import com.pixellink.dto.LinkCreateRequest;
import com.pixellink.dto.LinkResponse;
import com.pixellink.mapper.ClickLogMapper;
import com.pixellink.mapper.LinkMapper;
import com.pixellink.mapper.UserMapper;
import com.pixellink.model.Link;
import com.pixellink.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LinkService {

    @Autowired
    private LinkMapper linkMapper;

    @Autowired
    private ClickLogMapper clickLogMapper;

    @Autowired
    private UserMapper userMapper;

    private static final String CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public LinkResponse createLink(LinkCreateRequest request, String userId, String baseUrl) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("존재하지 않는 회원입니다.");
        }

        // 1. 구독 요금제 제약 조건 검증 (01_monetization.md 에 정의됨)
        List<Link> existingLinks = linkMapper.findByUserId(userId);
        validateSubscriptionLimits(user, request, existingLinks.size());

        String shortCode = request.getShortCode();
        if (shortCode == null || shortCode.trim().isEmpty()) {
            shortCode = generateUniqueShortCode();
        } else {
            // 커스텀 슬러그 중복 검사
            Link existing = linkMapper.findByShortCode(shortCode);
            if (existing != null) {
                throw new IllegalStateException("이미 사용 중인 단축 주소입니다. 다른 주소를 입력해주세요.");
            }
        }

        Link link = new Link();
        link.setId(UUID.randomUUID().toString());
        link.setUserId(userId);
        link.setShortCode(shortCode);
        link.setDefaultTargetUrl(request.getDefaultTargetUrl());
        link.setTitle(request.getTitle());
        link.setDescription(request.getDescription());
        link.setOgImage(request.getOgImage());
        link.setFbPixelId(request.getFbPixelId());
        link.setGaTrackingId(request.getGaTrackingId());
        link.setCustomScript(request.getCustomScript());
        link.setAdEnabled(false); // 1단계에서는 광고 미활성
        link.setAdTimerSeconds(request.getAdTimerSeconds());
        link.setPaywalled(false);
        link.setPrice(0);
        link.setClicksCount(0);

        linkMapper.insert(link);

        return LinkResponse.from(link, baseUrl);
    }

    private void validateSubscriptionLimits(User user, LinkCreateRequest request, int currentLinkCount) {
        String tier = user.getSubscriptionTier();

        // 1) 생성 가능 링크 수 체크
        if ("FREE".equals(tier) && currentLinkCount >= 3) {
            throw new IllegalStateException("FREE 등급은 최대 3개의 단축 링크만 생성 가능합니다. Starter 이상 요금제로 업그레이드하세요.");
        }
        if ("STARTER".equals(tier) && currentLinkCount >= 100) {
            throw new IllegalStateException("STARTER 등급은 최대 100개의 단축 링크만 생성 가능합니다. Premium 요금제로 업그레이드하세요.");
        }

        // 2) 픽셀 등록 제약 체크
        boolean hasFb = request.getFbPixelId() != null && !request.getFbPixelId().trim().isEmpty();
        boolean hasGa = request.getGaTrackingId() != null && !request.getGaTrackingId().trim().isEmpty();
        boolean hasCustom = request.getCustomScript() != null && !request.getCustomScript().trim().isEmpty();

        if ("FREE".equals(tier) && (hasFb || hasGa || hasCustom)) {
            throw new IllegalStateException("FREE 등급은 마케팅 픽셀 및 커스텀 스크립트를 삽입할 수 없습니다. Starter 이상 요금제로 업그레이드하세요.");
        }

        if ("STARTER".equals(tier)) {
            int pixelCount = 0;
            if (hasFb) pixelCount++;
            if (hasGa) pixelCount++;
            if (pixelCount > 2) {
                throw new IllegalStateException("STARTER 등급은 최대 2개의 마케팅 픽셀만 사용 가능합니다. Premium 요금제로 업그레이드하세요.");
            }
            if (hasCustom) {
                throw new IllegalStateException("STARTER 등급은 사용자 지정 스크립트(customScript)를 삽입할 수 없습니다. Premium 요금제로 업그레이드하세요.");
            }
        }
    }

    public List<LinkResponse> getLinksByUserId(String userId, String baseUrl) {
        return linkMapper.findByUserId(userId).stream()
                .map(link -> LinkResponse.from(link, baseUrl))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteLink(String id, String userId) {
        Link link = linkMapper.findById(id);
        if (link == null) {
            throw new IllegalArgumentException("존재하지 않는 링크입니다.");
        }
        if (!link.getUserId().equals(userId)) {
            throw new SecurityException("해당 링크를 삭제할 권한이 없습니다.");
        }
        linkMapper.delete(id);
    }

    public Map<String, Object> getLinkStats(String id, String userId) {
        Link link = linkMapper.findById(id);
        if (link == null) {
            throw new IllegalArgumentException("존재하지 않는 링크입니다.");
        }
        if (!link.getUserId().equals(userId)) {
            throw new SecurityException("통계 조회 권한이 없습니다.");
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalClicks", clickLogMapper.countByLinkId(id));
        stats.put("devices", mapListToMap(clickLogMapper.getClicksByDevice(id)));
        stats.put("operatingSystems", mapListToMap(clickLogMapper.getClicksByOs(id)));
        stats.put("referrers", mapListToMap(clickLogMapper.getClicksByReferrer(id)));
        
        // 일별 클릭 통계 포맷팅
        List<Map<String, Object>> dailyList = clickLogMapper.getDailyClicks(id);
        List<Map<String, Object>> formattedDaily = dailyList.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("date", m.get("date"));
            // SQLite/PostgreSQL 카운트 타입 호환성 처리
            map.put("clicks", m.get("clicks") != null ? ((Number) m.get("clicks")).intValue() : 0);
            return map;
        }).collect(Collectors.toList());
        stats.put("recentDailyClicks", formattedDaily);

        return stats;
    }

    private Map<String, Integer> mapListToMap(List<Map<String, Object>> list) {
        Map<String, Integer> result = new HashMap<>();
        for (Map<String, Object> m : list) {
            String name = (String) m.get("name");
            Number value = (Number) m.get("value");
            result.put(name != null ? name : "UNKNOWN", value != null ? value.intValue() : 0);
        }
        return result;
    }

    private String generateUniqueShortCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
            }
            String code = sb.toString();
            if (linkMapper.findByShortCode(code) == null) {
                return code;
            }
        }
        throw new IllegalStateException("단축 코드 생성에 실패했습니다. 다시 시도해 주세요.");
    }
}
