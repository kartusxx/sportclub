package ru.teamname.sportclub.booking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.teamname.sportclub.booking.dto.BookingResponse;
import ru.teamname.sportclub.booking.service.BookingService;

import java.util.List;

/**
 * Записи на занятие. Путь /api/classes/{id}/bookings начинается с префикса catalog-service,
 * но данные принадлежат booking-service: на шаге 7 gateway направит этот маршрут сюда.
 */
@RestController
@RequestMapping("/api/classes/{classId}/bookings")
@Tag(name = "Записи на занятия", description = "Связь участников и занятий (Many to Many с доп. полями)")
@RequiredArgsConstructor
public class ClassBookingsController {

    private final BookingService bookingService;

    @GetMapping
    @Operation(summary = "Записи на занятие (One to Many)",
            description = "Пагинация (максимум 50 записей на страницу). Общее количество — в заголовке X-Total-Count")
    public ResponseEntity<List<BookingResponse>> findByClass(
            @PathVariable Long classId, @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return Responses.page(bookingService.findByClass(classId, pageable));
    }
}
