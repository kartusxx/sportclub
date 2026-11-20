package ru.teamname.sportclub.member.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/** Что делает {@link BookingClient}, когда booking-service недоступен или цепь разомкнута. */
@Component
public class BookingClientFallbackFactory implements FallbackFactory<BookingClient> {

    @Override
    public BookingClient create(Throwable cause) {
        return memberId -> {
            throw FeignErrors.fallback("booking-service", cause);
        };
    }
}
