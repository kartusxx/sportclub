package ru.teamname.sportclub.member.controller;

import org.junit.jupiter.api.Test;
import ru.teamname.sportclub.member.AbstractIntegrationTest;

import java.time.LocalDate;

/** Внутренний API для booking-service. */
class InternalMemberApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void summaryReturnsStatusAndFullName() {
        long id = createMember("FROZEN");

        web.get().uri("/internal/members/{id}", id).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(id)
                .jsonPath("$.fullName").isEqualTo("Ivan Petrov")
                .jsonPath("$.status").isEqualTo("FROZEN");
    }

    @Test
    void summaryOfUnknownMemberReturns404() {
        web.get().uri("/internal/members/{id}", 987654321L).exchange().expectStatus().isNotFound();
    }

    @Test
    void coverageIsTrueOnlyInsideSubscriptionPeriod() {
        long memberId = createMember();
        purchaseSubscription(memberId, createPlan(30));
        LocalDate soon = LocalDate.now().plusDays(2);
        LocalDate farFuture = LocalDate.now().plusDays(400);

        web.get().uri("/internal/members/{id}/coverage?date={date}", memberId, soon).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.covered").isEqualTo(true);
        web.get().uri("/internal/members/{id}/coverage?date={date}", memberId, farFuture).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.covered").isEqualTo(false);
    }

    @Test
    void coverageOfMemberWithoutSubscriptionIsFalseAndUnknownMemberIs404() {
        long memberId = createMember();

        web.get().uri("/internal/members/{id}/coverage?date={date}", memberId, LocalDate.now()).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.covered").isEqualTo(false);
        web.get().uri("/internal/members/{id}/coverage?date={date}", 987654321L, LocalDate.now()).exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void coverageWithMalformedDateReturns400() {
        web.get().uri("/internal/members/1/coverage?date=yesterday").exchange().expectStatus().isBadRequest();
    }
}
