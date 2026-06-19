package com.pixellink.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pixellink.dto.LinkCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class LinkApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createLink_WithPortInUrl_ReturnsCreated() throws Exception {
        LinkCreateRequest request = new LinkCreateRequest();
        request.setDefaultTargetUrl("https://gw.hinetservice.co.kr:5173/appr/appr_read?apprNo=202606160001");

        mockMvc.perform(post("/api/links")
                        .param("userId", "test-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.defaultTargetUrl").value("https://gw.hinetservice.co.kr:5173/appr/appr_read?apprNo=202606160001"));
    }

    @Test
    void createLink_WithInvalidUrl_ReturnsBadRequest() throws Exception {
        LinkCreateRequest request = new LinkCreateRequest();
        request.setDefaultTargetUrl("invalid-url-format");

        mockMvc.perform(post("/api/links")
                        .param("userId", "test-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").value("올바른 URL 형식이어야 합니다."));
    }
}
