package ru.teamname.sportclub.catalog.mapper;

import ru.teamname.sportclub.catalog.dto.TrainingClassRequest;
import ru.teamname.sportclub.catalog.dto.TrainingClassResponse;
import ru.teamname.sportclub.catalog.entity.Discipline;
import ru.teamname.sportclub.catalog.entity.Trainer;
import ru.teamname.sportclub.catalog.entity.TrainingClass;

public final class TrainingClassMapper {

    private TrainingClassMapper() {
    }

    /** Имена тренера и направления сущность не хранит, поэтому сервис передаёт загруженные объекты. */
    public static TrainingClassResponse toResponse(TrainingClass trainingClass, Trainer trainer,
                                                   Discipline discipline) {
        return new TrainingClassResponse(
                trainingClass.getId(),
                trainingClass.getTitle(),
                trainingClass.getDescription(),
                trainingClass.getTrainerId(),
                trainer.getFullName(),
                trainingClass.getDisciplineId(),
                discipline.getName(),
                trainingClass.getRoom(),
                trainingClass.getStartsAt(),
                trainingClass.getDurationMinutes(),
                trainingClass.getCapacity(),
                trainingClass.getLevel());
    }

    public static void apply(TrainingClassRequest request, TrainingClass trainingClass) {
        trainingClass.setTitle(request.title());
        trainingClass.setDescription(request.description());
        trainingClass.setTrainerId(request.trainerId());
        trainingClass.setDisciplineId(request.disciplineId());
        trainingClass.setRoom(request.room());
        trainingClass.setStartsAt(request.startsAt());
        trainingClass.setDurationMinutes(request.durationMinutes());
        trainingClass.setCapacity(request.capacity());
        trainingClass.setLevel(request.level());
    }
}
