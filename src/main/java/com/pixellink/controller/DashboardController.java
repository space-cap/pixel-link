package com.pixellink.controller;

import com.pixellink.dto.LinkResponse;
import com.pixellink.mapper.UserMapper;
import com.pixellink.model.User;
import com.pixellink.service.LinkService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.List;

@Controller
public class DashboardController {

    @Autowired
    private LinkService linkService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private com.pixellink.mapper.SettlementMapper settlementMapper;

    @GetMapping("/")
    public String showLanding(HttpServletRequest request, Model model) {
        return "landing";
    }

    @GetMapping("/dashboard")
    public String showDashboard(
            @RequestParam(value = "userId", defaultValue = "test-user") String userId,
            HttpServletRequest request,
            Model model) {
        
        // 1. 유저 조회 (없으면 기본값 test-user 강제)
        User user = userMapper.findById(userId);
        if (user == null) {
            user = userMapper.findById("test-user");
            userId = "test-user";
        }

        // 2. 동적 호스트명(Base URL) 추출
        String baseUrl = getBaseUrl(request);

        // 3. 유저의 단축 링크 목록 조회
        List<LinkResponse> links = linkService.getLinksByUserId(userId, baseUrl);

        // 4. 대시보드 통계 요약 (전체 클릭 수 합산)
        int totalClicks = links.stream().mapToInt(LinkResponse::getClicksCount).sum();

        // 5. 목 테스트 유저 목록 (대시보드 상단 전환기 제공용)
        List<User> mockUsers = Arrays.asList(
            userMapper.findById("admin"),
            userMapper.findById("test-user"),
            userMapper.findById("free-user")
        );

        // 6. 관리자 설정용 비회원 만료일 조회
        String expiryDays = linkService.getSystemSetting("anon_link_expiry_days", "30");

        // 7. 정산금 잔액 조회
        Integer settlementBalance = settlementMapper.sumAmountByUserId(userId);
        if (settlementBalance == null) {
            settlementBalance = 0;
        }

        model.addAttribute("currentUser", user);
        model.addAttribute("links", links);
        model.addAttribute("totalClicks", totalClicks);
        model.addAttribute("mockUsers", mockUsers);
        model.addAttribute("currentUserId", userId);
        model.addAttribute("anonLinkExpiryDays", expiryDays);
        model.addAttribute("settlementBalance", settlementBalance);


        return "dashboard";
    }

    @GetMapping("/dashboard/monetization")
    public String showMonetization(
            @RequestParam(value = "userId", defaultValue = "test-user") String userId,
            Model model) {
        
        User user = userMapper.findById(userId);
        if (user == null) {
            user = userMapper.findById("test-user");
            userId = "test-user";
        }

        Integer settlementBalance = settlementMapper.sumAmountByUserId(userId);
        if (settlementBalance == null) {
            settlementBalance = 0;
        }

        List<com.pixellink.model.Settlement> settlements = settlementMapper.findByUserId(userId);

        List<User> mockUsers = Arrays.asList(
            userMapper.findById("admin"),
            userMapper.findById("test-user"),
            userMapper.findById("free-user")
        );

        model.addAttribute("currentUser", user);
        model.addAttribute("currentUserId", userId);
        model.addAttribute("settlementBalance", settlementBalance);
        model.addAttribute("settlements", settlements);
        model.addAttribute("mockUsers", mockUsers);

        return "monetization";
    }

    @GetMapping("/dashboard/developer")
    public String showDeveloper(
            @RequestParam(value = "userId", defaultValue = "test-user") String userId,
            Model model) {
        
        User user = userMapper.findById(userId);
        if (user == null) {
            user = userMapper.findById("test-user");
            userId = "test-user";
        }

        List<User> mockUsers = Arrays.asList(
            userMapper.findById("admin"),
            userMapper.findById("test-user"),
            userMapper.findById("free-user")
        );

        model.addAttribute("currentUser", user);
        model.addAttribute("currentUserId", userId);
        model.addAttribute("mockUsers", mockUsers);

        return "developer";
    }

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme(); // http or https
        String serverName = request.getServerName(); // localhost or domain
        int serverPort = request.getServerPort(); // 8080 or 80/443

        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);

        if (("http".equals(scheme) && serverPort != 80) || ("https".equals(scheme) && serverPort != 443)) {
            url.append(":").append(serverPort);
        }
        return url.toString();
    }
}
