package com.pixellink.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void showDashboard_ReturnsDashboardView() throws Exception {
        mockMvc.perform(get("/app/dashboard")
                        .param("userId", "test-user"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("currentUser"))
                .andExpect(model().attributeExists("links"))
                .andExpect(model().attributeExists("totalClicks"))
                .andExpect(model().attributeExists("mockUsers"));
    }

    @Test
    void showMonetization_ReturnsMonetizationView() throws Exception {
        mockMvc.perform(get("/app/monetization")
                        .param("userId", "test-user"))
                .andExpect(status().isOk())
                .andExpect(view().name("monetization"))
                .andExpect(model().attributeExists("currentUser"))
                .andExpect(model().attributeExists("settlementBalance"))
                .andExpect(model().attributeExists("settlements"));
    }

    @Test
    void showDeveloper_ReturnsDeveloperView() throws Exception {
        mockMvc.perform(get("/app/developer")
                        .param("userId", "test-user"))
                .andExpect(status().isOk())
                .andExpect(view().name("developer"))
                .andExpect(model().attributeExists("currentUser"))
                .andExpect(model().attributeExists("mockUsers"));
    }

    @Test
    void showLinks_ReturnsLinksView() throws Exception {
        mockMvc.perform(get("/app/links")
                        .param("userId", "test-user")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("links"))
                .andExpect(model().attributeExists("currentUser"))
                .andExpect(model().attributeExists("links"))
                .andExpect(model().attributeExists("currentPage"))
                .andExpect(model().attributeExists("totalPages"))
                .andExpect(model().attributeExists("searchKeyword"));
    }

    @Test
    void showLinks_WithSearch_ReturnsLinksView() throws Exception {
        mockMvc.perform(get("/app/links")
                        .param("userId", "test-user")
                        .param("search", "promo")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("links"))
                .andExpect(model().attribute("searchKeyword", "promo"));
    }

    @Test
    void showAdvancedCreate_ReturnsCreateView() throws Exception {
        mockMvc.perform(get("/app/create")
                        .param("userId", "test-user"))
                .andExpect(status().isOk())
                .andExpect(view().name("create"))
                .andExpect(model().attributeExists("currentUser"))
                .andExpect(model().attributeExists("currentUserId"))
                .andExpect(model().attributeExists("mockUsers"));
    }
}
