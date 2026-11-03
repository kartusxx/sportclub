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
import ru.teamname.sportclub.dto.TrainerRequest;
import ru.teamname.sportclub.dto.TrainerResponse;
import ru.teamname.sportclub.service.TrainerService;

import java.util.List;

@RestController
@RequestMapping("/api/trainers")
@Tag(name = "Тренеры", description = "Тренеры и их направления (Many to Many)")
public class TrainerController {

    private final TrainerService trainerService;

    public TrainerController(TrainerService trainerService) {
        this.trainerService = trainerService;
    }

    @GetMapping
    @Operation(summary = "Список тренеров",
            description = "Пагинация (максимум 50 записей на страницу). Общее количество — в заголовке X-Total-Count")
    public ResponseEntity<List<TrainerResponse>> findAll(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return Responses.page(trainerService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить тренера по id")
    public TrainerResponse findById(@PathVariable Long id) {
        return trainerService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Создать тренера", description = "В disciplineIds передаются id направлений тренера")
    public ResponseEntity<TrainerResponse> create(@Valid @RequestBody TrainerRequest request) {
        TrainerResponse created = trainerService.create(request);
        return Responses.created(created, created.id());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить тренера и список его направлений")
    public TrainerResponse update(@PathVariable Long id, @Valid @RequestBody TrainerRequest request) {
        return trainerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить тренера")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        trainerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
