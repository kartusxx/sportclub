package ru.teamname.sportclub.member.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import ru.teamname.sportclub.member.AbstractIntegrationTest;

import static org.mockito.Mockito.when;

class MemberApiIntegrationTest extends AbstractIntegrationTest {

    private void post(String json, int expectedStatus) {
        web.post().uri("/api/members").contentType(MediaType.APPLICATION_JSON).bodyValue(json)
                .exchange().expectStatus().isEqualTo(expectedStatus);
    }

    @Test
    void createReturns201WithLocationAndBody() {
        String email = unique("create") + "@example.com";

        web.post().uri("/api/members").contentType(MediaType.APPLICATION_JSON).bodyValue(memberJson(email, "ACTIVE"))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("Location")
                .expectBody()
                .jsonPath("$.id").isNumber()
                .jsonPath("$.email").isEqualTo(email)
                .jsonPath("$.status").isEqualTo("ACTIVE");
    }

    @Test
    void createWithoutStatusDefaultsToActive() {
        String json = """
                {"firstName":"Ivan","lastName":"Petrov","email":"%s@example.com"}
                """.formatted(unique("default"));

        web.post().uri("/api/members").contentType(MediaType.APPLICATION_JSON).bodyValue(json)
                .exchange()
                .expectStatus().isCreated()
                .expectBody().jsonPath("$.status").isEqualTo("ACTIVE");
    }

    @Test
    void createWithInvalidFieldsReturns400WithDetails() {
        String json = """
                {"firstName":"","lastName":"Petrov","email":"not-an-email"}
                """;

        web.post().uri("/api/members").contentType(MediaType.APPLICATION_JSON).bodyValue(json)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").isNotEmpty()
                .jsonPath("$.details.length()").isEqualTo(2);
    }

    @Test
    void createWithUnknownEnumValueReturns400() {
        post(memberJson(unique("enum") + "@example.com", "SUPERHERO"), 400);
    }

    @Test
    void createWithDuplicateEmailReturns409() {
        String email = unique("dup") + "@example.com";
        post(memberJson(email, "ACTIVE"), 201);

        web.post().uri("/api/members").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(memberJson(email.toUpperCase(), "ACTIVE"))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.status").isEqualTo(409);
    }

    @Test
    void getByIdReturnsMemberAndUnknownIdReturns404() {
        long id = createMember();

        web.get().uri("/api/members/{id}", id).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.id").isEqualTo(id);

        web.get().uri("/api/members/{id}", 987654321L).exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.message").isNotEmpty();
    }

    @Test
    void getByIdWithMalformedIdReturns400() {
        web.get().uri("/api/members/abc").exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.message").isNotEmpty();
    }

    @Test
    void updateChangesFieldsAndKeepsId() {
        long id = createMember();
        String newEmail = unique("updated") + "@example.com";

        web.put().uri("/api/members/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(memberJson(newEmail, "FROZEN"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(id)
                .jsonPath("$.email").isEqualTo(newEmail)
                .jsonPath("$.status").isEqualTo("FROZEN");
    }

    @Test
    void updateToEmailOfAnotherMemberReturns409AndUnknownMemberReturns404() {
        String takenEmail = unique("taken") + "@example.com";
        post(memberJson(takenEmail, "ACTIVE"), 201);
        long id = createMember();

        web.put().uri("/api/members/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(memberJson(takenEmail, "ACTIVE"))
                .exchange().expectStatus().isEqualTo(409);

        web.put().uri("/api/members/{id}", 987654321L).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(memberJson(unique("none") + "@example.com", "ACTIVE"))
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void deleteReturns204ThenMemberIsGone() {
        long id = createMember();

        web.delete().uri("/api/members/{id}", id).exchange().expectStatus().isNoContent();
        web.get().uri("/api/members/{id}", id).exchange().expectStatus().isNotFound();
    }

    @Test
    void deleteUnknownMemberReturns404() {
        web.delete().uri("/api/members/{id}", 987654321L).exchange().expectStatus().isNotFound();
    }

    @Test
    void deleteMemberWithBookingsReturns409() {
        long id = createMember();
        when(bookingGateway.hasBookings(id)).thenReturn(true);

        web.delete().uri("/api/members/{id}", id).exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.message").value(message ->
                        org.assertj.core.api.Assertions.assertThat((String) message).contains("записи"));
        web.get().uri("/api/members/{id}", id).exchange().expectStatus().isOk();
    }

    @Test
    void deleteReturns503WhenBookingServiceIsUnavailable() {
        long id = createMember();
        when(bookingGateway.hasBookings(id)).thenThrow(
                new ru.teamname.sportclub.member.exception.ServiceUnavailableException("booking-service недоступен"));

        web.delete().uri("/api/members/{id}", id).exchange()
                .expectStatus().isEqualTo(503)
                .expectBody().jsonPath("$.status").isEqualTo(503);
        web.get().uri("/api/members/{id}", id).exchange().expectStatus().isOk();
    }

    @Test
    void deleteMemberReferencedBySubscriptionReturns409() {
        long memberId = createMember();
        purchaseSubscription(memberId, createPlan(30));

        web.delete().uri("/api/members/{id}", memberId).exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.message").isNotEmpty();
    }

    @Test
    void listIsPaginatedAndReturnsTotalCountHeader() {
        for (int i = 0; i < 3; i++) {
            createMember();
        }

        web.get().uri("/api/members?page=0&size=2").exchange()
                .expectStatus().isOk()
                .expectHeader().value("X-Total-Count", total ->
                        org.assertj.core.api.Assertions.assertThat(Long.parseLong(total)).isGreaterThanOrEqualTo(3))
                .expectBody().jsonPath("$.length()").isEqualTo(2);
    }

    @Test
    void listNeverReturnsMoreThan50Records() {
        for (int i = 0; i < 55; i++) {
            createMember();
        }

        web.get().uri("/api/members?size=1000").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(50);
    }

    @Test
    void listSupportsSortingAndRejectsUnknownSortField() {
        web.get().uri("/api/members?sort=email,desc&size=5").exchange().expectStatus().isOk();
        web.get().uri("/api/members?sort=password").exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.message").isNotEmpty();
    }

    @Test
    void memberSubscriptionsEndpointListsOnlyOwnSubscriptions() {
        long memberId = createMember();
        long otherMemberId = createMember();
        long planId = createPlan(30);
        purchaseSubscription(memberId, planId);
        purchaseSubscription(otherMemberId, planId);

        web.get().uri("/api/members/{id}/subscriptions", memberId).exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Total-Count", "1")
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].memberId").isEqualTo(memberId);

        web.get().uri("/api/members/{id}/subscriptions", 987654321L).exchange().expectStatus().isNotFound();
    }

    @Test
    void unknownUrlReturns404() {
        web.get().uri("/api/does-not-exist").exchange().expectStatus().isNotFound();
    }
}
