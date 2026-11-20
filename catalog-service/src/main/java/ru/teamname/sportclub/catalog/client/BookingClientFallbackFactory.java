package ru.teamname.sportclub.catalog.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/** Что делает {@link BookingClient}, когда booking-service недоступен или цепь разомкнута. */
@Component
public class BookingClientFallbackFactory implements FallbackFactory<BookingClient> {

    private static final String SERVICE = "booking-service";

    @Override
    public BookingClient create(Throwable cause) {
        return new BookingClient() {
            @Override
            public CountView bookedCount(Long classId) {
                throw FeignErrors.fallback(SERVICE, cause);
            }

            @Override
            public void deleteByClass(Long classId) {
                throw FeignErrors.fallback(SERVICE, cause);
            }
        };
    }
}
