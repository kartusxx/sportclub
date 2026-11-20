package ru.teamname.sportclub.catalog.client;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import ru.teamname.sportclub.catalog.exception.ServiceUnavailableException;

/**
 * Реактивная обёртка над блокирующим Feign-клиентом: вызов уходит на boundedElastic,
 * чтобы не блокировать event-loop Netty.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingGateway {

    private final BookingClient bookingClient;

    /** Сколько участников записано на занятие. Нет ответа — 503 (нельзя безопасно проверить вместимость). */
    public Mono<Long> bookedCount(Long classId) {
        return Mono.fromCallable(() -> bookingClient.bookedCount(classId).count())
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(FeignException.class, e -> {
                    log.warn("booking-service: не удалось получить число записей: {}", e.getMessage());
                    return new ServiceUnavailableException(
                            "Не удалось проверить записи на занятие: booking-service недоступен, повторите запрос позже");
                });
    }

    /**
     * Удаляет записи удалённого занятия. Best-effort: сбой не откатывает удаление занятия
     * (разные БД, общей транзакции нет). Оставшиеся записи безвредны: booking-service
     * не покажет их без существующего занятия и не даст записаться на него.
     */
    public Mono<Void> deleteBookings(Long classId) {
        return Mono.<Void>fromRunnable(() -> bookingClient.deleteByClass(classId))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(FeignException.class, e -> {
                    log.warn("booking-service: записи занятия {} не удалены, останутся осиротевшими: {}",
                            classId, e.getMessage());
                    return Mono.empty();
                });
    }
}
