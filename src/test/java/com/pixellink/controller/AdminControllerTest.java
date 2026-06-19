package com.pixellink.controller;

import com.pixellink.mapper.UserMapper;
import com.pixellink.mapper.SettlementMapper;
import com.pixellink.model.Settlement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SettlementMapper settlementMapper;

    @BeforeEach
    void setUp() {
        userMapper.updateRole("admin", "ADMIN");
        userMapper.updateRole("test-user", "USER");
        userMapper.updateRole("free-user", "USER");
        userMapper.updateRole("anonymous", "USER");
    }

    @Test
    void viewUsers_AsAdmin_ReturnsUsersView() throws Exception {
        mockMvc.perform(get("/app/admin/users")
                        .session(new MockHttpSession())
                        .param("userId", "admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    void viewUsers_AsRegularUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/app/admin/users")
                        .session(new MockHttpSession())
                        .param("userId", "test-user"))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewDashboard_AsAdmin_ReturnsDashboardView() throws Exception {
        mockMvc.perform(get("/app/admin/dashboard")
                        .session(new MockHttpSession())
                        .param("userId", "admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("totalUsersCount"))
                .andExpect(model().attributeExists("premiumUsersCount"))
                .andExpect(model().attributeExists("freeUsersCount"))
                .andExpect(model().attributeExists("totalLinksCount"))
                .andExpect(model().attributeExists("totalClicksSum"))
                .andExpect(model().attributeExists("pendingSettlementsCount"))
                .andExpect(model().attributeExists("pendingSettlementsAmount"))
                .andExpect(model().attributeExists("totalPaymentsAmount"));
    }

    @Test
    void viewDashboard_AsRegularUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/app/admin/dashboard")
                        .session(new MockHttpSession())
                        .param("userId", "test-user"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateRole_AsAdmin_Success() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/app/admin/users/update-role")
                        .session(new MockHttpSession())
                        .param("userId", "admin")
                        .param("id", "test-user")
                        .param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"success\"}"));
    }

    @Test
    void updateRole_AsRegularUser_Forbidden() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/app/admin/users/update-role")
                        .session(new MockHttpSession())
                        .param("userId", "test-user")
                        .param("id", "free-user")
                        .param("role", "ADMIN"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateRole_SelfDemotion_BadRequest() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/app/admin/users/update-role")
                        .session(new MockHttpSession())
                        .param("userId", "admin")
                        .param("id", "admin")
                        .param("role", "USER"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void viewSettlements_AsAdmin_ReturnsView() throws Exception {
        mockMvc.perform(get("/app/admin/settlements")
                        .session(new MockHttpSession())
                        .param("userId", "admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/settlements"))
                .andExpect(model().attributeExists("settlements"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    void viewSettlements_AsRegularUser_Forbidden() throws Exception {
        mockMvc.perform(get("/app/admin/settlements")
                        .session(new MockHttpSession())
                        .param("userId", "test-user"))
                .andExpect(status().isForbidden());
    }

    @Test
    void approveSettlement_AsAdmin_Success() throws Exception {
        Settlement payout = new Settlement();
        String id = UUID.randomUUID().toString();
        payout.setId(id);
        payout.setUserId("test-user");
        payout.setAmount(-15000);
        payout.setStatus("PENDING");
        payout.setBankName("신한은행");
        payout.setAccountNumber("110123456789");
        payout.setAccountHolder("홍길동");
        settlementMapper.insert(payout);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/app/admin/settlements/" + id + "/approve")
                        .session(new MockHttpSession())
                        .param("userId", "admin"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"success\"}"));

        Settlement updated = settlementMapper.findAll().stream()
                .filter(s -> s.getId().equals(id))
                .findFirst().orElse(null);
        org.junit.jupiter.api.Assertions.assertNotNull(updated);
        org.junit.jupiter.api.Assertions.assertEquals("COMPLETED", updated.getStatus());
    }

    @Test
    void rejectSettlement_AsAdmin_Success() throws Exception {
        Settlement payout = new Settlement();
        String id = UUID.randomUUID().toString();
        payout.setId(id);
        payout.setUserId("test-user");
        payout.setAmount(-15000);
        payout.setStatus("PENDING");
        payout.setBankName("신한은행");
        payout.setAccountNumber("110123456789");
        payout.setAccountHolder("홍길동");
        settlementMapper.insert(payout);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/app/admin/settlements/" + id + "/reject")
                        .session(new MockHttpSession())
                        .param("userId", "admin"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"success\"}"));

        Settlement updated = settlementMapper.findAll().stream()
                .filter(s -> s.getId().equals(id))
                .findFirst().orElse(null);
        org.junit.jupiter.api.Assertions.assertNotNull(updated);
        org.junit.jupiter.api.Assertions.assertEquals("REJECTED", updated.getStatus());
    }

    @Test
    void viewSettings_AsAdmin_ReturnsSettingsPage() throws Exception {
        mockMvc.perform(get("/app/admin/settings")
                        .session(new MockHttpSession())
                        .param("userId", "admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/settings"))
                .andExpect(model().attributeExists("settings"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    void viewSettings_AsRegularUser_Forbidden() throws Exception {
        mockMvc.perform(get("/app/admin/settings")
                        .session(new MockHttpSession())
                        .param("userId", "test-user"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateSettings_AsAdmin_Success() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/app/admin/settings/update")
                        .session(new MockHttpSession())
                        .param("userId", "admin")
                        .param("anon_link_expiry_days", "15")
                        .param("ad_reward_per_click", "120")
                        .param("min_withdrawal_amount", "5000")
                        .param("starter_monthly_fee", "9900")
                        .param("premium_monthly_fee", "19900"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/app/admin/settings"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void updateSettings_InvalidValues_ReturnsErrorMessage() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/app/admin/settings/update")
                        .session(new MockHttpSession())
                        .param("userId", "admin")
                        .param("anon_link_expiry_days", "-5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/app/admin/settings"))
                .andExpect(flash().attributeExists("errorMessage"));
    }
}
