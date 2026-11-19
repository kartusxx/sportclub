package ru.teamname.sportclub.mapper;

import ru.teamname.sportclub.dto.TrainerRequest;
import ru.teamname.sportclub.dto.TrainerResponse;
import ru.teamname.sportclub.entity.Discipline;
import ru.teamname.sportclub.entity.Trainer;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public final class TrainerMapper {

    private TrainerMapper() {
    }

    public static TrainerResponse toResponse(Trainer trainer) {
        return new TrainerResponse(trainer.getId(), trainer.getFirstName(), trainer.getLastName(),
                trainer.getEmail(), trainer.getExperienceYears(), trainer.getBio(),
                trainer.getDisciplines().stream()
                        .sorted(Comparator.comparing(Discipline::getName))
                        .map(DisciplineMapper::toResponse)
                        .toList());
    }

    /** Переносит скалярные поля; направления выставляются отдельно, т.к. их нужно загрузить из БД. */
    public static void apply(TrainerRequest request, Trainer trainer, Set<Discipline> disciplines) {
        trainer.setFirstName(request.firstName());
        trainer.setLastName(request.lastName());
        trainer.setEmail(request.email());
        trainer.setExperienceYears(request.experienceYears());
        trainer.setBio(request.bio());
        trainer.setDisciplines(new HashSet<>(disciplines));
    }
}
