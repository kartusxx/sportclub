package ru.teamname.sportclub.dto;

import ru.teamname.sportclub.enums.PlanType;

import java.math.BigDecimal;

public record MembershipPlanResponse(Long id, String name, PlanType type, BigDecimal price, Integer durationDays) {
}
