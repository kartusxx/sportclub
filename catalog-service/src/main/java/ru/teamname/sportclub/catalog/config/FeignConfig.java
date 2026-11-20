package ru.teamname.sportclub.catalog.config;

import feign.Retryer;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Включает Feign-клиенты и задаёт повтор неудачных вызовов. */
@Configuration(proxyBeanMethods = false)
@EnableFeignClients(basePackages = "ru.teamname.sportclub.catalog.client")
public class FeignConfig {

    /**
     * Повтор: одна дополнительная попытка через 200 мс, только при сетевых сбоях и таймаутах
     * (Feign повторяет лишь RetryableException; ответы 4xx/5xx не повторяются).
     * Все Feign-клиенты этого сервиса содержат только идемпотентные методы (GET, DELETE),
     * поэтому повтор безопасен. Клиент с POST получил бы отдельную конфигурацию с Retryer.NEVER_RETRY.
     * Повтор выполняется внутри Circuit Breaker: он видит итог вызова, а не каждую попытку.
     */
    @Bean
    Retryer feignRetryer() {
        return new Retryer.Default(200, 200, 2);
    }
}
