package com.pixellink.controller;

import com.pixellink.dto.LinkResponse;
import com.pixellink.dto.SessionUser;
import com.pixellink.mapper.UserMapper;
import com.pixellink.model.User;
import com.pixellink.service.LinkService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @GetMapping("/login")
    public String showLogin() {
        return "login";
    }

    @GetMapping("/app/dashboard")
    public String showDashboard(
            @AuthenticationPrincipal SessionUser sessionUser,
            HttpServletRequest request,
            Model model) {
        
        String userId = (sessionUser != null) ? sessionUser.getId() : "test-user";
        // 1. 유저 조회 (없으면 기본값 test-user 강제)
        User user = userMapper.findById(userId);
        if (user == null) {
            user = userMapper.findById("test-user");
            userId = "test-user";
        }

        // 2. 동적 호스트명(Base URL) 추출
        String baseUrl = getBaseUrl(request);

        // 3. 유저의 전체 단축 링크 정보 조회 (통계 요약용)
        List<LinkResponse> allLinks = linkService.getLinksByUserId(userId, baseUrl);
        int totalClicks = allLinks.stream().mapToInt(LinkResponse::getClicksCount).sum();

        // 4. 대시보드 리스트 출력용 최신 5개 퀵뷰 링크 목록 조회
        List<LinkResponse> quickLinks = linkService.getLinksByUserIdPaged(userId, null, 1, 5, baseUrl);

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
        model.addAttribute("links", quickLinks); // 대시보드 리스트에는 퀵뷰 5개만 노출
        model.addAttribute("totalClicks", totalClicks);
        model.addAttribute("mockUsers", mockUsers);
        model.addAttribute("currentUserId", userId);
        model.addAttribute("anonLinkExpiryDays", expiryDays);
        model.addAttribute("settlementBalance", settlementBalance);

        return "dashboard";
    }

    @GetMapping("/app/create")
    public String showAdvancedCreate(
            @AuthenticationPrincipal SessionUser sessionUser,
            Model model) {
        
        String userId = (sessionUser != null) ? sessionUser.getId() : "test-user";
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

        return "create";
    }

    @GetMapping("/app/links")
    public String showLinks(
            @AuthenticationPrincipal SessionUser sessionUser,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            HttpServletRequest request,
            Model model) {
        
        String userId = (sessionUser != null) ? sessionUser.getId() : "test-user";
        User user = userMapper.findById(userId);
        if (user == null) {
            user = userMapper.findById("test-user");
            userId = "test-user";
        }

        String baseUrl = getBaseUrl(request);

        // 검색 필터 및 페이징 적용된 목록 조회
        List<LinkResponse> links = linkService.getLinksByUserIdPaged(userId, search, page, pageSize, baseUrl);
        int totalCount = linkService.getLinkCountByUserId(userId, search);
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);
        if (totalPages == 0) {
            totalPages = 1;
        }

        List<User> mockUsers = Arrays.asList(
            userMapper.findById("admin"),
            userMapper.findById("test-user"),
            userMapper.findById("free-user")
        );

        model.addAttribute("currentUser", user);
        model.addAttribute("links", links);
        model.addAttribute("mockUsers", mockUsers);
        model.addAttribute("currentUserId", userId);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("searchKeyword", search != null ? search : "");

        return "links";
    }

    @GetMapping("/app/monetization")
    public String showMonetization(
            @AuthenticationPrincipal SessionUser sessionUser,
            Model model) {
        
        String userId = (sessionUser != null) ? sessionUser.getId() : "test-user";
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

    @GetMapping("/app/developer")
    public String showDeveloper(
            @AuthenticationPrincipal SessionUser sessionUser,
            Model model) {
        
        String userId = (sessionUser != null) ? sessionUser.getId() : "test-user";
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
