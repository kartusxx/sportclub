package ru.teamname.sportclub.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.teamname.sportclub.enums.ClassLevel;

import java.time.LocalDateTime;

public record TrainingClassRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 1000) String description,
        @NotNull Long trainerId,
        @NotNull Long disciplineId,
        @NotBlank @Size(max = 50) String room,
        @NotNull @Future LocalDateTime startsAt,
        @NotNull @Min(15) @Max(240) Integer durationMinutes,
        @NotNull @Min(1) @Max(200) Integer capacity,
        @NotNull ClassLevel level) {
}
