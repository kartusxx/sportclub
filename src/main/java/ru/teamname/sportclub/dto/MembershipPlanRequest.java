package ru.teamname.sportclub.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import ru.teamname.sportclub.enums.PlanType;

import java.math.BigDecimal;

public record MembershipPlanRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull PlanType type,
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal price,
        @NotNull @Positive Integer durationDays) {
}
