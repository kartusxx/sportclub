package ru.teamname.sportclub.member.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** HTTP-клиент booking-service (обратная зависимость: проверка записей перед удалением участника). */
@FeignClient(name = "booking-service", contextId = "bookingClient",
        fallbackFactory = BookingClientFallbackFactory.class)
public interface BookingClient {

    @GetMapping("/internal/bookings/member/{memberId}/exists")
    ExistsView memberHasBookings(@PathVariable("memberId") Long memberId);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ExistsView(boolean exists) {
    }
}
