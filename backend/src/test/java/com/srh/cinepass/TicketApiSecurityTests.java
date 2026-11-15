package com.srh.cinepass;

import com.srh.cinepass.entity.Booking;
import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.entity.Show;
import com.srh.cinepass.entity.Ticket;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.support.TicketTestSupport;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TicketApiSecurityTests extends TicketTestSupport {

    private static String verifyBody(String token) {
        return "{\"verificationToken\":\"" + token + "\"}";
    }

    @Test
    void ticketEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/tickets/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        mockMvc.perform(post("/api/staff/tickets/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyBody("a".repeat(43))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerSeesOnlyOwnTicketsAndTheTokenIsNeverSerialized() throws Exception {
        User owner = createUser("Ticket Owner", "USER");
        User other = createUser("Other Customer", "USER");
        Movie movie = createMovie("Blade Runner");
        Show show = createShow(movie, showTimeWithinAdmissionWindow());

        Ticket ownTicket = issueTicket(createConfirmedBooking(owner, show));
        issueTicket(createConfirmedBooking(other, show));

        mockMvc.perform(get("/api/tickets/me").header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ticketId").value(ownTicket.getId()))
                .andExpect(jsonPath("$[0].movieTitle").value("Blade Runner"))
                .andExpect(jsonPath("$[0].ticketReference").isNotEmpty())
                .andExpect(jsonPath("$[0].verificationToken").doesNotExist())
                .andExpect(jsonPath("$[0].bookingId").isNumber())
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void customerCannotReadAnotherCustomersTicket() throws Exception {
        User owner = createUser("Real Owner", "USER");
        User intruder = createUser("Intruder", "USER");
        Movie movie = createMovie("Se7en");
        Show show = createShow(movie, showTimeWithinAdmissionWindow());
        Ticket ticket = issueTicket(createConfirmedBooking(owner, show));

        mockMvc.perform(get("/api/tickets/" + ticket.getId())
                        .header("Authorization", bearer(intruder)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/tickets/" + ticket.getId() + "/qr")
                        .header("Authorization", bearer(intruder)))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerReceivesPngQrCodeAndTokenIsNotInThePayloadText() throws Exception {
        User owner = createUser("Qr Owner", "USER");
        Movie movie = createMovie("Contact");
        Show show = createShow(movie, showTimeWithinAdmissionWindow());
        Ticket ticket = issueTicket(createConfirmedBooking(owner, show));

        MvcResult result = mockMvc.perform(get("/api/tickets/" + ticket.getId() + "/qr")
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn();

        byte[] png = result.getResponse().getContentAsByteArray();

        assertThat(png.length).isGreaterThan(100);
        assertThat(new String(png, java.nio.charset.StandardCharsets.ISO_8859_1))
                .doesNotContain(ticket.getVerificationToken());
    }

    @Test
    void customerCannotCallTheStaffVerificationApi() throws Exception {
        User customer = createUser("Customer Only", "USER");
        Movie movie = createMovie("The Thing");
        Show show = createShow(movie, showTimeWithinAdmissionWindow());
        Ticket ticket = issueTicket(createConfirmedBooking(customer, show));

        mockMvc.perform(post("/api/staff/tickets/verify")
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyBody(ticket.getVerificationToken())))
                .andExpect(status().isForbidden());

        assertThat(ticketRepository.findById(ticket.getId()).orElseThrow().getStatus().name())
                .isEqualTo("BOOKED");
    }

    @Test
    void staffCanVerifyATicketOnlyOnce() throws Exception {
        User customer = createUser("Guest Customer", "USER");
        User staff = createUser("Gate Staff", "STAFF");
        Movie movie = createMovie("Whiplash");
        Show show = createShow(movie, showTimeWithinAdmissionWindow());
        Ticket ticket = issueTicket(createConfirmedBooking(customer, show));

        mockMvc.perform(post("/api/staff/tickets/verify")
                        .header("Authorization", bearer(staff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyBody(ticket.getVerificationToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("VALID"))
                .andExpect(jsonPath("$.message").value("Ticket Verified Successfully"))
                .andExpect(jsonPath("$.ticketReference").value(ticket.getTicketReference()))
                .andExpect(jsonPath("$.customerName").value("Guest Customer"))
                .andExpect(jsonPath("$.verificationToken").doesNotExist());

        mockMvc.perform(post("/api/staff/tickets/verify")
                        .header("Authorization", bearer(staff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyBody(ticket.getVerificationToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("ALREADY_USED"))
                .andExpect(jsonPath("$.message").value("Ticket Already Used"));
    }

    @Test
    void invalidTokenReturnsBadRequestWithInvalidResult() throws Exception {
        User staff = createUser("Invalid Staff", "STAFF");

        mockMvc.perform(post("/api/staff/tickets/verify")
                        .header("Authorization", bearer(staff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyBody("not-a-real-token")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result").value("INVALID"))
                .andExpect(jsonPath("$.message").value("Invalid Ticket"));
    }

    @Test
    void blankTokenIsRejectedByValidation() throws Exception {
        User staff = createUser("Blank Staff", "STAFF");

        mockMvc.perform(post("/api/staff/tickets/verify")
                        .header("Authorization", bearer(staff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verificationToken\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Verification token is required"));
    }

    @Test
    void administratorsCanPromoteACustomerToStaffWithoutPublicSignup() throws Exception {
        User admin = createUser("Admin User", "ADMIN");
        User customer = createUser("Promoted Customer", "USER");

        String body = String.format(
                "{\"name\":\"%s\",\"email\":\"%s\"}",
                customer.getName(),
                customer.getEmail());

        mockMvc.perform(post("/api/admin/staff")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STAFF"))
                .andExpect(jsonPath("$.email").value(customer.getEmail()));

        User promoted = userRepository.findById(customer.getId()).orElseThrow();
        assertThat(promoted.getRole()).isEqualTo("STAFF");

        mockMvc.perform(get("/api/admin/staff")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value(customer.getEmail()));
    }

    @Test
    void nonAdminCannotManageStaffAccounts() throws Exception {
        User staff = createUser("Plain Staff", "STAFF");
        User customer = createUser("Plain Customer", "USER");

        mockMvc.perform(get("/api/admin/staff")
                        .header("Authorization", bearer(staff)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/staff")
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"email\":\"x@example.test\",\"password\":\"Password123!\"}"))
                .andExpect(status().isForbidden());
    }
}
