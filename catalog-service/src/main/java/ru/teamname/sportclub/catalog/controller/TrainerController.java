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
import ru.teamname.sportclub.catalog.dto.TrainerRequest;
import ru.teamname.sportclub.catalog.dto.TrainerResponse;
import ru.teamname.sportclub.catalog.service.TrainerService;
import ru.teamname.sportclub.catalog.web.PageQuery;
import ru.teamname.sportclub.catalog.web.Responses;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/trainers")
@Tag(name = "Тренеры", description = "Тренеры и их направления (Many to Many)")
@RequiredArgsConstructor
public class TrainerController {

    private static final Set<String> SORTABLE = Set.of("id", "firstName", "lastName", "email", "experienceYears");

    private final TrainerService trainerService;

    @GetMapping
    @Operation(summary = "Список тренеров",
            description = "Пагинация: page, size (максимум 50), sort=поле[,asc|desc]. "
                    + "Общее количество — в заголовке X-Total-Count")
    public Mono<ResponseEntity<List<TrainerResponse>>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        PageQuery query = PageQuery.of(page, size, sort, "id", SORTABLE);
        return trainerService.findAll(query).map(Responses::page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить тренера по id")
    public Mono<TrainerResponse> findById(@PathVariable Long id) {
        return trainerService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Создать тренера (транзакция)",
            description = "В disciplineIds передаются id направлений тренера")
    public Mono<ResponseEntity<TrainerResponse>> create(@Valid @RequestBody TrainerRequest request,
                                                        ServerHttpRequest http) {
        return trainerService.create(request).map(created -> Responses.created(created, created.id(), http));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить тренера и список его направлений (транзакция)")
    public Mono<TrainerResponse> update(@PathVariable Long id, @Valid @RequestBody TrainerRequest request) {
        return trainerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить тренера")
    public Mono<ResponseEntity<Void>> delete(@PathVariable Long id) {
        return trainerService.delete(id).thenReturn(Responses.noContent());
    }
}
