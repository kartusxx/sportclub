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
import ru.teamname.sportclub.member.dto.MemberRequest;
import ru.teamname.sportclub.member.dto.MemberResponse;
import ru.teamname.sportclub.member.dto.SubscriptionResponse;
import ru.teamname.sportclub.member.service.MemberService;
import ru.teamname.sportclub.member.service.SubscriptionService;
import ru.teamname.sportclub.member.web.Blocking;
import ru.teamname.sportclub.member.web.PageQuery;
import ru.teamname.sportclub.member.web.Responses;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/members")
@Tag(name = "Участники", description = "Управление участниками клуба")
@RequiredArgsConstructor
public class MemberController {

    private static final Set<String> SORTABLE = Set.of("id", "firstName", "lastName", "email", "status", "createdAt");
    private static final Set<String> SUBSCRIPTION_SORTABLE = Set.of("id", "startDate", "endDate", "status");

    private final MemberService memberService;
    private final SubscriptionService subscriptionService;

    @GetMapping
    @Operation(summary = "Список участников",
            description = "Пагинация: page, size (максимум 50), sort=поле[,asc|desc]. "
                    + "Общее количество — в заголовке X-Total-Count")
    public Mono<ResponseEntity<List<MemberResponse>>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        PageQuery query = PageQuery.of(page, size, sort, "id", SORTABLE);
        return Blocking.call(() -> memberService.findAll(query.toPageable())).map(Responses::page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить участника по id")
    public Mono<MemberResponse> findById(@PathVariable Long id) {
        return Blocking.call(() -> memberService.findById(id));
    }

    @GetMapping("/{id}/subscriptions")
    @Operation(summary = "Абонементы участника (One to Many)")
    public Mono<ResponseEntity<List<SubscriptionResponse>>> findSubscriptions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        PageQuery query = PageQuery.of(page, size, sort, "id", SUBSCRIPTION_SORTABLE);
        return Blocking.call(() -> subscriptionService.findByMember(id, query.toPageable()))
                .map(Responses::page);
    }

    @PostMapping
    @Operation(summary = "Создать участника")
    public Mono<ResponseEntity<MemberResponse>> create(@Valid @RequestBody MemberRequest request,
                                                       ServerHttpRequest http) {
        return Blocking.call(() -> memberService.create(request))
                .map(created -> Responses.created(created, created.id(), http));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить участника")
    public Mono<MemberResponse> update(@PathVariable Long id, @Valid @RequestBody MemberRequest request) {
        return Blocking.call(() -> memberService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить участника")
    public Mono<ResponseEntity<Void>> delete(@PathVariable Long id) {
        return Blocking.run(() -> memberService.delete(id)).thenReturn(Responses.noContent());
    }
}
