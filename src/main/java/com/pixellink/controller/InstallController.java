package com.pixellink.controller;

import com.pixellink.mapper.SystemAuditLogMapper;
import com.pixellink.mapper.SystemSettingMapper;
import com.pixellink.mapper.UserMapper;
import com.pixellink.model.SystemAuditLog;
import com.pixellink.model.SystemSetting;
import com.pixellink.model.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
public class InstallController {

    @Autowired
    private SystemSettingMapper systemSettingMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SystemAuditLogMapper systemAuditLogMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/app/install")
    public String showInstall(Model model) {
        if (isInstalled()) {
            return "redirect:/";
        }
        return "install";
    }

    @PostMapping("/app/install/process")
    public String processInstall(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam("name") String name,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "phone", required = false) String phone,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        if (isInstalled()) {
            redirectAttributes.addFlashAttribute("errorMessage", "이미 설치가 완료된 시스템입니다.");
            return "redirect:/";
        }

        if (username == null || username.trim().isEmpty() ||
            password == null || password.trim().isEmpty() ||
            name == null || name.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "필수 항목(아이디, 비밀번호, 이름)을 모두 입력해 주세요.");
            return "redirect:/app/install";
        }

        // 아이디 중복 체크
        User existing = userMapper.findById(username);
        if (existing != null) {
            redirectAttributes.addFlashAttribute("errorMessage", "이미 존재하는 사용자 ID입니다.");
            return "redirect:/app/install";
        }

        // 1. 관리자 유저 등록
        User admin = new User();
        admin.setId(username);
        admin.setName(name);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setEmail(email != null && !email.trim().isEmpty() ? email : null);
        admin.setPhone(phone != null && !phone.trim().isEmpty() ? phone : null);
        admin.setTermsAgreed(true);
        admin.setSubscriptionTier("PREMIUM");
        admin.setRole("ADMIN");
        userMapper.insert(admin);

        // 2. 감사 로그 기록
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        String userAgent = request.getHeader("User-Agent");

        SystemAuditLog auditLog = new SystemAuditLog();
        auditLog.setId(UUID.randomUUID().toString());
        auditLog.setEventType("SYSTEM_INSTALL");
        auditLog.setActorId(username);
        auditLog.setIpAddress(ip);
        auditLog.setUserAgent(userAgent);
        systemAuditLogMapper.insert(auditLog);

        // 3. 설치완료 상태 업데이트
        systemSettingMapper.updateValue("is_installed", "true");

        redirectAttributes.addFlashAttribute("successMessage", "시스템 관리자(ADMIN) 설정이 완료되었습니다! 로그인해 주세요.");
        return "redirect:/app/login?installed=true";
    }

    private boolean isInstalled() {
        SystemSetting setting = systemSettingMapper.findByKey("is_installed");
        return setting != null && "true".equalsIgnoreCase(setting.getSettingValue());
    }
}
