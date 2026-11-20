package ru.teamname.sportclub.booking.resilience;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import ru.teamname.sportclub.booking.client.ClassDirectory;
import ru.teamname.sportclub.booking.client.ClassInfo;
import ru.teamname.sportclub.booking.client.MemberDirectory;
import ru.teamname.sportclub.booking.client.MemberInfo;
import ru.teamname.sportclub.booking.exception.ResourceNotFoundException;
import ru.teamname.sportclub.booking.exception.ServiceUnavailableException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Контракты с member-service и catalog-service на уровне HTTP: пути, JSON, коды ответов, таймауты. */
class FeignDirectoriesIntegrationTest extends AbstractFeignStubTest {

    @Autowired
    private MemberDirectory memberDirectory;
    @Autowired
    private ClassDirectory classDirectory;

    @Test
    void memberIsReadFromInternalApiAndUnknownFieldsAreIgnored() {
        STUB.respond("/internal/members/1", 200,
                "{\"id\":1,\"fullName\":\"Ivan Petrov\",\"status\":\"ACTIVE\",\"email\":\"i@example.com\"}");

        MemberInfo member = memberDirectory.getMember(1L);

        assertThat(member.fullName()).isEqualTo("Ivan Petrov");
        assertThat(member.isActive()).isTrue();
    }

    @Test
    void coverageIsRequestedWithIsoDate() {
        STUB.respond("/internal/members/1/coverage", 200, "{\"covered\":true}");

        assertThat(memberDirectory.hasActiveSubscription(1L, LocalDate.of(2026, 12, 1))).isTrue();
        assertThat(STUB.lastQuery()).isEqualTo("date=2026-12-01");
    }

    @Test
    void classIsReadFromCatalogApi() {
        STUB.respond("/api/classes/5", 200,
                "{\"id\":5,\"title\":\"Yoga\",\"startsAt\":\"2030-01-01T10:00:00\",\"capacity\":12,"
                        + "\"trainerName\":\"Anna\",\"level\":\"BEGINNER\"}");

        ClassInfo info = classDirectory.getClassInfo(5L);

        assertThat(info.title()).isEqualTo("Yoga");
        assertThat(info.startsAt()).isEqualTo(LocalDateTime.of(2030, 1, 1, 10, 0));
        assertThat(info.capacity()).isEqualTo(12);
    }

    @Test
    void notFoundFromOtherServicesBecomesResourceNotFound() {
        // пути не зарегистрированы: заглушка отвечает 404
        assertThatThrownBy(() -> memberDirectory.getMember(404L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> memberDirectory.hasActiveSubscription(404L, LocalDate.now()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> classDirectory.getClassInfo(404L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void serverErrorBecomesServiceUnavailable() {
        STUB.respond("/internal/members/1", 500, "{\"error\":\"boom\"}");
        STUB.respond("/api/classes/5", 503, "{}");

        assertThatThrownBy(() -> memberDirectory.getMember(1L))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("member-service");
        assertThatThrownBy(() -> classDirectory.getClassInfo(5L))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("catalog-service");
    }

    @Test
    void slowServiceIsCutOffByReadTimeout() {
        STUB.respondWithDelay("/internal/members/7", 200, "{\"id\":7,\"fullName\":\"Slow\",\"status\":\"ACTIVE\"}", 2500);

        long started = System.nanoTime();
        assertThatThrownBy(() -> memberDirectory.getMember(7L)).isInstanceOf(ServiceUnavailableException.class);
        long elapsedMillis = (System.nanoTime() - started) / 1_000_000;

        // readTimeout 500 мс + одна повторная попытка через 200 мс, но не 2,5 секунды ожидания ответа
        assertThat(elapsedMillis).isLessThan(2400);
    }

    @Test
    void bookingEndpointWorksEndToEndThroughRealClients() throws Exception {
        String startsAt = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.SECONDS).toString();
        STUB.respond("/api/classes/880001", 200,
                "{\"id\":880001,\"title\":\"Yoga\",\"startsAt\":\"%s\",\"capacity\":5}".formatted(startsAt));
        STUB.respond("/internal/members/880002", 200, "{\"id\":880002,\"fullName\":\"Ivan\",\"status\":\"ACTIVE\"}");
        STUB.respond("/internal/members/880002/coverage", 200, "{\"covered\":true}");

        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(880002, 880001)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BOOKED"));
    }

    @Test
    void bookingEndpointAnswers503WhenCatalogServiceFails() throws Exception {
        STUB.respond("/api/classes/880011", 500, "{}");

        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(880012, 880011)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("catalog-service")));
    }

    @Test
    void bookingEndpointAnswers404WhenMemberDoesNotExist() throws Exception {
        String startsAt = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.SECONDS).toString();
        STUB.respond("/api/classes/880021", 200,
                "{\"id\":880021,\"title\":\"Yoga\",\"startsAt\":\"%s\",\"capacity\":5}".formatted(startsAt));
        // участника заглушка не знает: ответит 404

        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(880022, 880021)))
                .andExpect(status().isNotFound());
    }
}
