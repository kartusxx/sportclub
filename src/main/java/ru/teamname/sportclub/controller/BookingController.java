package ru.teamname.sportclub.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.teamname.sportclub.dto.BookingRequest;
import ru.teamname.sportclub.dto.BookingResponse;
import ru.teamname.sportclub.service.BookingService;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Записи на занятия", description = "Связь участников и занятий (Many to Many с доп. полями)")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    @Operation(summary = "Список записей",
            description = "Пагинация (максимум 50 записей на страницу). Общее количество — в заголовке X-Total-Count")
    public ResponseEntity<List<BookingResponse>> findAll(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return Responses.page(bookingService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить запись по id")
    public BookingResponse findById(@PathVariable Long id) {
        return bookingService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Записаться на занятие (транзакция)",
            description = "Проверяет абонемент, дубликаты и наличие свободных мест")
    public ResponseEntity<BookingResponse> book(@Valid @RequestBody BookingRequest request) {
        BookingResponse created = bookingService.book(request);
        return Responses.created(created, created.id());
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Отменить запись")
    public BookingResponse cancel(@PathVariable Long id) {
        return bookingService.cancel(id);
    }

    @PostMapping("/{id}/attend")
    @Operation(summary = "Отметить посещение занятия")
    public BookingResponse attend(@PathVariable Long id) {
        return bookingService.markAttended(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить запись")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
