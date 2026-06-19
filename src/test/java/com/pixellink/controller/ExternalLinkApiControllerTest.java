package com.pixellink.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pixellink.dto.ExternalLinkRequest;
import com.pixellink.model.ApiKey;
import com.pixellink.service.ApiKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ExternalLinkApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private ObjectMapper objectMapper;

    private String validApiKey;

    @BeforeEach
    void setUp() {
        // 테스트용 계정인 test-user(초기 DDL 등록 계정)에 대해 API Key 발급
        ApiKey apiKey = apiKeyService.generateApiKey("test-user", "Test Key");
        validApiKey = apiKey.getApiKey();
    }

    @Test
    void createShortUrl_WithoutHeader_Returns401() throws Exception {
        ExternalLinkRequest request = new ExternalLinkRequest();
        request.setDefaultTargetUrl("https://example.com/target");

        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Missing X-API-KEY header"));
    }

    @Test
    void createShortUrl_WithInvalidHeader_Returns401() throws Exception {
        ExternalLinkRequest request = new ExternalLinkRequest();
        request.setDefaultTargetUrl("https://example.com/target");

        mockMvc.perform(post("/api/v1/links")
                        .header("X-API-KEY", "pxl_live_invalidkeytoken123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid or inactive API Key"));
    }

    @Test
    void createShortUrl_WithValidHeader_Returns201() throws Exception {
        ExternalLinkRequest request = new ExternalLinkRequest();
        request.setDefaultTargetUrl("https://example.com/target");
        request.setTitle("Test Title");
        request.setDescription("Test Desc");

        mockMvc.perform(post("/api/v1/links")
                        .header("X-API-KEY", validApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").isNotEmpty())
                .andExpect(jsonPath("$.shortUrl").isNotEmpty())
                .andExpect(jsonPath("$.originalUrl").value("https://example.com/target"));
    }
}
