package ru.teamname.sportclub.member;

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
import ru.teamname.sportclub.member.client.BookingGateway;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.UUID;

/**
 * Базовый класс интеграционных тестов member-service: один общий контейнер PostgreSQL (Testcontainers)
 * на весь прогон, WebTestClient без сетевого порта и помощники создания данных через REST API.
 * Config Server и Eureka в тестах отключены, настройки JPA задаются здесь (в рабочем режиме их отдаёт config-repo).
 * Внешний сервис booking-service заменён заглушкой {@link BookingGateway}.
 */
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.fail-fast=false",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.jpa.open-in-view=false",
        "spring.jpa.hibernate.ddl-auto=validate"
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
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    protected BookingGateway bookingGateway;

    protected WebTestClient web;

    @BeforeEach
    protected void setUpClient() {
        web = WebTestClient.bindToApplicationContext(applicationContext).build();
    }

    protected static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    protected long postAndGetId(String url, String json) {
        byte[] body = web.post().uri(url).contentType(MediaType.APPLICATION_JSON).bodyValue(json)
                .exchange()
                .expectStatus().isCreated()
                .expectBody().returnResult().getResponseBody();
        return ((Number) JsonPath.read(new String(body, StandardCharsets.UTF_8), "$.id")).longValue();
    }

    protected static String memberJson(String email, String status) {
        return """
                {"firstName":"Ivan","lastName":"Petrov","email":"%s","phone":"+7 900 123-45-67","status":"%s"}
                """.formatted(email, status);
    }

    protected long createMember() {
        return createMember("ACTIVE");
    }

    protected long createMember(String status) {
        return postAndGetId("/api/members", memberJson(unique("member") + "@example.com", status));
    }

    protected long createPlan(int durationDays) {
        return postAndGetId("/api/plans", """
                {"name":"%s","type":"MONTHLY","price":1500.00,"durationDays":%d}
                """.formatted(unique("plan"), durationDays));
    }

    protected long purchaseSubscription(long memberId, long planId) {
        return postAndGetId("/api/subscriptions", """
                {"memberId":%d,"planId":%d}
                """.formatted(memberId, planId));
    }
}
