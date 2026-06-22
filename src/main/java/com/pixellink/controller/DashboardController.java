package com.pixellink.controller;

import com.pixellink.dto.LinkResponse;
import com.pixellink.dto.SessionUser;
import com.pixellink.mapper.UserMapper;
import com.pixellink.model.User;
import com.pixellink.service.LinkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Controller
public class DashboardController {

    @Autowired
    private LinkService linkService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private com.pixellink.mapper.SettlementMapper settlementMapper;

    @Autowired
    private HttpSession httpSession;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    @GetMapping("/")
    public String showLanding(HttpServletRequest request, Model model) {
        return "landing";
    }

    @GetMapping("/app/login")
    public String showLogin(Model model) {
        model.addAttribute("activeProfile", activeProfile);
        
        // OAuth 활성화 여부 조회 (기본값 true)
        model.addAttribute("googleEnabled", "true".equals(linkService.getSystemSetting("oauth_google_enabled", "true")));
        model.addAttribute("facebookEnabled", "true".equals(linkService.getSystemSetting("oauth_facebook_enabled", "true")));
        model.addAttribute("naverEnabled", "true".equals(linkService.getSystemSetting("oauth_naver_enabled", "true")));
        model.addAttribute("kakaoEnabled", "true".equals(linkService.getSystemSetting("oauth_kakao_enabled", "true")));

        return "login";
    }

    @GetMapping("/app/signup")
    public String showSignup() {
        return "signup";
    }

    @PostMapping("/app/signup/process")
    public String processSignup(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam("name") String name,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "termsAgreed", defaultValue = "false") boolean termsAgreed,
            RedirectAttributes redirectAttributes) {

        if (username == null || username.trim().isEmpty() ||
            password == null || password.trim().isEmpty() ||
            name == null || name.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "필수 입력 항목(이름, 아이디, 비밀번호)을 채워주세요.");
            return "redirect:/app/signup";
        }

        if (!termsAgreed) {
            redirectAttributes.addFlashAttribute("errorMessage", "이용약관 및 개인정보 처리방침에 동의해야 합니다.");
            return "redirect:/app/signup";
        }

        User existing = userMapper.findById(username);
        if (existing != null) {
            redirectAttributes.addFlashAttribute("errorMessage", "이미 사용 중인 아이디입니다.");
            return "redirect:/app/signup";
        }

        User user = new User();
        user.setId(username);
        user.setName(name);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email != null && !email.trim().isEmpty() ? email : null);
        user.setPhone(phone != null && !phone.trim().isEmpty() ? phone : null);
        user.setTermsAgreed(true);
        user.setSubscriptionTier("FREE");
        user.setRole("USER");

        userMapper.insert(user);

        redirectAttributes.addFlashAttribute("successMessage", "회원가입이 완료되었습니다. 로그인해주세요.");
        return "redirect:/app/login";
    }


    @GetMapping("/app/dashboard")
    public String showDashboard(
            HttpServletRequest request,
            Model model) {
        
        SessionUser sessionUser = (SessionUser) httpSession.getAttribute("user");
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

        // 5. 관리자 설정용 비회원 만료일 조회
        String expiryDays = linkService.getSystemSetting("anon_link_expiry_days", "30");

        // 6. 정산금 잔액 조회
        Integer settlementBalance = settlementMapper.sumAmountByUserId(userId);
        if (settlementBalance == null) {
            settlementBalance = 0;
        }

        model.addAttribute("currentUser", user);
        model.addAttribute("links", quickLinks); // 대시보드 리스트에는 퀵뷰 5개만 노출
        model.addAttribute("totalClicks", totalClicks);
        model.addAttribute("currentUserId", userId);
        model.addAttribute("anonLinkExpiryDays", expiryDays);
        model.addAttribute("settlementBalance", settlementBalance);

        return "dashboard";
    }

    @GetMapping("/app/create")
    public String showAdvancedCreate(
            Model model) {
        
        SessionUser sessionUser = (SessionUser) httpSession.getAttribute("user");
        String userId = (sessionUser != null) ? sessionUser.getId() : "test-user";
        User user = userMapper.findById(userId);
        if (user == null) {
            user = userMapper.findById("test-user");
            userId = "test-user";
        }

        model.addAttribute("currentUser", user);
        model.addAttribute("currentUserId", userId);

        return "create";
    }

    @GetMapping("/app/links")
    public String showLinks(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            HttpServletRequest request,
            Model model) {
        
        SessionUser sessionUser = (SessionUser) httpSession.getAttribute("user");
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

        model.addAttribute("currentUser", user);
        model.addAttribute("links", links);
        model.addAttribute("currentUserId", userId);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("searchKeyword", search != null ? search : "");

        return "links";
    }

    @GetMapping("/app/monetization")
    public String showMonetization(
            Model model) {
        
        SessionUser sessionUser = (SessionUser) httpSession.getAttribute("user");
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

        model.addAttribute("currentUser", user);
        model.addAttribute("currentUserId", userId);
        model.addAttribute("settlementBalance", settlementBalance);
        model.addAttribute("settlements", settlements);

        return "monetization";
    }

    @GetMapping("/app/developer")
    public String showDeveloper(
            Model model) {
        
        SessionUser sessionUser = (SessionUser) httpSession.getAttribute("user");
        String userId = (sessionUser != null) ? sessionUser.getId() : "test-user";
        User user = userMapper.findById(userId);
        if (user == null) {
            user = userMapper.findById("test-user");
            userId = "test-user";
        }

        model.addAttribute("currentUser", user);
        model.addAttribute("currentUserId", userId);

        return "developer";
    }

    @GetMapping("/info/faq")
    public String showFaq(HttpServletRequest request, Model model) {
        SessionUser sessionUser = (SessionUser) httpSession.getAttribute("user");
        if (sessionUser != null) {
            User user = userMapper.findById(sessionUser.getId());
            model.addAttribute("currentUser", user);
            model.addAttribute("currentUserId", sessionUser.getId());
        }
        return "faq";
    }

    @GetMapping("/info/privacy")
    public String showPrivacy(HttpServletRequest request, Model model) {
        SessionUser sessionUser = (SessionUser) httpSession.getAttribute("user");
        if (sessionUser != null) {
            User user = userMapper.findById(sessionUser.getId());
            model.addAttribute("currentUser", user);
            model.addAttribute("currentUserId", sessionUser.getId());
        }
        return "privacy";
    }

    @GetMapping("/info/terms")
    public String showTerms(HttpServletRequest request, Model model) {
        SessionUser sessionUser = (SessionUser) httpSession.getAttribute("user");
        if (sessionUser != null) {
            User user = userMapper.findById(sessionUser.getId());
            model.addAttribute("currentUser", user);
            model.addAttribute("currentUserId", sessionUser.getId());
        }
        return "terms";
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
