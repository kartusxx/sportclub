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
import ru.teamname.sportclub.dto.DisciplineRequest;
import ru.teamname.sportclub.dto.DisciplineResponse;
import ru.teamname.sportclub.service.DisciplineService;

import java.util.List;

@RestController
@RequestMapping("/api/disciplines")
@Tag(name = "Направления", description = "Спортивные направления клуба")
public class DisciplineController {

    private final DisciplineService disciplineService;

    public DisciplineController(DisciplineService disciplineService) {
        this.disciplineService = disciplineService;
    }

    @GetMapping
    @Operation(summary = "Список направлений",
            description = "Пагинация (максимум 50 записей на страницу). Общее количество — в заголовке X-Total-Count")
    public ResponseEntity<List<DisciplineResponse>> findAll(
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return Responses.page(disciplineService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить направление по id")
    public DisciplineResponse findById(@PathVariable Long id) {
        return disciplineService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Создать направление")
    public ResponseEntity<DisciplineResponse> create(@Valid @RequestBody DisciplineRequest request) {
        DisciplineResponse created = disciplineService.create(request);
        return Responses.created(created, created.id());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить направление")
    public DisciplineResponse update(@PathVariable Long id, @Valid @RequestBody DisciplineRequest request) {
        return disciplineService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить направление")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        disciplineService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
