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
import ru.teamname.sportclub.dto.MemberRequest;
import ru.teamname.sportclub.dto.MemberResponse;
import ru.teamname.sportclub.dto.SubscriptionResponse;
import ru.teamname.sportclub.service.MemberService;
import ru.teamname.sportclub.service.SubscriptionService;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@Tag(name = "Участники", description = "Управление участниками клуба")
public class MemberController {

    private final MemberService memberService;
    private final SubscriptionService subscriptionService;

    public MemberController(MemberService memberService, SubscriptionService subscriptionService) {
        this.memberService = memberService;
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    @Operation(summary = "Список участников",
            description = "Пагинация (максимум 50 записей на страницу). Общее количество — в заголовке X-Total-Count")
    public ResponseEntity<List<MemberResponse>> findAll(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return Responses.page(memberService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить участника по id")
    public MemberResponse findById(@PathVariable Long id) {
        return memberService.findById(id);
    }

    @GetMapping("/{id}/subscriptions")
    @Operation(summary = "Абонементы участника (One to Many)")
    public ResponseEntity<List<SubscriptionResponse>> findSubscriptions(
            @PathVariable Long id, @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return Responses.page(subscriptionService.findByMember(id, pageable));
    }

    @PostMapping
    @Operation(summary = "Создать участника")
    public ResponseEntity<MemberResponse> create(@Valid @RequestBody MemberRequest request) {
        MemberResponse created = memberService.create(request);
        return Responses.created(created, created.id());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить участника")
    public MemberResponse update(@PathVariable Long id, @Valid @RequestBody MemberRequest request) {
        return memberService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить участника")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        memberService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
