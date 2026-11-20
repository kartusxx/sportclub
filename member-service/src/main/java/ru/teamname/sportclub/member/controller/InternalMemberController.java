package ru.teamname.sportclub.member.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.teamname.sportclub.member.dto.CoverageResponse;
import ru.teamname.sportclub.member.dto.MemberSummary;
import ru.teamname.sportclub.member.service.MemberService;
import ru.teamname.sportclub.member.service.SubscriptionService;
import ru.teamname.sportclub.member.web.Blocking;
import ru.teamname.sportclub.member.web.Responses;

import java.time.LocalDate;

/**
 * Внутренний API для booking-service. В Swagger не показывается и через gateway не публикуется
 * (маршрут /internal/** на шаге 7 не создаётся).
 */
@Hidden
@RestController
@RequestMapping("/internal/members")
@RequiredArgsConstructor
public class InternalMemberController {

    private final MemberService memberService;
    private final SubscriptionService subscriptionService;

    @GetMapping("/{id}")
    public Mono<ResponseEntity<MemberSummary>> summary(@PathVariable Long id) {
        return Blocking.call(() -> memberService.findById(id))
                .map(member -> new MemberSummary(member.id(), member.firstName() + " " + member.lastName(),
                        member.status().name()))
                .map(Responses::ok);
    }

    /** Есть ли у участника действующий абонемент, покрывающий дату (замена countCovering из монолита). */
    @GetMapping("/{id}/coverage")
    public Mono<ResponseEntity<CoverageResponse>> coverage(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Blocking.call(() -> new CoverageResponse(subscriptionService.hasCoverage(id, date)))
                .map(Responses::ok);
    }
}
