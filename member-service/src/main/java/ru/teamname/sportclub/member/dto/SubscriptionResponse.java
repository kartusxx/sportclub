package ru.teamname.sportclub.member.dto;

import ru.teamname.sportclub.member.enums.SubscriptionStatus;

import java.time.LocalDate;

public record SubscriptionResponse(
        Long id,
        Long memberId,
        String memberName,
        Long planId,
        String planName,
        LocalDate startDate,
        LocalDate endDate,
        SubscriptionStatus status) {
}
