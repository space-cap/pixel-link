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
                    "/api/links/*/payments/confirm"
                ).permitAll()
                // 로그인 페이지는 누구나 접근 가능 (보호막에 걸리기 전에 통과)
                .requestMatchers("/app/login", "/app/login/mock").permitAll()
                // 관리자 경로는 ADMIN 권한 필요
                .requestMatchers("/app/admin/**").hasRole("ADMIN")
                // 대시보드는 무조건 보호
                .requestMatchers("/app", "/app/**").authenticated()
                // 단축 주소 리다이렉트 패턴 허용 (알파뉴메릭 1자 이상)
                .requestMatchers("/{shortCode:[a-zA-Z0-9]+}").permitAll()
                // 나머지 요청들은 permitAll 또는 컨트롤러에서 추가 검증 수행
                .anyRequest().permitAll()
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

        // 로컬/테스트 환경에서 ?userId=xxx 파라미터를 넘겼을 때 로그인 세션을 자동 구성해 주는 필터 탑재
        http.addFilterBefore(new LocalMockUserFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 로컬 개발 프로파일에서 간편하게 ?userId= 드롭다운 전환을 사용할 수 있도록 지원하는 헬퍼 필터
     */
    private class LocalMockUserFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            String userIdParam = request.getParameter("userId");
            if ("local".equals(activeProfile) && userIdParam != null && !userIdParam.trim().isEmpty()) {
                User user = userMapper.findById(userIdParam);
                if (user != null) {
                    // 세션 정보 강제 주입
                    SessionUser sessionUser = new SessionUser(user);
                    httpSession.setAttribute("user", sessionUser);

                    // Security Context 인증 강제 주입
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole());
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            sessionUser, null, Collections.singletonList(authority));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    httpSession.removeAttribute("user");
                    SecurityContextHolder.clearContext();
                }
            } else {
                // 파라미터가 없더라도 세션에 "user"가 존재한다면 시큐리티 컨텍스트에 바인딩 유지
                SessionUser sessionUser = (SessionUser) httpSession.getAttribute("user");
                if (sessionUser != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + sessionUser.getRole());
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            sessionUser, null, Collections.singletonList(authority));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
            filterChain.doFilter(request, response);
        }
    }
}
