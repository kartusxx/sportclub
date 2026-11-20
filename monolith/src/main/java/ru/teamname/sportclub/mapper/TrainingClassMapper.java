package ru.teamname.sportclub.mapper;

import ru.teamname.sportclub.dto.TrainingClassRequest;
import ru.teamname.sportclub.dto.TrainingClassResponse;
import ru.teamname.sportclub.entity.Discipline;
import ru.teamname.sportclub.entity.Trainer;
import ru.teamname.sportclub.entity.TrainingClass;

public final class TrainingClassMapper {

    private TrainingClassMapper() {
    }

    public static TrainingClassResponse toResponse(TrainingClass trainingClass) {
        return new TrainingClassResponse(
                trainingClass.getId(),
                trainingClass.getTitle(),
                trainingClass.getDescription(),
                trainingClass.getTrainer().getId(),
                trainingClass.getTrainer().getFullName(),
                trainingClass.getDiscipline().getId(),
                trainingClass.getDiscipline().getName(),
                trainingClass.getRoom(),
                trainingClass.getStartsAt(),
                trainingClass.getDurationMinutes(),
                trainingClass.getCapacity(),
                trainingClass.getLevel());
    }

    public static void apply(TrainingClassRequest request, TrainingClass trainingClass,
                             Trainer trainer, Discipline discipline) {
        trainingClass.setTitle(request.title());
        trainingClass.setDescription(request.description());
        trainingClass.setTrainer(trainer);
        trainingClass.setDiscipline(discipline);
        trainingClass.setRoom(request.room());
        trainingClass.setStartsAt(request.startsAt());
        trainingClass.setDurationMinutes(request.durationMinutes());
        trainingClass.setCapacity(request.capacity());
        trainingClass.setLevel(request.level());
    }
}
