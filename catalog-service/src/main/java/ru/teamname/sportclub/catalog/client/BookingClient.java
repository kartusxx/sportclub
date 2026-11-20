package ru.teamname.sportclub.catalog.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** HTTP-клиент booking-service (обратная зависимость catalog → booking). Блокирующий. */
@FeignClient(name = "booking-service", contextId = "bookingClient",
        fallbackFactory = BookingClientFallbackFactory.class)
public interface BookingClient {

    @GetMapping("/internal/bookings/class/{classId}/booked-count")
    CountView bookedCount(@PathVariable("classId") Long classId);

    @DeleteMapping("/internal/bookings/class/{classId}")
    void deleteByClass(@PathVariable("classId") Long classId);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CountView(long count) {
    }
}
