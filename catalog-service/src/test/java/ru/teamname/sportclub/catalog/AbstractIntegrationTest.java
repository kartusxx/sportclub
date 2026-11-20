package ru.teamname.sportclub.catalog;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;
import reactor.core.publisher.Mono;
import ru.teamname.sportclub.catalog.client.BookingGateway;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Properties;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Базовый класс интеграционных тестов catalog-service: один общий контейнер PostgreSQL (Testcontainers)
 * на весь прогон. Приложение подключается к нему двумя способами, как и в рабочем режиме:
 * R2DBC для запросов и JDBC для миграций Flyway. Config Server и Eureka отключены.
 * Внешний сервис booking-service заменён заглушкой {@link BookingGateway}.
 */
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.fail-fast=false",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
public abstract class AbstractIntegrationTest {

    /** Версия образа, имя БД, пользователь и пароль лежат в src/test/resources/test-database.properties. */
    private static final Properties DATABASE = loadDatabaseProperties();

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DATABASE.getProperty("test.postgres.image"))
            .withDatabaseName(DATABASE.getProperty("test.postgres.database"))
            .withUsername(DATABASE.getProperty("test.postgres.username"))
            .withPassword(DATABASE.getProperty("test.postgres.password"));

    static {
        POSTGRES.start();
    }

    private static Properties loadDatabaseProperties() {
        try (InputStream in = AbstractIntegrationTest.class.getResourceAsStream("/test-database.properties")) {
            if (in == null) {
                throw new IllegalStateException("Не найден src/test/resources/test-database.properties");
            }
            Properties properties = new Properties();
            properties.load(in);
            return properties;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://" + POSTGRES.getHost() + ":"
                + POSTGRES.getMappedPort(5432) + "/" + POSTGRES.getDatabaseName());
        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
    }

    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    protected BookingGateway bookingGateway;

    protected WebTestClient web;

    @BeforeEach
    protected void setUpClient() {
        web = WebTestClient.bindToApplicationContext(applicationContext).build();
        // Удаление записей по умолчанию проходит успешно; тесты переопределяют это при необходимости
        when(bookingGateway.deleteBookings(anyLong())).thenReturn(Mono.empty());
    }

    protected static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    protected static String futureDate(int daysAhead) {
        return LocalDateTime.now().plusDays(daysAhead).truncatedTo(ChronoUnit.MINUTES).toString();
    }

    protected long postAndGetId(String url, String json) {
        byte[] body = web.post().uri(url).contentType(MediaType.APPLICATION_JSON).bodyValue(json)
                .exchange()
                .expectStatus().isCreated()
                .expectBody().returnResult().getResponseBody();
        return ((Number) JsonPath.read(new String(body, StandardCharsets.UTF_8), "$.id")).longValue();
    }

    protected long createDiscipline() {
        return postAndGetId("/api/disciplines", """
                {"name":"%s","description":"Описание"}
                """.formatted(unique("discipline")));
    }

    protected long createTrainer(long... disciplineIds) {
        StringBuilder ids = new StringBuilder();
        for (long id : disciplineIds) {
            ids.append(ids.isEmpty() ? "" : ",").append(id);
        }
        return postAndGetId("/api/trainers", trainerJson(unique("trainer") + "@example.com", ids.toString()));
    }

    protected static String trainerJson(String email, String disciplineIds) {
        return """
                {"firstName":"Anna","lastName":"Smirnova","email":"%s","experienceYears":5,
                 "bio":"Опытный тренер","disciplineIds":[%s]}
                """.formatted(email, disciplineIds);
    }

    protected static String classJson(String title, long trainerId, long disciplineId, int capacity,
                                      String startsAt) {
        return """
                {"title":"%s","description":"Групповое занятие","trainerId":%d,"disciplineId":%d,
                 "room":"Зал 1","startsAt":"%s","durationMinutes":60,"capacity":%d,"level":"BEGINNER"}
                """.formatted(title, trainerId, disciplineId, startsAt, capacity);
    }

    protected long createClass(long trainerId, long disciplineId, int capacity) {
        return postAndGetId("/api/classes",
                classJson(unique("class"), trainerId, disciplineId, capacity, futureDate(3)));
    }
}
