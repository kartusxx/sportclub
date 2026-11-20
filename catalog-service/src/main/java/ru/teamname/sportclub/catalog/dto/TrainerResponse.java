package ru.teamname.sportclub.catalog.dto;

import java.util.List;

public record TrainerResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        Integer experienceYears,
        String bio,
        List<DisciplineResponse> disciplines) {
}
