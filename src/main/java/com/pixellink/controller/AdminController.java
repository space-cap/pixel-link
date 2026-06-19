package com.pixellink.controller;

import com.pixellink.dto.SessionUser;
import com.pixellink.mapper.UserMapper;
import com.pixellink.mapper.LinkMapper;
import com.pixellink.mapper.SettlementMapper;
import com.pixellink.mapper.PaymentMapper;
import com.pixellink.model.User;
import com.pixellink.model.Link;
import com.pixellink.model.Settlement;
import com.pixellink.model.Payment;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/app/admin")
public class AdminController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LinkMapper linkMapper;

    @Autowired
    private SettlementMapper settlementMapper;

    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private HttpSession httpSession;

    @GetMapping("/dashboard")
    public String viewDashboard(Model model) {
        SessionUser sessionUser = (SessionUser) httpSession.getAttribute("user");
        if (sessionUser == null || !"ADMIN".equalsIgnoreCase(sessionUser.getRole())) {
            return "redirect:/app/dashboard";
        }

        // 1. 전체 회원 수 및 상세 분류
        List<User> users = userMapper.findAll();
        long totalUsersCount = users.size();
        long premiumUsersCount = users.stream().filter(u -> "PREMIUM".equalsIgnoreCase(u.getSubscriptionTier())).count();
        long freeUsersCount = totalUsersCount - premiumUsersCount;

        // 2. 전체 링크 수 및 클릭수 합계
        List<Link> links = linkMapper.findAll();
        long totalLinksCount = links.size();
        long totalClicksSum = links.stream().mapToLong(Link::getClicksCount).sum();

        // 3. 정산 대기 건수 및 총액
        List<Settlement> settlements = settlementMapper.findAll();
        long pendingSettlementsCount = settlements.stream().filter(s -> "PENDING".equalsIgnoreCase(s.getStatus())).count();
        long pendingSettlementsAmount = settlements.stream().filter(s -> "PENDING".equalsIgnoreCase(s.getStatus())).mapToLong(Settlement::getAmount).sum();

        // 4. 누적 결제 매출
        List<Payment> payments = paymentMapper.findAll();
        long totalPaymentsAmount = payments.stream().mapToLong(Payment::getAmount).sum();

        // 모델 바인딩
        model.addAttribute("user", sessionUser);
        model.addAttribute("totalUsersCount", totalUsersCount);
        model.addAttribute("premiumUsersCount", premiumUsersCount);
        model.addAttribute("freeUsersCount", freeUsersCount);
        model.addAttribute("totalLinksCount", totalLinksCount);
        model.addAttribute("totalClicksSum", totalClicksSum);
        model.addAttribute("pendingSettlementsCount", pendingSettlementsCount);
        model.addAttribute("pendingSettlementsAmount", pendingSettlementsAmount);
        model.addAttribute("totalPaymentsAmount", totalPaymentsAmount);

        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String viewUsers(Model model) {
        SessionUser sessionUser = (SessionUser) httpSession.getAttribute("user");
        if (sessionUser == null || !"ADMIN".equalsIgnoreCase(sessionUser.getRole())) {
            return "redirect:/app/dashboard";
        }

        List<User> users = userMapper.findAll();
        model.addAttribute("users", users);
        model.addAttribute("user", sessionUser);
        
        return "admin/users";
    }

    @PostMapping("/users/update-role")
    @ResponseBody
    public ResponseEntity<?> updateRole(
            @RequestParam("id") String id,
            @RequestParam("role") String role) {
        SessionUser sessionUser = (SessionUser) httpSession.getAttribute("user");
        if (sessionUser == null || !"ADMIN".equalsIgnoreCase(sessionUser.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied");
        }

        // 본인 계정의 권한은 스스로 바꿀 수 없음 (보안 안전 장치)
        if (sessionUser.getId().equals(id)) {
            return ResponseEntity.badRequest().body("Self-demotion is not allowed");
        }

        // 권한 값 유효성 체크
        if (!"USER".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.badRequest().body("Invalid role value");
        }

        userMapper.updateRole(id, role.toUpperCase());
        return ResponseEntity.ok().body("{\"status\":\"success\"}");
    }

    @GetMapping("/settlements")
    public String viewSettlements(Model model) {
        SessionUser sessionUser = (SessionUser) httpSession.getAttribute("user");
        if (sessionUser == null || !"ADMIN".equalsIgnoreCase(sessionUser.getRole())) {
            return "redirect:/app/dashboard";
        }

        List<Settlement> settlements = settlementMapper.findAll();
        model.addAttribute("settlements", settlements);
        model.addAttribute("user", sessionUser);
        
        return "admin/settlements";
    }

    @PostMapping("/settlements/{id}/approve")
    @ResponseBody
    public ResponseEntity<?> approveSettlement(@PathVariable("id") String id) {
        SessionUser sessionUser = (SessionUser) httpSession.getAttribute("user");
        if (sessionUser == null || !"ADMIN".equalsIgnoreCase(sessionUser.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied");
        }

        settlementMapper.updateStatus(id, "COMPLETED");
        return ResponseEntity.ok().body("{\"status\":\"success\"}");
    }

    @PostMapping("/settlements/{id}/reject")
    @ResponseBody
    public ResponseEntity<?> rejectSettlement(@PathVariable("id") String id) {
        SessionUser sessionUser = (SessionUser) httpSession.getAttribute("user");
        if (sessionUser == null || !"ADMIN".equalsIgnoreCase(sessionUser.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied");
        }

        settlementMapper.updateStatus(id, "REJECTED");
        return ResponseEntity.ok().body("{\"status\":\"success\"}");
    }
}
