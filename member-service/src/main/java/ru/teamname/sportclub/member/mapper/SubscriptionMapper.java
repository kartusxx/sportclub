package ru.teamname.sportclub.member.mapper;

import ru.teamname.sportclub.member.dto.SubscriptionResponse;
import ru.teamname.sportclub.member.entity.Subscription;

public final class SubscriptionMapper {

    private SubscriptionMapper() {
    }

    public static SubscriptionResponse toResponse(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getMember().getId(),
                subscription.getMember().getFullName(),
                subscription.getPlan().getId(),
                subscription.getPlan().getName(),
                subscription.getStartDate(),
                subscription.getEndDate(),
                subscription.getStatus());
    }
}
