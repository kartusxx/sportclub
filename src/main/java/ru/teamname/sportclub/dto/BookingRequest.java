package ru.teamname.sportclub.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookingRequest(
        @NotNull Long memberId,
        @NotNull Long classId,
        @Size(max = 255) String note) {
}
