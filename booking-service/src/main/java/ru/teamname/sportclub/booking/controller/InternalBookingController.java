package ru.teamname.sportclub.booking.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.teamname.sportclub.booking.dto.CountResponse;
import ru.teamname.sportclub.booking.dto.ExistsResponse;
import ru.teamname.sportclub.booking.service.BookingService;

/**
 * Внутренний API для member-service и catalog-service. В Swagger не показывается и через gateway
 * не публикуется (маршрут /internal/** на шаге 7 не создаётся).
 */
@Hidden
@RestController
@RequestMapping("/internal/bookings")
@RequiredArgsConstructor
public class InternalBookingController {

    private final BookingService bookingService;

    /** Сколько активных записей на занятии (для проверки вместимости при изменении занятия). */
    @GetMapping("/class/{classId}/booked-count")
    public CountResponse bookedCount(@PathVariable Long classId) {
        return new CountResponse(bookingService.countBooked(classId));
    }

    /** Есть ли у участника хоть одна запись (для запрета удаления участника). */
    @GetMapping("/member/{memberId}/exists")
    public ExistsResponse memberHasBookings(@PathVariable Long memberId) {
        return new ExistsResponse(bookingService.hasBookings(memberId));
    }

    /** Удаляет все записи занятия; идемпотентно: повторный вызов ничего не удалит и вернёт 204. */
    @DeleteMapping("/class/{classId}")
    public ResponseEntity<Void> deleteByClass(@PathVariable Long classId) {
        bookingService.deleteByClass(classId);
        return ResponseEntity.noContent().build();
    }
}
