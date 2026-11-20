package ru.teamname.sportclub.booking.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import ru.teamname.sportclub.booking.AbstractMockedDirectoriesTest;
import ru.teamname.sportclub.booking.exception.ResourceNotFoundException;
import ru.teamname.sportclub.booking.exception.ServiceUnavailableException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Записи на занятия: транзакционная часть работает с настоящей БД, чужие сервисы заменены заглушками. */
class BookingApiIntegrationTest extends AbstractMockedDirectoriesTest {

    @Test
    void bookingWithSubscriptionReturns201() throws Exception {
        long classId = newClass(5);
        long memberId = newMember();

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
    void bookingWithoutSubscriptionReturns422() throws Exception {
        long classId = newClass(5);
        long memberId = stubMember("ACTIVE", false);

        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(memberId, classId)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message").value("У участника нет действующего абонемента на дату занятия"));
    }

    @Test
    void bookingForNotActiveMemberReturns422() throws Exception {
        long classId = newClass(5);
        long memberId = stubMember("FROZEN", true);

        assertThat(bookingStatus(memberId, classId)).isEqualTo(422);
    }

    @Test
    void bookingOfClassThatAlreadyStartedReturns422() throws Exception {
        long classId = newClass(5, LocalDateTime.now().minusHours(1));
        long memberId = newMember();

        assertThat(bookingStatus(memberId, classId)).isEqualTo(422);
    }

    @Test
    void bookingWithUnknownMemberOrClassReturns404() throws Exception {
        long classId = newClass(3);
        long memberId = newMember();
        when(memberDirectory.getMember(987654321L)).thenThrow(new ResourceNotFoundException("Участник", 987654321L));
        when(classDirectory.getClassInfo(987654321L)).thenThrow(new ResourceNotFoundException("Занятие", 987654321L));

        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(987654321L, classId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(memberId, 987654321L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Занятие с id=987654321 не найден(а)"));
    }

    @Test
    void bookingReturns503WhenAnotherServiceIsUnavailable() throws Exception {
        long classId = newClass(3);
        long memberId = newMember();
        when(memberDirectory.getMember(memberId)).thenThrow(new ServiceUnavailableException("member-service недоступен"));

        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(memberId, classId)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

    @Test
    void duplicateBookingReturns409() throws Exception {
        long classId = newClass(5);
        long memberId = newMember();
        assertThat(bookingStatus(memberId, classId)).isEqualTo(201);

        assertThat(bookingStatus(memberId, classId)).isEqualTo(409);
    }

    @Test
    void fullClassRejectsNextBookingUntilSomeoneCancels() throws Exception {
        long classId = newClass(1);
        long first = newMember();
        long second = newMember();
        long bookingId = postAndGetId("/api/bookings", bookingJson(first, classId));

        assertThat(bookingStatus(second, classId)).isEqualTo(409);

        mockMvc.perform(post("/api/bookings/{id}/cancel", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(bookingStatus(second, classId)).isEqualTo(201);
    }

    @Test
    void memberCanRebookAfterCancellationAndKeepsTheSameRecord() throws Exception {
        long classId = newClass(3);
        long memberId = newMember();
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
        long bookingId = postAndGetId("/api/bookings", bookingJson(newMember(), classId));
        mockMvc.perform(post("/api/bookings/{id}/cancel", bookingId)).andExpect(status().isOk());

        mockMvc.perform(post("/api/bookings/{id}/cancel", bookingId)).andExpect(status().is(422));
    }

    @Test
    void cancelAfterClassStartedReturns422() throws Exception {
        long classId = newClass(3);
        long bookingId = postAndGetId("/api/bookings", bookingJson(newMember(), classId));
        stubClass(classId, 3, LocalDateTime.now().minusMinutes(5));

        mockMvc.perform(post("/api/bookings/{id}/cancel", bookingId)).andExpect(status().is(422));
    }

    @Test
    void attendanceCannotBeMarkedBeforeTheClassStarts() throws Exception {
        long classId = newClass(3);
        long bookingId = postAndGetId("/api/bookings", bookingJson(newMember(), classId));

        mockMvc.perform(post("/api/bookings/{id}/attend", bookingId)).andExpect(status().is(422));
    }

    @Test
    void attendanceIsMarkedAfterTheClassStarted() throws Exception {
        long classId = newClass(3);
        long bookingId = postAndGetId("/api/bookings", bookingJson(newMember(), classId));
        stubClass(classId, 3, LocalDateTime.now().minusMinutes(5));

        mockMvc.perform(post("/api/bookings/{id}/attend", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATTENDED"));
    }

    @Test
    void validationErrorsAreListed() throws Exception {
        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ошибка валидации входных данных"))
                .andExpect(jsonPath("$.details.length()").value(2));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content("{не json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void readAndDeleteEndpoints() throws Exception {
        long classId = newClass(3);
        long bookingId = postAndGetId("/api/bookings", bookingJson(newMember(), classId));

        mockMvc.perform(get("/api/bookings/{id}", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId));
        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Total-Count"));
        mockMvc.perform(delete("/api/bookings/{id}", bookingId)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/bookings/{id}", bookingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Запись с id=" + bookingId + " не найден(а)"));
        mockMvc.perform(delete("/api/bookings/{id}", bookingId)).andExpect(status().isNotFound());
    }

    @Test
    void listIsPagedAndCappedAt50() throws Exception {
        long classId = newClass(10);
        postAndGetId("/api/bookings", bookingJson(newMember(), classId));
        postAndGetId("/api/bookings", bookingJson(newMember(), classId));

        mockMvc.perform(get("/api/bookings?page=0&size=1"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Total-Count"))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/bookings?size=1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.lessThanOrEqualTo(50)));
    }

    @Test
    void unknownSortFieldReturns400() throws Exception {
        mockMvc.perform(get("/api/bookings?sort=secret"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Сортировка по полю 'secret' невозможна"));
    }

    @Test
    void bookingsOfClassAreListedWithTotalCount() throws Exception {
        long classId = newClass(10);
        postAndGetId("/api/bookings", bookingJson(newMember(), classId));
        postAndGetId("/api/bookings", bookingJson(newMember(), classId));

        mockMvc.perform(get("/api/classes/{id}/bookings", classId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "2"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].classId").value(classId));
    }

    @Test
    void bookingsOfUnknownClassReturn404() throws Exception {
        when(classDirectory.getClassInfo(987654321L)).thenThrow(new ResourceNotFoundException("Занятие", 987654321L));

        mockMvc.perform(get("/api/classes/{id}/bookings", 987654321L)).andExpect(status().isNotFound());
    }

    /**
     * Доказательство необходимости блокировки: при одновременных запросах на единственное место
     * успешной должна быть ровно одна запись. Без консультативной блокировки занятия две транзакции
     * видят «0 записей» и обе вставляют строку (овербукинг).
     */
    @Test
    void concurrentBookingsNeverExceedClassCapacity() throws Exception {
        long classId = newClass(1);
        int attempts = 6;
        List<Long> memberIds = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            memberIds.add(newMember());
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
