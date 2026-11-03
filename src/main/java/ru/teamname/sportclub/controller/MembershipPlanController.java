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
import ru.teamname.sportclub.dto.MembershipPlanRequest;
import ru.teamname.sportclub.dto.MembershipPlanResponse;
import ru.teamname.sportclub.service.MembershipPlanService;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
@Tag(name = "Тарифы", description = "Тарифы абонементов")
public class MembershipPlanController {

    private final MembershipPlanService planService;

    public MembershipPlanController(MembershipPlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    @Operation(summary = "Список тарифов",
            description = "Пагинация (максимум 50 записей на страницу). Общее количество — в заголовке X-Total-Count")
    public ResponseEntity<List<MembershipPlanResponse>> findAll(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return Responses.page(planService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить тариф по id")
    public MembershipPlanResponse findById(@PathVariable Long id) {
        return planService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Создать тариф")
    public ResponseEntity<MembershipPlanResponse> create(@Valid @RequestBody MembershipPlanRequest request) {
        MembershipPlanResponse created = planService.create(request);
        return Responses.created(created, created.id());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить тариф")
    public MembershipPlanResponse update(@PathVariable Long id, @Valid @RequestBody MembershipPlanRequest request) {
        return planService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить тариф")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        planService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
