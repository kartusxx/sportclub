package ru.teamname.sportclub.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DisciplineRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description) {
}
