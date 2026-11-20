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
import ru.teamname.sportclub.catalog.dto.DisciplineRequest;
import ru.teamname.sportclub.catalog.dto.DisciplineResponse;
import ru.teamname.sportclub.catalog.service.DisciplineService;
import ru.teamname.sportclub.catalog.web.PageQuery;
import ru.teamname.sportclub.catalog.web.Responses;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/disciplines")
@Tag(name = "Направления", description = "Спортивные направления клуба")
@RequiredArgsConstructor
public class DisciplineController {

    private static final Set<String> SORTABLE = Set.of("id", "name");

    private final DisciplineService disciplineService;

    @GetMapping
    @Operation(summary = "Список направлений",
            description = "Пагинация: page, size (максимум 50), sort=поле[,asc|desc]. "
                    + "Общее количество — в заголовке X-Total-Count")
    public Mono<ResponseEntity<List<DisciplineResponse>>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        PageQuery query = PageQuery.of(page, size, sort, "name", SORTABLE);
        return disciplineService.findAll(query).map(Responses::page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить направление по id")
    public Mono<DisciplineResponse> findById(@PathVariable Long id) {
        return disciplineService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Создать направление")
    public Mono<ResponseEntity<DisciplineResponse>> create(@Valid @RequestBody DisciplineRequest request,
                                                           ServerHttpRequest http) {
        return disciplineService.create(request)
                .map(created -> Responses.created(created, created.id(), http));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить направление")
    public Mono<DisciplineResponse> update(@PathVariable Long id, @Valid @RequestBody DisciplineRequest request) {
        return disciplineService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить направление")
    public Mono<ResponseEntity<Void>> delete(@PathVariable Long id) {
        return disciplineService.delete(id).thenReturn(Responses.noContent());
    }
}
