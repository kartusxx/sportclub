package ru.teamname.sportclub.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import ru.teamname.sportclub.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MemberApiIntegrationTest extends AbstractIntegrationTest {

    private static String memberJson(String email, String status) {
        return """
                {"firstName":"Ivan","lastName":"Petrov","email":"%s","phone":"+7 900 123-45-67",
                 "birthDate":"1995-05-20","status":"%s"}
                """.formatted(email, status);
    }

    @Test
    void createReturns201WithLocationAndBody() throws Exception {
        String email = unique("create") + "@example.com";

        mockMvc.perform(post("/api/members").contentType(MediaType.APPLICATION_JSON)
                        .content(memberJson(email, "ACTIVE")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createWithoutStatusDefaultsToActive() throws Exception {
        String json = """
                {"firstName":"Ivan","lastName":"Petrov","email":"%s@example.com"}
                """.formatted(unique("default"));

        mockMvc.perform(post("/api/members").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createWithInvalidFieldsReturns400WithDetails() throws Exception {
        String json = """
                {"firstName":"","lastName":"Petrov","email":"not-an-email"}
                """;

        mockMvc.perform(post("/api/members").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.details.length()").value(2));
    }

    @Test
    void createWithUnknownEnumValueReturns400() throws Exception {
        mockMvc.perform(post("/api/members").contentType(MediaType.APPLICATION_JSON)
                        .content(memberJson(unique("enum") + "@example.com", "SUPERHERO")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void createWithDuplicateEmailReturns409() throws Exception {
        String email = unique("dup") + "@example.com";
        mockMvc.perform(post("/api/members").contentType(MediaType.APPLICATION_JSON)
                .content(memberJson(email, "ACTIVE"))).andExpect(status().isCreated());

        mockMvc.perform(post("/api/members").contentType(MediaType.APPLICATION_JSON)
                        .content(memberJson(email.toUpperCase(), "ACTIVE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void getByIdReturnsMemberAndUnknownIdReturns404() throws Exception {
        long id = createMember();

        mockMvc.perform(get("/api/members/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        mockMvc.perform(get("/api/members/{id}", 987654321L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void getByIdWithMalformedIdReturns400() throws Exception {
        mockMvc.perform(get("/api/members/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void updateChangesFieldsAndKeepsId() throws Exception {
        long id = createMember();
        String newEmail = unique("updated") + "@example.com";

        mockMvc.perform(put("/api/members/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(memberJson(newEmail, "FROZEN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.email").value(newEmail))
                .andExpect(jsonPath("$.status").value("FROZEN"));
    }

    @Test
    void updateToEmailOfAnotherMemberReturns409() throws Exception {
        String takenEmail = unique("taken") + "@example.com";
        mockMvc.perform(post("/api/members").contentType(MediaType.APPLICATION_JSON)
                .content(memberJson(takenEmail, "ACTIVE"))).andExpect(status().isCreated());
        long id = createMember();

        mockMvc.perform(put("/api/members/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(memberJson(takenEmail, "ACTIVE")))
                .andExpect(status().isConflict());
    }

    @Test
    void updateUnknownMemberReturns404() throws Exception {
        mockMvc.perform(put("/api/members/{id}", 987654321L).contentType(MediaType.APPLICATION_JSON)
                        .content(memberJson(unique("none") + "@example.com", "ACTIVE")))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns204ThenMemberIsGone() throws Exception {
        long id = createMember();

        mockMvc.perform(delete("/api/members/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/members/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void deleteMemberReferencedBySubscriptionReturns409() throws Exception {
        long memberId = createMember();
        purchaseSubscription(memberId, createPlan(30));

        mockMvc.perform(delete("/api/members/{id}", memberId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void listIsPaginatedAndReturnsTotalCountHeader() throws Exception {
        for (int i = 0; i < 3; i++) {
            createMember();
        }

        MvcResult result = mockMvc.perform(get("/api/members").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Total-Count"))
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn();

        long total = Long.parseLong(result.getResponse().getHeader("X-Total-Count"));
        assertThat(total).isGreaterThanOrEqualTo(3);
    }

    @Test
    void listNeverReturnsMoreThan50Records() throws Exception {
        for (int i = 0; i < 55; i++) {
            createMember();
        }

        mockMvc.perform(get("/api/members").param("size", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(50));
    }

    @Test
    void memberSubscriptionsEndpointListsOnlyOwnSubscriptions() throws Exception {
        long memberId = createMember();
        long otherMemberId = createMember();
        long planId = createPlan(30);
        purchaseSubscription(memberId, planId);
        purchaseSubscription(otherMemberId, planId);

        mockMvc.perform(get("/api/members/{id}/subscriptions", memberId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].memberId").value(memberId));

        mockMvc.perform(get("/api/members/{id}/subscriptions", 987654321L))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownUrlReturns404WithHumanReadableBody() throws Exception {
        mockMvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isNotFound());
    }
}
