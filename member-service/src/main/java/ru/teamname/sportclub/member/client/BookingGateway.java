package ru.teamname.sportclub.member.client;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.teamname.sportclub.member.exception.ServiceUnavailableException;

/** Обёртка над Feign-клиентом booking-service: превращает сбои вызова в понятный ответ 503. */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingGateway {

    private final BookingClient bookingClient;

    /**
     * Есть ли у участника записи на занятия. Если booking-service недоступен, ответить нельзя,
     * поэтому операция отклоняется (лучше отказать в удалении, чем оставить «висящие» записи).
     */
    public boolean hasBookings(Long memberId) {
        try {
            return bookingClient.memberHasBookings(memberId).exists();
        } catch (FeignException e) {
            log.warn("Вызов booking-service завершился ошибкой: {}", e.getMessage());
            throw new ServiceUnavailableException(
                    "Не удалось проверить записи участника: booking-service недоступен, повторите запрос позже");
        }
    }
}
