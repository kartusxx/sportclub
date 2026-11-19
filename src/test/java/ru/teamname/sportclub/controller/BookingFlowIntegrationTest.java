package ru.teamname.sportclub.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import ru.teamname.sportclub.AbstractIntegrationTest;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Бизнес-сценарии с транзакциями: покупка абонемента и запись на занятие. */
class BookingFlowIntegrationTest extends AbstractIntegrationTest {

    private long newClass(int capacity) throws Exception {
        long disciplineId = createDiscipline();
        long trainerId = createTrainer(disciplineId);
        return createClass(trainerId, disciplineId, capacity, futureStart());
    }

    private long memberWithSubscription(long planId) throws Exception {
        long memberId = createMember();
        purchaseSubscription(memberId, planId);
        return memberId;
    }

    private int bookingStatus(long memberId, long classId) throws Exception {
        return mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(memberId, classId)))
                .andReturn().getResponse().getStatus();
    }

    private String bodyOf(String url, String json) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    // ---- покупка абонемента ----

    @Test
    void purchaseCreatesActiveSubscriptionStartingToday() throws Exception {
        long memberId = createMember();
        long planId = createPlan(30);

        mockMvc.perform(post("/api/subscriptions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":%d,\"planId\":%d}".formatted(memberId, planId)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.startDate").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.endDate").value(LocalDate.now().plusDays(29).toString()));
    }

    @Test
    void secondPurchaseStartsAfterTheFirstOneEnds() throws Exception {
        long memberId = createMember();
        long planId = createPlan(30);
        String first = bodyOf("/api/subscriptions", "{\"memberId\":%d,\"planId\":%d}".formatted(memberId, planId));
        String second = bodyOf("/api/subscriptions", "{\"memberId\":%d,\"planId\":%d}".formatted(memberId, planId));

        LocalDate firstEnd = LocalDate.parse(JsonPath.read(first, "$.endDate"));
        LocalDate secondStart = LocalDate.parse(JsonPath.read(second, "$.startDate"));
        assertThat(secondStart).isEqualTo(firstEnd.plusDays(1));
    }

    @Test
    void purchaseForBlockedMemberReturns422() throws Exception {
        long memberId = createMember("BLOCKED");
        long planId = createPlan(30);

        mockMvc.perform(post("/api/subscriptions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":%d,\"planId\":%d}".formatted(memberId, planId)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void purchaseWithUnknownMemberOrPlanReturns404() throws Exception {
        long memberId = createMember();
        long planId = createPlan(30);

        mockMvc.perform(post("/api/subscriptions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":987654321,\"planId\":%d}".formatted(planId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/subscriptions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":%d,\"planId\":987654321}".formatted(memberId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void purchaseWithMissingFieldsReturns400() throws Exception {
        mockMvc.perform(post("/api/subscriptions").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.length()").value(2));
    }

    @Test
    void subscriptionCanBeCancelledOnlyOnce() throws Exception {
        long id = purchaseSubscription(createMember(), createPlan(30));

        mockMvc.perform(post("/api/subscriptions/{id}/cancel", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mockMvc.perform(post("/api/subscriptions/{id}/cancel", id))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void subscriptionReadListAndDelete() throws Exception {
        long id = purchaseSubscription(createMember(), createPlan(30));

        mockMvc.perform(get("/api/subscriptions/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
        mockMvc.perform(get("/api/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Total-Count"));
        mockMvc.perform(delete("/api/subscriptions/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/subscriptions/{id}", id)).andExpect(status().isNotFound());
    }

    // ---- запись на занятие ----

    @Test
    void bookingWithoutSubscriptionReturns422() throws Exception {
        long classId = newClass(5);
        long memberId = createMember();

        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(memberId, classId)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void bookingWithSubscriptionReturns201() throws Exception {
        long classId = newClass(5);
        long memberId = memberWithSubscription(createPlan(30));

        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(memberId, classId)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("BOOKED"))
                .andExpect(jsonPath("$.memberId").value(memberId))
                .andExpect(jsonPath("$.classId").value(classId))
                .andExpect(jsonPath("$.note").value("see you"));
    }

    @Test
    void duplicateBookingReturns409() throws Exception {
        long classId = newClass(5);
        long memberId = memberWithSubscription(createPlan(30));
        assertThat(bookingStatus(memberId, classId)).isEqualTo(201);

        assertThat(bookingStatus(memberId, classId)).isEqualTo(409);
    }

    @Test
    void fullClassRejectsNextBookingUntilSomeoneCancels() throws Exception {
        long classId = newClass(1);
        long planId = createPlan(30);
        long first = memberWithSubscription(planId);
        long second = memberWithSubscription(planId);
        long bookingId = postAndGetId("/api/bookings", bookingJson(first, classId));

        assertThat(bookingStatus(second, classId)).isEqualTo(409);

        mockMvc.perform(post("/api/bookings/{id}/cancel", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(bookingStatus(second, classId)).isEqualTo(201);
    }

    @Test
    void memberCanRebookAfterCancellation() throws Exception {
        long classId = newClass(3);
        long memberId = memberWithSubscription(createPlan(30));
        long bookingId = postAndGetId("/api/bookings", bookingJson(memberId, classId));
        mockMvc.perform(post("/api/bookings/{id}/cancel", bookingId)).andExpect(status().isOk());

        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(memberId, classId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(bookingId))
                .andExpect(jsonPath("$.status").value("BOOKED"));
    }

    @Test
    void cancelledBookingCannotBeCancelledAgain() throws Exception {
        long classId = newClass(3);
        long memberId = memberWithSubscription(createPlan(30));
        long bookingId = postAndGetId("/api/bookings", bookingJson(memberId, classId));
        mockMvc.perform(post("/api/bookings/{id}/cancel", bookingId)).andExpect(status().isOk());

        mockMvc.perform(post("/api/bookings/{id}/cancel", bookingId))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void attendanceCannotBeMarkedBeforeTheClassStarts() throws Exception {
        long classId = newClass(3);
        long memberId = memberWithSubscription(createPlan(30));
        long bookingId = postAndGetId("/api/bookings", bookingJson(memberId, classId));

        mockMvc.perform(post("/api/bookings/{id}/attend", bookingId))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void bookingWithUnknownMemberOrClassReturns404() throws Exception {
        long classId = newClass(3);
        long memberId = createMember();

        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(987654321L, classId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(memberId, 987654321L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void bookingForFrozenMemberReturns422() throws Exception {
        long classId = newClass(3);
        long memberId = memberWithSubscription(createPlan(30));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/members/{id}", memberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ivan","lastName":"Petrov","email":"%s@example.com","status":"FROZEN"}
                                """.formatted(unique("frozen"))))
                .andExpect(status().isOk());

        assertThat(bookingStatus(memberId, classId)).isEqualTo(422);
    }

    @Test
    void bookingValidationAndReadEndpoints() throws Exception {
        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        long classId = newClass(3);
        long memberId = memberWithSubscription(createPlan(30));
        long bookingId = postAndGetId("/api/bookings", bookingJson(memberId, classId));

        mockMvc.perform(get("/api/bookings/{id}", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId));
        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Total-Count"));
        mockMvc.perform(delete("/api/bookings/{id}", bookingId)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/bookings/{id}", bookingId)).andExpect(status().isNotFound());
    }

    /**
     * Доказательство необходимости транзакции с блокировкой: при одновременных запросах
     * на единственное место успешной должна быть ровно одна запись.
     */
    @Test
    void concurrentBookingsNeverExceedClassCapacity() throws Exception {
        long classId = newClass(1);
        long planId = createPlan(30);
        int attempts = 6;
        List<Long> memberIds = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            memberIds.add(memberWithSubscription(planId));
        }

        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        try {
            for (Long memberId : memberIds) {
                Callable<Integer> task = () -> {
                    ready.countDown();
                    start.await();
                    return bookingStatus(memberId, classId);
                };
                results.add(pool.submit(task));
            }
            ready.await();
            start.countDown();

            int created = 0;
            int conflicts = 0;
            for (Future<Integer> result : results) {
                int code = result.get();
                if (code == 201) {
                    created++;
                } else if (code == 409) {
                    conflicts++;
                }
            }
            assertThat(created).isEqualTo(1);
            assertThat(conflicts).isEqualTo(attempts - 1);
        } finally {
            pool.shutdownNow();
        }
    }
}
