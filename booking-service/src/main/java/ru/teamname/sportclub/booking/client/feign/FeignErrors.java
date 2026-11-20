package ru.teamname.sportclub.booking.client.feign;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import ru.teamname.sportclub.booking.exception.ServiceUnavailableException;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/** Единое место превращения сбоев Feign в понятный клиенту ответ 503. */
@Slf4j
final class FeignErrors {

    private FeignErrors() {
    }

    static ServiceUnavailableException unavailable(String service, FeignException cause) {
        log.warn("Вызов {} завершился ошибкой: {}", service, cause.getMessage());
        return new ServiceUnavailableException(message(service));
    }

    /**
     * Что делает fallback при сбое вызова. Ответы 4xx (например, 404 «участник не найден») —
     * это ответ сервиса, а не его отказ: они пробрасываются как есть и обрабатываются вызывающим кодом.
     * Всё остальное (отказ соединения, таймаут, 5xx, разомкнутая цепь) — это 503.
     */
    static RuntimeException fallback(String service, Throwable cause) {
        Throwable root = unwrap(cause);
        if (root instanceof FeignException.FeignClientException clientError) {
            return clientError;
        }
        log.warn("Fallback для {}: {}: {}", service, root.getClass().getSimpleName(), root.getMessage());
        return new ServiceUnavailableException(message(service));
    }

    private static Throwable unwrap(Throwable cause) {
        Throwable current = cause;
        while ((current instanceof ExecutionException || current instanceof CompletionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static String message(String service) {
        return "Сервис " + service + " временно недоступен, повторите запрос позже";
    }
}
