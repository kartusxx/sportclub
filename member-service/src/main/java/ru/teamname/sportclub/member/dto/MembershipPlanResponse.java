package ru.teamname.sportclub.member.dto;

import ru.teamname.sportclub.member.enums.PlanType;

import java.math.BigDecimal;

public record MembershipPlanResponse(Long id, String name, PlanType type, BigDecimal price, Integer durationDays) {
}
