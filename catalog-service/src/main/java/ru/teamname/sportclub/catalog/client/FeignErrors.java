package ru.teamname.sportclub.catalog.client;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import ru.teamname.sportclub.catalog.exception.ServiceUnavailableException;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/** Превращение сбоев Feign в понятный клиенту ответ 503. */
@Slf4j
final class FeignErrors {

    private FeignErrors() {
    }

    /**
     * Ответы 4xx — это ответ сервиса, а не его отказ: пробрасываются как есть.
     * Отказ соединения, таймаут, 5xx и разомкнутая цепь превращаются в 503.
     */
    static RuntimeException fallback(String service, Throwable cause) {
        Throwable root = cause;
        while ((root instanceof ExecutionException || root instanceof CompletionException)
                && root.getCause() != null) {
            root = root.getCause();
        }
        if (root instanceof FeignException.FeignClientException clientError) {
            return clientError;
        }
        log.warn("Fallback для {}: {}: {}", service, root.getClass().getSimpleName(), root.getMessage());
        return new ServiceUnavailableException(
                "Не удалось связаться с сервисом " + service + ", повторите запрос позже");
    }
}
