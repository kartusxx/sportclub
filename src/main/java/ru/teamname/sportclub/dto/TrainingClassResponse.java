package ru.teamname.sportclub.dto;

import ru.teamname.sportclub.enums.ClassLevel;

import java.time.LocalDateTime;

public record TrainingClassResponse(
        Long id,
        String title,
        String description,
        Long trainerId,
        String trainerName,
        Long disciplineId,
        String disciplineName,
        String room,
        LocalDateTime startsAt,
        Integer durationMinutes,
        Integer capacity,
        ClassLevel level) {
}
