package com.srh.cinepass;

import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.entity.TheatreVerification;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.support.TicketTestSupport;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TheatreVerificationSecurityTests extends TicketTestSupport {

    @Test
    void publicCatalogueAndLoginRemainAccessibleWithoutAToken() throws Exception {
        Movie movie = createMovie("Public Movie");
        User owner = createUser("Public Owner", "THEATRE_OWNER");

        mockMvc.perform(get("/api/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Public Movie"));

        mockMvc.perform(get("/api/movies/" + movie.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Public Movie"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + owner.getEmail()
                                + "\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void theatreVerificationSubmissionAndOwnApplicationRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/theatre-verification/my"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/theatre-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pendingApplicationsRequireAdmin() throws Exception {
        User owner = createUser("Pending Owner", "THEATRE_OWNER");
        User customer = createUser("Pending Customer", "USER");
        User admin = createUser("Pending Admin", "ADMIN");
        createTheatreVerification(owner, "PENDING");

        mockMvc.perform(get("/api/theatre-verification/pending"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/theatre-verification/pending")
                        .header("Authorization", bearer(customer)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/theatre-verification/pending")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].theatreName").isNotEmpty());
    }

    @Test
    void approveAndRejectRequireAdmin() throws Exception {
        User owner = createUser("Review Owner", "THEATRE_OWNER");
        User customer = createUser("Review Customer", "USER");
        User admin = createUser("Review Admin", "ADMIN");

        TheatreVerification first = createTheatreVerification(owner, "PENDING");

        mockMvc.perform(put("/api/theatre-verification/" + first.getId() + "/approve"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/theatre-verification/" + first.getId() + "/approve")
                        .header("Authorization", bearer(customer)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/theatre-verification/" + first.getId() + "/approve")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        User secondOwner = createUser("Reject Owner", "THEATRE_OWNER");
        TheatreVerification second = createTheatreVerification(secondOwner, "PENDING");

        mockMvc.perform(put("/api/theatre-verification/" + second.getId() + "/reject")
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Incomplete documents\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/theatre-verification/" + second.getId() + "/reject")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Incomplete documents\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Incomplete documents"));
    }

    @Test
    void verificationResponsesNeverContainPasswordHashes() throws Exception {
        User owner = createUser("Hash Owner", "THEATRE_OWNER");
        User admin = createUser("Hash Admin", "ADMIN");
        createTheatreVerification(owner, "PENDING");

        MvcResult pending = mockMvc.perform(get("/api/theatre-verification/pending")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult own = mockMvc.perform(get("/api/theatre-verification/my")
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andReturn();

        String pendingJson = pending.getResponse().getContentAsString();
        String ownJson = own.getResponse().getContentAsString();

        assertThat(pendingJson).doesNotContain(owner.getPassword());
        assertThat(pendingJson).doesNotContain("\"password\"");
        assertThat(pendingJson).doesNotContain("\"user\"");

        assertThat(ownJson).doesNotContain(owner.getPassword());
        assertThat(ownJson).doesNotContain("\"password\"");
        assertThat(ownJson).doesNotContain("\"user\"");
    }

    @Test
    void publicBrowsingDoesNotLeakPrivateKycFields() throws Exception {
        createMovie("Catalogue Movie");

        MvcResult result = mockMvc.perform(get("/api/movies"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        String json = result.getResponse().getContentAsString();

        assertThat(json).doesNotContain("gstin");
        assertThat(json).doesNotContain("panNumber");
        assertThat(json).doesNotContain("password");
    }

    @Test
    void paymentRecoveryQueueRequiresAdmin() throws Exception {
        User customer = createUser("Recovery Customer", "USER");
        User admin = createUser("Recovery Admin", "ADMIN");

        mockMvc.perform(get("/api/admin/payments/recovery"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/payments/recovery")
                        .header("Authorization", bearer(customer)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/payments/recovery")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
