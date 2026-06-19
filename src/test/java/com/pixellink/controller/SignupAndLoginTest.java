package com.pixellink.controller;

import com.pixellink.mapper.UserMapper;
import com.pixellink.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class SignupAndLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void showSignup_ReturnsSignupView() throws Exception {
        mockMvc.perform(get("/app/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"));
    }

    @Test
    void processSignup_Success() throws Exception {
        mockMvc.perform(post("/app/signup/process")
                        .param("username", "company123")
                        .param("password", "securepwd123")
                        .param("name", "(주)픽셀링크")
                        .param("email", "contact@pixel.corp")
                        .param("phone", "02-123-4567")
                        .param("termsAgreed", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/app/login"))
                .andExpect(flash().attributeExists("successMessage"));

        User inserted = userMapper.findById("company123");
        assertNotNull(inserted);
        assertEquals("(주)픽셀링크", inserted.getName());
        assertEquals("contact@pixel.corp", inserted.getEmail());
        assertEquals("02-123-4567", inserted.getPhone());
        assertTrue(inserted.isTermsAgreed());
        assertTrue(passwordEncoder.matches("securepwd123", inserted.getPassword()));
        assertEquals("FREE", inserted.getSubscriptionTier());
        assertEquals("USER", inserted.getRole());
    }

    @Test
    void processSignup_MissingRequiredFields_RedirectsWithErrorMessage() throws Exception {
        mockMvc.perform(post("/app/signup/process")
                        .param("username", "")
                        .param("password", "securepwd123")
                        .param("name", "(주)픽셀링크")
                        .param("termsAgreed", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/app/signup"))
                .andExpect(flash().attribute("errorMessage", "필수 입력 항목(이름, 아이디, 비밀번호)을 채워주세요."));
    }

    @Test
    void processSignup_DuplicateUsername_RedirectsWithErrorMessage() throws Exception {
        // test-user already exists in DB from schema SQL
        mockMvc.perform(post("/app/signup/process")
                        .param("username", "test-user")
                        .param("password", "anotherpwd")
                        .param("name", "다른회사")
                        .param("termsAgreed", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/app/signup"))
                .andExpect(flash().attribute("errorMessage", "이미 사용 중인 아이디입니다."));
    }

    @Test
    void formLogin_Success() throws Exception {
        // Create user
        User user = new User();
        user.setId("corporate");
        user.setName("테스트기업");
        user.setPassword(passwordEncoder.encode("mypassword"));
        user.setSubscriptionTier("FREE");
        user.setRole("USER");
        userMapper.insert(user);

        // Perform login
        mockMvc.perform(formLogin("/app/login/process")
                        .user("username", "corporate")
                        .password("password", "mypassword"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/app/dashboard"))
                .andExpect(authenticated().withUsername("corporate"));
    }

    @Test
    void formLogin_Failure_InvalidPassword() throws Exception {
        // Create user
        User user = new User();
        user.setId("corporate");
        user.setName("테스트기업");
        user.setPassword(passwordEncoder.encode("mypassword"));
        user.setSubscriptionTier("FREE");
        user.setRole("USER");
        userMapper.insert(user);

        // Perform login with wrong password
        mockMvc.perform(formLogin("/app/login/process")
                        .user("username", "corporate")
                        .password("password", "wrongpassword"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/app/login?error=true"))
                .andExpect(unauthenticated());
    }
}
