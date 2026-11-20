package ru.teamname.sportclub.booking;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Базовый класс интеграционных тестов booking-service: один общий контейнер PostgreSQL (Testcontainers)
 * на весь прогон и MockMvc без сетевого порта. Config Server и Eureka отключены, а настройки,
 * которые в рабочем режиме отдаёт config-repo (JPA, пагинация), заданы здесь.
 */
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.fail-fast=false",
        "eureka.client.enabled=false",
        "spring.jpa.open-in-view=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.data.web.pageable.default-page-size=20",
        "spring.data.web.pageable.max-page-size=50"
})
public abstract class AbstractIntegrationTest {

    /** Версия образа, имя БД, пользователь и пароль лежат в src/test/resources/test-database.properties. */
    private static final Properties DATABASE = loadDatabaseProperties();

    /** Контейнер живёт до конца прогона тестов; останавливает его Testcontainers (Ryuk), поэтому close() не нужен. */
    @SuppressWarnings("resource")
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
    private WebApplicationContext webApplicationContext;

    protected MockMvc mockMvc;

    @BeforeEach
    protected void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    protected long postAndGetId(String url, String json) throws Exception {
        String body = mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    protected static String bookingJson(long memberId, long classId) {
        return "{\"memberId\":%d,\"classId\":%d,\"note\":\"see you\"}".formatted(memberId, classId);
    }

    protected int bookingStatus(long memberId, long classId) throws Exception {
        return mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(memberId, classId)))
                .andReturn().getResponse().getStatus();
    }
}
