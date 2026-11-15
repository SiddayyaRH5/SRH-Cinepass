package com.srh.cinepass;

import com.srh.cinepass.entity.Location;
import com.srh.cinepass.entity.Theatre;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.support.TicketTestSupport;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TheatreAuthorizationTests extends TicketTestSupport {

    @Test
    void publicTheatreCatalogueDoesNotLeakOwnerOrPrivateFields() throws Exception {
        User owner = createUser("Catalogue Owner", "THEATRE_OWNER");
        createTheatre(owner, "Public Theatre");

        MvcResult result = mockMvc.perform(get("/api/theatres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Public Theatre"))
                .andExpect(jsonPath("$[0].location.city").isNotEmpty())
                .andReturn();

        String json = result.getResponse().getContentAsString();

        assertThat(json).doesNotContain("\"owner\"");
        assertThat(json).doesNotContain("\"email\"");
        assertThat(json).doesNotContain("\"password\"");
        assertThat(json).doesNotContain("gstin");
        assertThat(json).doesNotContain("panNumber");
    }

    @Test
    void customerCannotCreateUpdateOrDeleteATheatre() throws Exception {
        User owner = createUser("Target Owner", "THEATRE_OWNER");
        User customer = createUser("Theatre Customer", "USER");
        Theatre theatre = createTheatre(owner, "Target Theatre");

        mockMvc.perform(post("/api/theatres")
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(theatrePayload("Fake Theatre", theatre.getLocation().getId())))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/theatres/" + theatre.getId())
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(theatrePayload("Hijacked Theatre", theatre.getLocation().getId())))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/theatres/" + theatre.getId())
                        .header("Authorization", bearer(customer)))
                .andExpect(status().isForbidden());

        assertThat(theatreRepository.findById(theatre.getId())).isPresent();
    }

    @Test
    void staffCannotManageTheatres() throws Exception {
        User owner = createUser("Staff Target Owner", "THEATRE_OWNER");
        User staff = createUser("Theatre Staff", "STAFF");
        Theatre theatre = createTheatre(owner, "Staff Target Theatre");

        mockMvc.perform(put("/api/theatres/" + theatre.getId())
                        .header("Authorization", bearer(staff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(theatrePayload("Staff Hijack", theatre.getLocation().getId())))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/theatres/" + theatre.getId())
                        .header("Authorization", bearer(staff)))
                .andExpect(status().isForbidden());
    }

    @Test
    void approvedOwnerCanManageOwnTheatreButNotAnotherOwnersTheatre() throws Exception {
        User owner = createUser("Approved Owner", "THEATRE_OWNER");
        createTheatreVerification(owner, "APPROVED");
        Theatre own = createTheatre(owner, "Own Theatre");

        User otherOwner = createUser("Other Approved Owner", "THEATRE_OWNER");
        createTheatreVerification(otherOwner, "APPROVED");
        Theatre other = createTheatre(otherOwner, "Other Theatre");

        mockMvc.perform(put("/api/theatres/" + own.getId())
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(theatrePayload("Own Theatre Renamed", own.getLocation().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Own Theatre Renamed"));

        mockMvc.perform(put("/api/theatres/" + other.getId())
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(theatrePayload("Attempted Hijack", other.getLocation().getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void unapprovedTheatreOwnerCannotCreateATheatre() throws Exception {
        User owner = createUser("Unapproved Owner", "THEATRE_OWNER");
        Location location = createLocation("Unapproved City");

        mockMvc.perform(post("/api/theatres")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(theatrePayload("Unapproved Theatre", location.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateUpdateAndDeleteTheatres() throws Exception {
        User admin = createUser("Theatre Admin", "ADMIN");
        Location location = createLocation("Admin City");

        mockMvc.perform(post("/api/theatres")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(theatrePayload("Admin Theatre", location.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Admin Theatre"))
                .andExpect(jsonPath("$.owner").doesNotExist());

        Theatre created = theatreRepository.findAll()
                .stream()
                .filter(theatre -> "Admin Theatre".equals(theatre.getName()))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(put("/api/theatres/" + created.getId())
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(theatrePayload("Admin Theatre Renamed", location.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Admin Theatre Renamed"));

        mockMvc.perform(delete("/api/theatres/" + created.getId())
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());

        assertThat(theatreRepository.findById(created.getId())).isEmpty();
    }

    @Test
    void unknownRouteIsDeniedForAuthenticatedUsers() throws Exception {
        User customer = createUser("Unknown Route Customer", "USER");

        mockMvc.perform(get("/api/not-a-real-endpoint")
                        .header("Authorization", bearer(customer)))
                .andExpect(status().isForbidden());
    }

    private String theatrePayload(String name, Long locationId) {
        return "{\"name\":\"" + name
                + "\",\"totalSeats\":100,\"location\":{\"id\":"
                + locationId + "}}";
    }
}
