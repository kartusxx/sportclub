package ru.teamname.sportclub.booking.resilience;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import ru.teamname.sportclub.booking.client.ClassDirectory;
import ru.teamname.sportclub.booking.client.MemberDirectory;
import ru.teamname.sportclub.booking.exception.ResourceNotFoundException;
import ru.teamname.sportclub.booking.exception.ServiceUnavailableException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Circuit Breaker на настоящих Feign-клиентах: окно в 4 вызова, остальные параметры как в config-repo.
 * Каждый тест использует свой метод клиента, а цепь создаётся на метод, поэтому тесты не влияют друг на друга:
 * getMember (разрыв цепи), coverage (восстановление), getClass (404 не считается сбоем).
 */
@TestPropertySource(properties = {
        "resilience4j.circuitbreaker.configs.default.slidingWindowSize=4",
        "resilience4j.circuitbreaker.configs.default.minimumNumberOfCalls=4"
})
class CircuitBreakerIntegrationTest extends AbstractFeignStubTest {

    private static final String MEMBER_PATH = "/internal/members/1";

    @Autowired
    private MemberDirectory memberDirectory;
    @Autowired
    private ClassDirectory classDirectory;

    @Test
    void circuitOpensAfterRepeatedFailuresAndStopsCallingTheService() throws Exception {
        STUB.respond(MEMBER_PATH, 500, "{}");

        for (int i = 0; i < 6; i++) {
            assertThatThrownBy(() -> memberDirectory.getMember(1L)).isInstanceOf(ServiceUnavailableException.class);
        }

        // Первые 4 вызова дошли до сервиса и заполнили окно, 5-й и 6-й отклонены цепью без обращения по сети
        assertThat(STUB.hits(MEMBER_PATH)).isEqualTo(4);

        // Клиент при этом получает осмысленный 503, а не зависает и не получает 500
        String startsAt = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.SECONDS).toString();
        STUB.respond("/api/classes/890001", 200,
                "{\"id\":890001,\"title\":\"Yoga\",\"startsAt\":\"%s\",\"capacity\":5}".formatted(startsAt));
        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(1, 890001)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("member-service")));
        assertThat(STUB.hits(MEMBER_PATH)).isEqualTo(4);
    }

    @Test
    void circuitClosesAgainWhenServiceRecovers() {
        String path = "/internal/members/2/coverage";
        STUB.respond(path, 500, "{}");
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> memberDirectory.hasActiveSubscription(2L, LocalDate.of(2026, 12, 1)))
                    .isInstanceOf(ServiceUnavailableException.class);
        }
        // цепь разомкнута: сервис не вызывается
        assertThatThrownBy(() -> memberDirectory.hasActiveSubscription(2L, LocalDate.of(2026, 12, 1)))
                .isInstanceOf(ServiceUnavailableException.class);
        assertThat(STUB.hits(path)).isEqualTo(4);

        // сервис ожил; после waitDurationInOpenState (1 с) цепь пропускает пробный вызов и замыкается
        STUB.respond(path, 200, "{\"covered\":true}");
        await().atMost(Duration.ofSeconds(8)).pollInterval(Duration.ofMillis(250)).ignoreExceptions()
                .untilAsserted(() -> assertThat(
                        memberDirectory.hasActiveSubscription(2L, LocalDate.of(2026, 12, 1))).isTrue());

        assertThat(memberDirectory.hasActiveSubscription(2L, LocalDate.of(2026, 12, 1))).isTrue();
    }

    @Test
    void answersOf4xxAreNotCountedAsFailuresOfTheService() {
        String path = "/api/classes/3";
        // 10 ответов 404 подряд: окно из 4 вызовов было бы переполнено, будь они сбоями
        for (int i = 0; i < 10; i++) {
            assertThatThrownBy(() -> classDirectory.getClassInfo(3L)).isInstanceOf(ResourceNotFoundException.class);
        }
        assertThat(STUB.hits(path)).isEqualTo(10);

        // цепь осталась замкнутой: когда занятие появилось, вызов проходит сразу
        STUB.respond(path, 200, "{\"id\":3,\"title\":\"Yoga\",\"startsAt\":\"2030-01-01T10:00:00\",\"capacity\":5}");
        assertThat(classDirectory.getClassInfo(3L).title()).isEqualTo("Yoga");
    }
}
