package ru.teamname.sportclub.member.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import ru.teamname.sportclub.member.AbstractIntegrationTest;

class PlanAndSubscriptionApiIntegrationTest extends AbstractIntegrationTest {

    private static String planJson(String name, int days) {
        return """
                {"name":"%s","type":"YEARLY","price":9990.50,"durationDays":%d}
                """.formatted(name, days);
    }

    @Test
    void planCrudWorks() {
        String name = unique("plan");
        long id = postAndGetId("/api/plans", planJson(name, 365));

        web.get().uri("/api/plans/{id}", id).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.name").isEqualTo(name).jsonPath("$.type").isEqualTo("YEARLY");

        web.put().uri("/api/plans/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(planJson(name, 30))
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.durationDays").isEqualTo(30);

        web.get().uri("/api/plans?size=5").exchange()
                .expectStatus().isOk().expectHeader().exists("X-Total-Count");

        web.delete().uri("/api/plans/{id}", id).exchange().expectStatus().isNoContent();
        web.get().uri("/api/plans/{id}", id).exchange().expectStatus().isNotFound();
    }

    @Test
    void planValidationAndDuplicateName() {
        web.post().uri("/api/plans").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"\",\"type\":\"MONTHLY\",\"price\":-1,\"durationDays\":0}")
                .exchange().expectStatus().isBadRequest();

        String name = unique("dup-plan");
        postAndGetId("/api/plans", planJson(name, 30));
        web.post().uri("/api/plans").contentType(MediaType.APPLICATION_JSON).bodyValue(planJson(name, 30))
                .exchange().expectStatus().isEqualTo(409);
    }

    @Test
    void purchaseCreatesActiveSubscriptionStartingToday() {
        long memberId = createMember();
        long planId = createPlan(30);

        long id = purchaseSubscription(memberId, planId);

        web.get().uri("/api/subscriptions/{id}", id).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ACTIVE")
                .jsonPath("$.memberId").isEqualTo(memberId)
                .jsonPath("$.planId").isEqualTo(planId);
    }

    @Test
    void secondPurchaseExtendsAfterTheFirst() {
        long memberId = createMember();
        long planId = createPlan(30);
        long first = purchaseSubscription(memberId, planId);
        long second = purchaseSubscription(memberId, planId);

        byte[] firstBody = web.get().uri("/api/subscriptions/{id}", first).exchange()
                .expectBody().returnResult().getResponseBody();
        web.get().uri("/api/subscriptions/{id}", second).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.startDate").value(start -> org.assertj.core.api.Assertions
                        .assertThat(String.valueOf(start)).isNotEqualTo(extractStart(firstBody)));
    }

    private static String extractStart(byte[] body) {
        return com.jayway.jsonpath.JsonPath.read(new String(body, java.nio.charset.StandardCharsets.UTF_8),
                "$.startDate");
    }

    @Test
    void purchaseForFrozenMemberReturnsBusinessRuleError() {
        long memberId = createMember("FROZEN");
        long planId = createPlan(30);

        web.post().uri("/api/subscriptions").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"memberId\":%d,\"planId\":%d}".formatted(memberId, planId))
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBody().jsonPath("$.message").isNotEmpty();
    }

    @Test
    void purchaseWithUnknownMemberOrPlanReturns404() {
        long memberId = createMember();
        long planId = createPlan(30);

        web.post().uri("/api/subscriptions").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"memberId\":987654321,\"planId\":%d}".formatted(planId))
                .exchange().expectStatus().isNotFound();
        web.post().uri("/api/subscriptions").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"memberId\":%d,\"planId\":987654321}".formatted(memberId))
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void cancelChangesStatusAndSecondCancelIsRejected() {
        long id = purchaseSubscription(createMember(), createPlan(30));

        web.post().uri("/api/subscriptions/{id}/cancel", id).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("CANCELLED");
        web.post().uri("/api/subscriptions/{id}/cancel", id).exchange().expectStatus().is4xxClientError();
    }

    @Test
    void deleteRemovesSubscriptionAndListIsPaginated() {
        long id = purchaseSubscription(createMember(), createPlan(30));

        web.get().uri("/api/subscriptions?size=5").exchange()
                .expectStatus().isOk().expectHeader().exists("X-Total-Count");
        web.delete().uri("/api/subscriptions/{id}", id).exchange().expectStatus().isNoContent();
        web.get().uri("/api/subscriptions/{id}", id).exchange().expectStatus().isNotFound();
    }
}
