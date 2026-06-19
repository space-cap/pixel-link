package com.pixellink.controller;

import com.pixellink.dto.SessionUser;
import com.pixellink.mapper.UserMapper;
import com.pixellink.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
@AutoConfigureMockMvc
public class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    private MockHttpSession getUserSession() {
        User user = userMapper.findById("test-user");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", new SessionUser(user));
        return session;
    }

    @Test
    void showDashboard_ReturnsDashboardView() throws Exception {
        mockMvc.perform(get("/app/dashboard")
                        .session(getUserSession())
                        .with(user("test-user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("currentUser"))
                .andExpect(model().attributeExists("links"))
                .andExpect(model().attributeExists("totalClicks"));
    }

    @Test
    void showMonetization_ReturnsMonetizationView() throws Exception {
        mockMvc.perform(get("/app/monetization")
                        .session(getUserSession())
                        .with(user("test-user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("monetization"))
                .andExpect(model().attributeExists("currentUser"))
                .andExpect(model().attributeExists("settlementBalance"))
                .andExpect(model().attributeExists("settlements"));
    }

    @Test
    void showDeveloper_ReturnsDeveloperView() throws Exception {
        mockMvc.perform(get("/app/developer")
                        .session(getUserSession())
                        .with(user("test-user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("developer"))
                .andExpect(model().attributeExists("currentUser"));
    }

    @Test
    void showLinks_ReturnsLinksView() throws Exception {
        mockMvc.perform(get("/app/links")
                        .session(getUserSession())
                        .with(user("test-user").roles("USER"))
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
                        .session(getUserSession())
                        .with(user("test-user").roles("USER"))
                        .param("search", "promo")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("links"))
                .andExpect(model().attribute("searchKeyword", "promo"));
    }

    @Test
    void showAdvancedCreate_ReturnsCreateView() throws Exception {
        mockMvc.perform(get("/app/create")
                        .session(getUserSession())
                        .with(user("test-user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("create"))
                .andExpect(model().attributeExists("currentUser"))
                .andExpect(model().attributeExists("currentUserId"));
    }
}
