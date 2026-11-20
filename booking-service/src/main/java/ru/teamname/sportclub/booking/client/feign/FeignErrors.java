package ru.teamname.sportclub.booking.client.feign;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import ru.teamname.sportclub.booking.exception.ServiceUnavailableException;

/** Единое место превращения сбоев Feign в понятный клиенту ответ 503. */
@Slf4j
final class FeignErrors {

    private FeignErrors() {
    }

    static ServiceUnavailableException unavailable(String service, FeignException cause) {
        log.warn("Вызов {} завершился ошибкой: {}", service, cause.getMessage());
        return new ServiceUnavailableException("Сервис " + service + " временно недоступен, повторите запрос позже");
    }
}
