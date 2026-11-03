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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.teamname.sportclub.dto.BookingResponse;
import ru.teamname.sportclub.dto.SliceResponse;
import ru.teamname.sportclub.dto.TrainingClassRequest;
import ru.teamname.sportclub.dto.TrainingClassResponse;
import ru.teamname.sportclub.service.BookingService;
import ru.teamname.sportclub.service.TrainingClassService;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
@Tag(name = "Занятия", description = "Расписание групповых занятий")
public class TrainingClassController {

    private final TrainingClassService classService;
    private final BookingService bookingService;

    public TrainingClassController(TrainingClassService classService, BookingService bookingService) {
        this.classService = classService;
        this.bookingService = bookingService;
    }

    @GetMapping
    @Operation(summary = "Лента занятий (бесконечная прокрутка)",
            description = "Максимум 50 записей на страницу. Общее количество не считается: "
                    + "клиент ориентируется по полю hasNext")
    public SliceResponse<TrainingClassResponse> findFeed(
            @PageableDefault(size = 20, sort = "startsAt") Pageable pageable) {
        return classService.findFeed(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить занятие по id")
    public TrainingClassResponse findById(@PathVariable Long id) {
        return classService.findById(id);
    }

    @GetMapping("/{id}/bookings")
    @Operation(summary = "Записи на занятие (One to Many)")
    public ResponseEntity<List<BookingResponse>> findBookings(
            @PathVariable Long id, @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return Responses.page(bookingService.findByClass(id, pageable));
    }

    @PostMapping
    @Operation(summary = "Создать занятие")
    public ResponseEntity<TrainingClassResponse> create(@Valid @RequestBody TrainingClassRequest request) {
        TrainingClassResponse created = classService.create(request);
        return Responses.created(created, created.id());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить занятие")
    public TrainingClassResponse update(@PathVariable Long id, @Valid @RequestBody TrainingClassRequest request) {
        return classService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить занятие вместе с записями (транзакция)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        classService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
