package ru.teamname.sportclub.catalog.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.teamname.sportclub.catalog.dto.SliceResponse;
import ru.teamname.sportclub.catalog.dto.TrainingClassRequest;
import ru.teamname.sportclub.catalog.dto.TrainingClassResponse;
import ru.teamname.sportclub.catalog.service.TrainingClassService;
import ru.teamname.sportclub.catalog.web.PageQuery;
import ru.teamname.sportclub.catalog.web.Responses;

import java.util.Set;

@RestController
@RequestMapping("/api/classes")
@Tag(name = "Занятия", description = "Расписание групповых занятий")
@RequiredArgsConstructor
public class TrainingClassController {

    private static final Set<String> SORTABLE = Set.of("id", "title", "startsAt", "level", "capacity");

    private final TrainingClassService classService;

    @GetMapping
    @Operation(summary = "Лента занятий (бесконечная прокрутка)",
            description = "Пагинация: page, size (максимум 50), sort=поле[,asc|desc]. Общее количество "
                    + "не считается: клиент ориентируется по полю hasNext")
    public Mono<ResponseEntity<SliceResponse<TrainingClassResponse>>> findFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        return classService.findFeed(PageQuery.of(page, size, sort, "startsAt", SORTABLE))
                .map(Responses::ok);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить занятие по id")
    public Mono<ResponseEntity<TrainingClassResponse>> findById(@PathVariable Long id) {
        return classService.findById(id)
                .map(Responses::ok);
    }

    @PostMapping
    @Operation(summary = "Создать занятие")
    public Mono<ResponseEntity<TrainingClassResponse>> create(@Valid @RequestBody TrainingClassRequest request,
                                                              ServerHttpRequest http) {
        return classService.create(request).map(created -> Responses.created(created, created.id(), http));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить занятие")
    public Mono<ResponseEntity<TrainingClassResponse>> update(@PathVariable Long id,
                                              @Valid @RequestBody TrainingClassRequest request) {
        return classService.update(id, request)
                .map(Responses::ok);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить занятие")
    public Mono<ResponseEntity<Void>> delete(@PathVariable Long id) {
        return classService.delete(id).thenReturn(Responses.noContent());
    }
}
