package ru.teamname.sportclub.dto;

import ru.teamname.sportclub.enums.SubscriptionStatus;

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
