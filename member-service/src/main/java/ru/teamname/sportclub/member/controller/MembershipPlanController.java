package ru.teamname.sportclub.member.controller;

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
import ru.teamname.sportclub.member.dto.MembershipPlanRequest;
import ru.teamname.sportclub.member.dto.MembershipPlanResponse;
import ru.teamname.sportclub.member.service.MembershipPlanService;
import ru.teamname.sportclub.member.web.Blocking;
import ru.teamname.sportclub.member.web.PageQuery;
import ru.teamname.sportclub.member.web.Responses;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/plans")
@Tag(name = "Тарифы", description = "Тарифы абонементов")
@RequiredArgsConstructor
public class MembershipPlanController {

    private static final Set<String> SORTABLE = Set.of("id", "name", "type", "price", "durationDays");

    private final MembershipPlanService planService;

    @GetMapping
    @Operation(summary = "Список тарифов",
            description = "Пагинация: page, size (максимум 50), sort=поле[,asc|desc]. "
                    + "Общее количество — в заголовке X-Total-Count")
    public Mono<ResponseEntity<List<MembershipPlanResponse>>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        PageQuery query = PageQuery.of(page, size, sort, "id", SORTABLE);
        return Blocking.call(() -> planService.findAll(query.toPageable())).map(Responses::page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить тариф по id")
    public Mono<MembershipPlanResponse> findById(@PathVariable Long id) {
        return Blocking.call(() -> planService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Создать тариф")
    public Mono<ResponseEntity<MembershipPlanResponse>> create(@Valid @RequestBody MembershipPlanRequest request,
                                                               ServerHttpRequest http) {
        return Blocking.call(() -> planService.create(request))
                .map(created -> Responses.created(created, created.id(), http));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить тариф")
    public Mono<MembershipPlanResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody MembershipPlanRequest request) {
        return Blocking.call(() -> planService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить тариф")
    public Mono<ResponseEntity<Void>> delete(@PathVariable Long id) {
        return Blocking.run(() -> planService.delete(id)).thenReturn(Responses.noContent());
    }
}
