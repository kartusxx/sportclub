package ru.teamname.sportclub.booking.resilience;

import org.junit.jupiter.api.AfterEach;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import ru.teamname.sportclub.booking.AbstractIntegrationTest;

/**
 * Тесты настоящих Feign-клиентов: запросы реально уходят по HTTP на {@link StubServer}.
 * Адрес сервисов подставляется через SimpleDiscoveryClient вместо Eureka, то есть проходит весь рабочий путь:
 * Feign → LoadBalancer → Circuit Breaker → HTTP. Порты и контракты клиентов не мокаются.
 */
@TestPropertySource(locations = "classpath:booking-feign-test.properties")
public abstract class AbstractFeignStubTest extends AbstractIntegrationTest {

    protected static final StubServer STUB = StubServer.start();

    @DynamicPropertySource
    static void stubbedServices(DynamicPropertyRegistry registry) {
        String uri = "http://127.0.0.1:" + STUB.port();
        registry.add("spring.cloud.discovery.client.simple.instances.member-service[0].uri", () -> uri);
        registry.add("spring.cloud.discovery.client.simple.instances.catalog-service[0].uri", () -> uri);
    }

    @AfterEach
    void resetStub() {
        STUB.reset();
    }
}
