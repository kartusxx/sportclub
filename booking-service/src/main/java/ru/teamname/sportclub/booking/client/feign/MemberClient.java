package ru.teamname.sportclub.booking.client.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** HTTP-клиент member-service. Адрес берётся из Eureka по имени сервиса. */
@FeignClient(name = "member-service", contextId = "memberClient",
        fallbackFactory = MemberClientFallbackFactory.class)
public interface MemberClient {

    @GetMapping("/internal/members/{id}")
    MemberView getMember(@PathVariable("id") Long id);

    /** Дата передаётся строкой ISO (yyyy-MM-dd), чтобы формат не зависел от локали. */
    @GetMapping("/internal/members/{id}/coverage")
    CoverageView coverage(@PathVariable("id") Long id, @RequestParam("date") String date);
}
