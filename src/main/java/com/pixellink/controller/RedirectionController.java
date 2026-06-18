package com.pixellink.controller;

import com.pixellink.model.Link;
import com.pixellink.service.RedirectionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class RedirectionController {

    @Autowired
    private RedirectionService redirectionService;

    @GetMapping("/{code}")
    public String handleRedirection(
            @PathVariable("code") String code,
            HttpServletRequest request,
            Model model) {
        
        // 1. 제외할 시스템 엔드포인트 필터링 (정적 리소스 오독 방지)
        if (code.equals("favicon.ico") || code.equals("error") || code.startsWith("api") || code.startsWith("css") || code.startsWith("js")) {
            return "redirect:/";
        }

        // 2. 접속자 정보 추출 (User-Agent, IP, Referer)
        String userAgent = request.getHeader("User-Agent");
        String referrer = request.getHeader("Referer");
        String clientIp = getClientIp(request);

        // 3. 클릭 통계 적재 및 단축 링크 조회
        Link link = redirectionService.getLinkAndRecordClick(code, userAgent, clientIp, referrer);

        if (link == null) {
            // 없는 링크의 경우 메인 대시보드로 리다이렉트
            return "redirect:/?error=link_not_found";
        }

        // 4. 리다이렉션 화면 바인딩
        model.addAttribute("link", link);
        return "redirect";
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 프록시 거쳐 다중 IP 들어올 경우 첫 번째 IP 선택
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
