package com.pixellink.config;

import com.pixellink.dto.SessionUser;
import com.pixellink.mapper.UserMapper;
import com.pixellink.model.User;
import com.pixellink.service.CustomOAuth2UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private HttpSession httpSession;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.disable())) // SQLite 또는 h2 콘솔 접근 대비
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                    "/", 
                    "/login", 
                    "/css/**", 
                    "/js/**", 
                    "/images/**", 
                    "/favicon.ico", 
                    "/monetization/**", 
                    "/api/links", 
                    "/api/links/*/ad-click", 
                    "/api/links/*/payments/confirm",
                    "/info/faq",
                    "/info/privacy",
                    "/info/terms",
                    "/sitemap.xml",
                    "/robots.txt"
                ).permitAll()
                // 로그인, 회원가입 및 설치 페이지는 누구나 접근 가능 (보호막에 걸리기 전에 통과)
                .requestMatchers("/app/login", "/app/signup", "/app/signup/process", "/app/install", "/app/install/process").permitAll()
                // 관리자 경로는 ADMIN 권한 필요
                .requestMatchers("/app/admin/**").hasRole("ADMIN")
                // 대시보드는 무조건 보호
                .requestMatchers("/app", "/app/**").authenticated()
                // 단축 주소 리다이렉트 패턴 허용 (알파뉴메릭 1자 이상)
                .requestMatchers("/{shortCode:[a-zA-Z0-9]+}").permitAll()
                // 나머지 요청들은 permitAll 또는 컨트롤러에서 추가 검증 수행
                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .loginPage("/app/login")
                .loginProcessingUrl("/app/login/process")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler((request, response, authentication) -> {
                    Object principal = authentication.getPrincipal();
                    if (principal instanceof CustomUserDetails) {
                        User user = ((CustomUserDetails) principal).getUser();
                        request.getSession().setAttribute("user", new SessionUser(user));
                    }
                    response.sendRedirect("/app/dashboard");
                })
                .failureUrl("/app/login?error=true")
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/app/login")
                .defaultSuccessUrl("/app/dashboard", true)
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            );

        return http.build();
    }
}
