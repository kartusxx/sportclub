package ru.teamname.sportclub;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Базовый класс интеграционных тестов: один общий контейнер PostgreSQL (Testcontainers)
 * на весь прогон тестов и набор помощников для создания тестовых данных через REST API.
 * Тесты используют уникальные данные, поэтому не мешают друг другу.
 * Образ, имя базы, логин и пароль берутся из src/test/resources/application-test.properties (профиль "test").
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    private static final String TEST_CONFIG = "application-test.properties";

    static final PostgreSQLContainer POSTGRES = createPostgresContainer();

    static {
        POSTGRES.start();
    }

    /** Контейнер создаётся до запуска Spring-контекста, поэтому настройки читаются из файла напрямую. */
    private static PostgreSQLContainer createPostgresContainer() {
        Properties config = loadTestConfig();
        return new PostgreSQLContainer(required(config, "test.postgres.image"))
                .withDatabaseName(required(config, "test.postgres.database"))
                .withUsername(required(config, "spring.datasource.username"))
                .withPassword(required(config, "spring.datasource.password"));
    }

    private static Properties loadTestConfig() {
        try {
            return PropertiesLoaderUtils.loadProperties(new ClassPathResource(TEST_CONFIG));
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать " + TEST_CONFIG, e);
        }
    }

    private static String required(Properties config, String key) {
        String value = config.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("В " + TEST_CONFIG + " не задано свойство " + key);
        }
        return value.trim();
    }

    /** Порт контейнера случайный, поэтому URL известен только после его запуска. */
    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    protected MockMvc mockMvc;

    @BeforeEach
    protected void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    protected static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    protected static LocalDateTime futureStart() {
        return LocalDateTime.now().plusDays(1).withNano(0);
    }

    protected long postAndGetId(String url, String json) throws Exception {
        String body = mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    protected long createMember() throws Exception {
        return createMember("ACTIVE");
    }

    protected long createMember(String status) throws Exception {
        String json = """
                {"firstName":"Ivan","lastName":"Petrov","email":"%s@example.com",
                 "phone":"+7 900 123-45-67","status":"%s"}
                """.formatted(unique("member"), status);
        return postAndGetId("/api/members", json);
    }

    protected long createPlan(int durationDays) throws Exception {
        String json = """
                {"name":"%s","type":"MONTHLY","price":1500.00,"durationDays":%d}
                """.formatted(unique("plan"), durationDays);
        return postAndGetId("/api/plans", json);
    }

    protected long createDiscipline() throws Exception {
        String json = """
                {"name":"%s","description":"test discipline"}
                """.formatted(unique("discipline"));
        return postAndGetId("/api/disciplines", json);
    }

    protected long createTrainer(long... disciplineIds) throws Exception {
        String ids = LongStream.of(disciplineIds).mapToObj(String::valueOf).collect(Collectors.joining(","));
        String json = """
                {"firstName":"Anna","lastName":"Smirnova","email":"%s@example.com",
                 "experienceYears":5,"bio":"coach","disciplineIds":[%s]}
                """.formatted(unique("trainer"), ids);
        return postAndGetId("/api/trainers", json);
    }

    protected long createClass(long trainerId, long disciplineId, int capacity, LocalDateTime startsAt)
            throws Exception {
        String json = """
                {"title":"%s","description":"test class","trainerId":%d,"disciplineId":%d,"room":"Hall 1",
                 "startsAt":"%s","durationMinutes":60,"capacity":%d,"level":"BEGINNER"}
                """.formatted(unique("class"), trainerId, disciplineId, startsAt, capacity);
        return postAndGetId("/api/classes", json);
    }

    protected long purchaseSubscription(long memberId, long planId) throws Exception {
        return postAndGetId("/api/subscriptions", """
                {"memberId":%d,"planId":%d}
                """.formatted(memberId, planId));
    }

    protected static String bookingJson(long memberId, long classId) {
        return """
                {"memberId":%d,"classId":%d,"note":"see you"}
                """.formatted(memberId, classId);
    }
}
