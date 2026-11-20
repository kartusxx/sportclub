package ru.teamname.sportclub.catalog.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.teamname.sportclub.catalog.client.BookingGateway;
import ru.teamname.sportclub.catalog.dto.SliceResponse;
import ru.teamname.sportclub.catalog.dto.TrainingClassRequest;
import ru.teamname.sportclub.catalog.dto.TrainingClassResponse;
import ru.teamname.sportclub.catalog.entity.Discipline;
import ru.teamname.sportclub.catalog.entity.Trainer;
import ru.teamname.sportclub.catalog.entity.TrainingClass;
import ru.teamname.sportclub.catalog.exception.ConflictException;
import ru.teamname.sportclub.catalog.exception.ResourceNotFoundException;
import ru.teamname.sportclub.catalog.mapper.TrainingClassMapper;
import ru.teamname.sportclub.catalog.repository.DisciplineRepository;
import ru.teamname.sportclub.catalog.repository.TrainerRepository;
import ru.teamname.sportclub.catalog.repository.TrainingClassRepository;
import ru.teamname.sportclub.catalog.support.EntityValidator;
import ru.teamname.sportclub.catalog.support.PagedReader;
import ru.teamname.sportclub.catalog.web.PageQuery;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainingClassService {

    private final TrainingClassRepository classRepository;
    private final TrainerRepository trainerRepository;
    private final DisciplineRepository disciplineRepository;
    private final PagedReader pagedReader;
    private final EntityValidator validator;
    private final BookingGateway bookingGateway;

    /** Лента занятий для бесконечной прокрутки: без подсчёта общего количества записей. */
    public Mono<SliceResponse<TrainingClassResponse>> findFeed(PageQuery query) {
        return pagedReader.readSlice(TrainingClass.class, query)
                .flatMap(slice -> toResponses(slice.getContent())
                        .map(content -> new SliceResponse<>(content, slice.getNumber(), slice.getSize(),
                                slice.hasNext())));
    }

    public Mono<TrainingClassResponse> findById(Long id) {
        return getOrThrow(id)
                .flatMap(trainingClass -> toResponses(List.of(trainingClass)))
                .map(responses -> responses.get(0));
    }

    public Mono<TrainingClassResponse> create(TrainingClassRequest request) {
        return Mono.zip(getTrainer(request.trainerId()), getDiscipline(request.disciplineId()))
                .flatMap(refs -> {
                    TrainingClass trainingClass = new TrainingClass();
                    TrainingClassMapper.apply(request, trainingClass);
                    return save(trainingClass)
                            .map(saved -> TrainingClassMapper.toResponse(saved, refs.getT1(), refs.getT2()));
                });
    }

    /**
     * Вместимость нельзя уменьшить ниже числа уже записанных участников. Записи хранит booking-service,
     * поэтому число запрашивается по сети и только если вместимость уменьшается.
     * Проверка и сохранение не атомарны: запись, пришедшая между ними, может превысить новую вместимость
     * (допущение eventual consistency, описано в README).
     */
    public Mono<TrainingClassResponse> update(Long id, TrainingClassRequest request) {
        return getOrThrow(id)
                .flatMap(trainingClass -> checkCapacity(trainingClass, request)
                        .then(Mono.zip(getTrainer(request.trainerId()), getDiscipline(request.disciplineId())))
                        .flatMap(refs -> {
                            TrainingClassMapper.apply(request, trainingClass);
                            return save(trainingClass)
                                    .map(saved -> TrainingClassMapper.toResponse(saved, refs.getT1(),
                                            refs.getT2()));
                        }));
    }

    /**
     * Сначала удаляется занятие, затем (best-effort) его записи в booking-service. Обратный порядок
     * опаснее: при сбое удаления занятия записи уже потеряны. Осиротевшие записи не видны пользователям.
     */
    public Mono<Void> delete(Long id) {
        return getOrThrow(id)
                .flatMap(trainingClass -> classRepository.delete(trainingClass)
                        .then(bookingGateway.deleteBookings(id)));
    }

    private Mono<Void> checkCapacity(TrainingClass current, TrainingClassRequest request) {
        if (request.capacity() >= current.getCapacity()) {
            return Mono.empty();
        }
        return bookingGateway.bookedCount(current.getId())
                .flatMap(booked -> booked > request.capacity()
                        ? Mono.<Void>error(new ConflictException(
                                "Нельзя уменьшить вместимость до " + request.capacity()
                                        + ": на занятие уже записано " + booked))
                        : Mono.<Void>empty());
    }

    /** Подтягивает имена тренеров и направлений двумя запросами на всю пачку занятий (без N+1). */
    private Mono<List<TrainingClassResponse>> toResponses(List<TrainingClass> classes) {
        if (classes.isEmpty()) {
            return Mono.just(List.<TrainingClassResponse>of());
        }
        Set<Long> trainerIds = classes.stream().map(TrainingClass::getTrainerId).collect(Collectors.toSet());
        Set<Long> disciplineIds = classes.stream().map(TrainingClass::getDisciplineId)
                .collect(Collectors.toSet());
        Mono<Map<Long, Trainer>> trainers = trainerRepository.findAllById(trainerIds)
                .collectMap(Trainer::getId);
        Mono<Map<Long, Discipline>> disciplines = disciplineRepository.findAllById(disciplineIds)
                .collectMap(Discipline::getId);
        return Mono.zip(trainers, disciplines)
                .map(refs -> classes.stream()
                        .map(trainingClass -> TrainingClassMapper.toResponse(trainingClass,
                                refs.getT1().get(trainingClass.getTrainerId()),
                                refs.getT2().get(trainingClass.getDisciplineId())))
                        .toList());
    }

    private Mono<TrainingClass> save(TrainingClass trainingClass) {
        return Mono.fromSupplier(() -> validator.validate(trainingClass)).flatMap(classRepository::save);
    }

    private Mono<TrainingClass> getOrThrow(Long id) {
        return classRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Занятие", id)));
    }

    private Mono<Trainer> getTrainer(Long id) {
        return trainerRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Тренер", id)));
    }

    private Mono<Discipline> getDiscipline(Long id) {
        return disciplineRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Направление", id)));
    }
}
