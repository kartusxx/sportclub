package ru.teamname.sportclub.catalog.mapper;

import ru.teamname.sportclub.catalog.dto.DisciplineRequest;
import ru.teamname.sportclub.catalog.dto.DisciplineResponse;
import ru.teamname.sportclub.catalog.entity.Discipline;

public final class DisciplineMapper {

    private DisciplineMapper() {
    }

    public static DisciplineResponse toResponse(Discipline discipline) {
        return new DisciplineResponse(discipline.getId(), discipline.getName(), discipline.getDescription());
    }

    public static Discipline toEntity(DisciplineRequest request) {
        Discipline discipline = new Discipline();
        apply(request, discipline);
        return discipline;
    }

    public static void apply(DisciplineRequest request, Discipline discipline) {
        discipline.setName(request.name());
        discipline.setDescription(request.description());
    }
}
