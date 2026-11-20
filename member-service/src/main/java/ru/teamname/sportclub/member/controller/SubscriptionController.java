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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.teamname.sportclub.member.dto.SubscriptionRequest;
import ru.teamname.sportclub.member.dto.SubscriptionResponse;
import ru.teamname.sportclub.member.service.SubscriptionService;
import ru.teamname.sportclub.member.web.Blocking;
import ru.teamname.sportclub.member.web.PageQuery;
import ru.teamname.sportclub.member.web.Responses;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/subscriptions")
@Tag(name = "Абонементы", description = "Покупка и управление абонементами")
@RequiredArgsConstructor
public class SubscriptionController {

    private static final Set<String> SORTABLE = Set.of("id", "startDate", "endDate", "status");

    private final SubscriptionService subscriptionService;

    @GetMapping
    @Operation(summary = "Список абонементов",
            description = "Пагинация: page, size (максимум 50), sort=поле[,asc|desc]. "
                    + "Общее количество — в заголовке X-Total-Count")
    public Mono<ResponseEntity<List<SubscriptionResponse>>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        PageQuery query = PageQuery.of(page, size, sort, "id", SORTABLE);
        return Blocking.call(() -> subscriptionService.findAll(query.toPageable())).map(Responses::page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить абонемент по id")
    public Mono<SubscriptionResponse> findById(@PathVariable Long id) {
        return Blocking.call(() -> subscriptionService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Купить абонемент (транзакция)",
            description = "Если у участника уже есть действующий абонемент, новый начинается сразу после него")
    public Mono<ResponseEntity<SubscriptionResponse>> purchase(@Valid @RequestBody SubscriptionRequest request,
                                                               ServerHttpRequest http) {
        return Blocking.call(() -> subscriptionService.purchase(request))
                .map(created -> Responses.created(created, created.id(), http));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Отменить действующий абонемент")
    public Mono<SubscriptionResponse> cancel(@PathVariable Long id) {
        return Blocking.call(() -> subscriptionService.cancel(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить абонемент")
    public Mono<ResponseEntity<Void>> delete(@PathVariable Long id) {
        return Blocking.run(() -> subscriptionService.delete(id)).thenReturn(Responses.noContent());
    }
}
