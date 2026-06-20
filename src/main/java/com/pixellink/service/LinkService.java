package com.pixellink.service;

import com.pixellink.dto.LinkCreateRequest;
import com.pixellink.dto.LinkResponse;
import com.pixellink.mapper.ClickLogMapper;
import com.pixellink.mapper.LinkMapper;
import com.pixellink.mapper.SystemSettingMapper;
import com.pixellink.mapper.UserMapper;
import com.pixellink.model.Link;
import com.pixellink.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
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

    @Autowired
    private SystemSettingMapper systemSettingMapper;

    @Autowired
    private com.pixellink.mapper.RouteRuleMapper routeRuleMapper;

    @Autowired
    private com.pixellink.mapper.SettlementMapper settlementMapper;

    @Autowired
    private com.pixellink.mapper.PaymentMapper paymentMapper;

    private static final String CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final SecureRandom random = new SecureRandom();


    public List<LinkResponse> getLinksByUserIdPaged(String userId, String search, int page, int pageSize, String baseUrl) {
        int offset = (page - 1) * pageSize;
        List<Link> links = linkMapper.findByUserIdPaged(userId, search, pageSize, offset);
        return links.stream()
                .map(link -> LinkResponse.from(link, baseUrl))
                .collect(java.util.stream.Collectors.toList());
    }

    public int getLinkCountByUserId(String userId, String search) {
        return linkMapper.countByUserId(userId, search);
    }

    @Transactional
    public LinkResponse createLink(LinkCreateRequest request, String userId, String baseUrl) {
        if (userId == null || userId.trim().isEmpty() || "anonymous".equalsIgnoreCase(userId)) {
            userId = "anonymous";
        }
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("존재하지 않는 회원입니다.");
        }

        // 1. 구독 요금제 제약 조건 검증 (01_monetization.md 에 정의됨)
        List<Link> existingLinks = linkMapper.findByUserId(userId);
        validateSubscriptionLimits(user, request, existingLinks.size());

        // 비회원 전용 정책 적용: 커스텀 슬러그 사용 불가 및 만료일 지정
        LocalDateTime expiredAt = null;
        if ("anonymous".equals(userId)) {
            request.setShortCode(null); // 커스텀 슬러그 강제 무효화
            
            com.pixellink.model.SystemSetting setting = systemSettingMapper.findByKey("anon_link_expiry_days");
            int expiryDays = 30;
            if (setting != null) {
                try {
                    expiryDays = Integer.parseInt(setting.getSettingValue());
                } catch (NumberFormatException e) {
                    // 폴백 사용
                }
            }
            expiredAt = LocalDateTime.now().plusDays(expiryDays);
        }

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
        
        // 2~4단계 광고 및 페이월 모니타이제이션 속성 반영
        link.setAdEnabled(request.isAdEnabled());
        link.setAdTimerSeconds(request.getAdTimerSeconds());
        link.setPaywalled(request.isPaywalled());
        link.setPrice(request.getPrice());
        
        link.setClicksCount(0);
        link.setExpiredAt(expiredAt);

        linkMapper.insert(link);

        // 2단계: 스마트 라우팅 규칙 저장
        java.util.List<com.pixellink.model.RouteRule> routeRules = new java.util.ArrayList<>();
        if (request.getRouteRules() != null && !request.getRouteRules().isEmpty()) {
            for (com.pixellink.dto.LinkCreateRequest.RouteRuleRequest rReq : request.getRouteRules()) {
                if (rReq.getRuleType() != null && rReq.getRuleValue() != null && rReq.getTargetUrl() != null) {
                    com.pixellink.model.RouteRule rule = new com.pixellink.model.RouteRule();
                    rule.setId(UUID.randomUUID().toString());
                    rule.setLinkId(link.getId());
                    rule.setRuleType(rReq.getRuleType());
                    rule.setRuleValue(rReq.getRuleValue());
                    rule.setTargetUrl(rReq.getTargetUrl());
                    routeRuleMapper.insert(rule);
                    routeRules.add(rule);
                }
            }
        }

        return LinkResponse.from(link, routeRules, baseUrl);

    }

    private void validateSubscriptionLimits(User user, LinkCreateRequest request, int currentLinkCount) {
        String tier = user.getSubscriptionTier();

        // 1) 생성 가능 링크 수 체크 (익명 비회원은 누적 개수 제한 예외 처리)
        if ("FREE".equals(tier) && !"anonymous".equals(user.getId()) && currentLinkCount >= 3) {
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
                .map(link -> {
                    java.util.List<com.pixellink.model.RouteRule> rules = routeRuleMapper.findByLinkId(link.getId());
                    return LinkResponse.from(link, rules, baseUrl);
                })
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

    @Transactional
    public void updateSystemSetting(String key, String value) {
        if ("anon_link_expiry_days".equals(key)) {
            try {
                int days = Integer.parseInt(value);
                if (days <= 0) {
                    throw new IllegalArgumentException("만료 기간은 1일 이상이어야 합니다.");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("만료 기간은 숫자 형식이어야 합니다.");
            }
        }
        systemSettingMapper.updateValue(key, value);
    }

    public String getSystemSetting(String key, String defaultValue) {
        com.pixellink.model.SystemSetting setting = systemSettingMapper.findByKey(key);
        return setting != null ? setting.getSettingValue() : defaultValue;
    }

    @Transactional
    public void recordAdClick(String linkId, String clientIp) {
        Link link = linkMapper.findById(linkId);
        if (link == null) {
            throw new IllegalArgumentException("존재하지 않는 링크입니다.");
        }

        String ipHash = hashIpAddress(clientIp);
        clickLogMapper.updateAdClicked(linkId, ipHash);

        // 광고 클릭 적립 단가 가져오기
        String adRewardStr = getSystemSetting("ad_reward_per_click", "70");
        int adReward = 70;
        try {
            adReward = Integer.parseInt(adRewardStr);
        } catch (NumberFormatException e) {
            // 기본값 유지
        }

        // 광고 클릭 쉐어액 적재
        com.pixellink.model.Settlement settlement = new com.pixellink.model.Settlement();
        settlement.setId(UUID.randomUUID().toString());
        settlement.setUserId(link.getUserId());
        settlement.setAmount(adReward);
        settlement.setStatus("PENDING");
        settlementMapper.insert(settlement);
    }

    private String hashIpAddress(String ip) {
        if (ip == null) ip = "127.0.0.1";
        String salt = java.time.LocalDate.now().toString(); // 매일 변경되는 일일 솔트
        String input = ip + salt;
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode()); // 대체 폴백
        }
    }

    @Transactional
    public void withdrawSettlements(String userId, String bankName, String accountNumber, String accountHolder) {
        Integer balance = settlementMapper.sumAmountByUserId(userId);
        
        String minWithdrawStr = getSystemSetting("min_withdrawal_amount", "10000");
        int minWithdraw = 10000;
        try {
            minWithdraw = Integer.parseInt(minWithdrawStr);
        } catch (NumberFormatException e) {
            // 기본값 유지
        }
        
        if (balance == null || balance < minWithdraw) {
            throw new IllegalStateException("출금 신청 가능한 정산금이 부족합니다. (최소 " + String.format("%,d", minWithdraw) + "원)");
        }
        settlementMapper.updateStatusByUserId(userId, "COMPLETED");

        // 잔액 차감을 위한 역정산 거래 기입 (관리자 승인 대기 상태 PENDING)
        com.pixellink.model.Settlement balanceReset = new com.pixellink.model.Settlement();
        balanceReset.setId(UUID.randomUUID().toString());
        balanceReset.setUserId(userId);
        balanceReset.setAmount(-balance);
        balanceReset.setStatus("PENDING");
        balanceReset.setBankName(bankName);
        balanceReset.setAccountNumber(accountNumber);
        balanceReset.setAccountHolder(accountHolder);
        settlementMapper.insert(balanceReset);
    }

    @Transactional
    public void confirmPayment(String linkId, String clientIp, int amount) {
        Link link = linkMapper.findById(linkId);
        if (link == null) {
            throw new IllegalArgumentException("존재하지 않는 링크입니다.");
        }

        String ipHash = hashIpAddress(clientIp);
        com.pixellink.model.Payment existing = paymentMapper.findByLinkIdAndIpHash(linkId, ipHash);
        if (existing != null) {
            return;
        }

        com.pixellink.model.Payment payment = new com.pixellink.model.Payment();
        payment.setId(UUID.randomUUID().toString());
        payment.setLinkId(linkId);
        payment.setIpHash(ipHash);
        payment.setAmount(amount);
        paymentMapper.insert(payment);

        // 수수료 5% 제하고 95% 판매 정산금 적재
        int profit = (int) (amount * 0.95);
        com.pixellink.model.Settlement settlement = new com.pixellink.model.Settlement();
        settlement.setId(UUID.randomUUID().toString());
        settlement.setUserId(link.getUserId());
        settlement.setAmount(profit);
        settlement.setStatus("PENDING");
        settlementMapper.insert(settlement);
    }

    public boolean isLinkPaidByClient(String linkId, String clientIp) {
        String ipHash = hashIpAddress(clientIp);
        return paymentMapper.findByLinkIdAndIpHash(linkId, ipHash) != null;
    }

    @Transactional
    public void updateSystemSettings(java.util.Map<String, String> settings) {
        if (settings == null) return;

        // OAuth 활성화 여부 기본값 보완 (체크되지 않으면 false로 세팅)
        String[] oauthKeys = {"oauth_google_enabled", "oauth_facebook_enabled", "oauth_naver_enabled", "oauth_kakao_enabled"};
        for (String k : oauthKeys) {
            if (!settings.containsKey(k)) {
                settings.put(k, "false");
            }
        }

        for (java.util.Map.Entry<String, String> entry : settings.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value == null) continue;
            
            // 검증 로직
            if ("anon_link_expiry_days".equals(key)) {
                try {
                    int days = Integer.parseInt(value);
                    if (days <= 0) {
                        throw new IllegalArgumentException("만료 기간은 1일 이상이어야 합니다.");
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("만료 기간은 숫자 형식이어야 합니다.");
                }
            } else if ("ad_reward_per_click".equals(key)) {
                try {
                    int reward = Integer.parseInt(value);
                    if (reward < 0) {
                        throw new IllegalArgumentException("클릭당 적립 단가는 0원 이상이어야 합니다.");
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("적립 단가는 숫자 형식이어야 합니다.");
                }
            } else if ("min_withdrawal_amount".equals(key)) {
                try {
                    int minAmount = Integer.parseInt(value);
                    if (minAmount < 1000) {
                        throw new IllegalArgumentException("최소 출금 가능액은 1,000원 이상이어야 합니다.");
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("최소 출금 가능액은 숫자 형식이어야 합니다.");
                }
            } else if ("starter_monthly_fee".equals(key) || "premium_monthly_fee".equals(key)) {
                try {
                    int fee = Integer.parseInt(value);
                    if (fee < 0) {
                        throw new IllegalArgumentException("요금은 0원 이상이어야 합니다.");
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("요금은 숫자 형식이어야 합니다.");
                }
            }
            
            systemSettingMapper.updateValue(key, value);
        }
    }
}

