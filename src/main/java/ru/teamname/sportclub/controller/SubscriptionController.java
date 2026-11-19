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
import ru.teamname.sportclub.dto.SubscriptionRequest;
import ru.teamname.sportclub.dto.SubscriptionResponse;
import ru.teamname.sportclub.service.SubscriptionService;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@Tag(name = "Абонементы", description = "Покупка и управление абонементами")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    @Operation(summary = "Список абонементов",
            description = "Пагинация (максимум 50 записей на страницу). Общее количество — в заголовке X-Total-Count")
    public ResponseEntity<List<SubscriptionResponse>> findAll(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return Responses.page(subscriptionService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить абонемент по id")
    public SubscriptionResponse findById(@PathVariable Long id) {
        return subscriptionService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Купить абонемент (транзакция)",
            description = "Если у участника уже есть действующий абонемент, новый начинается сразу после него")
    public ResponseEntity<SubscriptionResponse> purchase(@Valid @RequestBody SubscriptionRequest request) {
        SubscriptionResponse created = subscriptionService.purchase(request);
        return Responses.created(created, created.id());
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Отменить действующий абонемент")
    public SubscriptionResponse cancel(@PathVariable Long id) {
        return subscriptionService.cancel(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить абонемент")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        subscriptionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
