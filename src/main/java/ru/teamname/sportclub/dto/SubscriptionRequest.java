package ru.teamname.sportclub.dto;

import jakarta.validation.constraints.NotNull;

/** Запрос на покупку абонемента. */
public record SubscriptionRequest(
        @NotNull Long memberId,
        @NotNull Long planId) {
}
