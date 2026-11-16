package com.srh.cinepass;

import com.srh.cinepass.entity.User;
import com.srh.cinepass.support.TicketTestSupport;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Focused tests for the automation configuration endpoint and the manual
 * automation safety gate.
 */
class AutomationConfigAndSafetyTests extends TicketTestSupport {

    @Test
    void theConfigEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/automation/config"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void theConfigEndpointRequiresTheAdminRole() throws Exception {
        User customer = createUser("Config Customer", "USER");

        mockMvc.perform(get("/api/admin/automation/config")
                        .header("Authorization", bearer(customer)))
                .andExpect(status().isForbidden());
    }

    @Test
    void theConfigEndpointReturnsEffectiveConfigurationWithoutSecrets() throws Exception {
        User admin = createUser("Config Admin", "ADMIN");

        mockMvc.perform(get("/api/admin/automation/config")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schedulerEnabled").value(false))
                .andExpect(jsonPath("$.cleanupEnabled").value(false))
                .andExpect(jsonPath("$.tmdbSyncEnabled").value(true))
                .andExpect(jsonPath("$.retirementEnabled").value(true))
                .andExpect(jsonPath("$.ticketBackfillEnabled").value(false))
                .andExpect(jsonPath("$.horizonDays").value(0))
                .andExpect(jsonPath("$.timezone").value("Asia/Kolkata"))
                // No secrets are ever returned.
                .andExpect(jsonPath("$.tmdbAccessToken").doesNotExist())
                .andExpect(jsonPath("$.tmdbSyncKey").doesNotExist())
                .andExpect(jsonPath("$.jwtSecret").doesNotExist())
                .andExpect(jsonPath("$.dbPassword").doesNotExist())
                .andExpect(jsonPath("$.razorpayKeySecret").doesNotExist());
    }

    @Test
    void manualAutomationRunIsSkippedWhenSchedulerIsDisabled() throws Exception {
        User admin = createUser("Safety Admin", "ADMIN");

        mockMvc.perform(post("/api/admin/automation/run")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("SKIPPED"))
                .andExpect(jsonPath("$.message").value("scheduler disabled"));
    }
}
