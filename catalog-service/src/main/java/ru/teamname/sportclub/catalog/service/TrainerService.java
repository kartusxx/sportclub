package ru.teamname.sportclub.catalog.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import ru.teamname.sportclub.catalog.dto.TrainerRequest;
import ru.teamname.sportclub.catalog.dto.TrainerResponse;
import ru.teamname.sportclub.catalog.entity.Discipline;
import ru.teamname.sportclub.catalog.entity.Trainer;
import ru.teamname.sportclub.catalog.exception.ConflictException;
import ru.teamname.sportclub.catalog.exception.ResourceNotFoundException;
import ru.teamname.sportclub.catalog.mapper.TrainerMapper;
import ru.teamname.sportclub.catalog.repository.DisciplineRepository;
import ru.teamname.sportclub.catalog.repository.TrainerDisciplineRepository;
import ru.teamname.sportclub.catalog.repository.TrainerRepository;
import ru.teamname.sportclub.catalog.support.EntityValidator;
import ru.teamname.sportclub.catalog.support.PagedReader;
import ru.teamname.sportclub.catalog.web.PageQuery;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainerService {

    private final TrainerRepository trainerRepository;
    private final DisciplineRepository disciplineRepository;
    private final TrainerDisciplineRepository trainerDisciplines;
    private final PagedReader pagedReader;
    private final EntityValidator validator;

    public Mono<Page<TrainerResponse>> findAll(PageQuery query) {
        return pagedReader.readPage(Trainer.class, query)
                .flatMap(page -> attachDisciplines(page.getContent()).thenReturn(page))
                .map(page -> page.map(TrainerMapper::toResponse));
    }

    public Mono<TrainerResponse> findById(Long id) {
        return getOrThrow(id)
                .flatMap(trainer -> attachDisciplines(List.of(trainer)).thenReturn(trainer))
                .map(TrainerMapper::toResponse);
    }

    /**
     * Запись тренера затрагивает две таблицы: trainers и trainer_disciplines (Many to Many).
     * Операция выполняется в одной транзакции, иначе при сбое на вставке связей остался бы тренер
     * без направлений (или наоборот: направления удалены, а новые не записаны).
     */
    @Transactional
    public Mono<TrainerResponse> create(TrainerRequest request) {
        return ensureEmailIsFree(request.email(), null)
                .then(loadDisciplines(request.disciplineIds()))
                .flatMap(disciplines -> {
                    Trainer trainer = new Trainer();
                    TrainerMapper.apply(request, trainer, disciplines);
                    return saveWithDisciplines(trainer, disciplines);
                })
                .map(TrainerMapper::toResponse);
    }

    /** Обновление тоже транзакционное: см. {@link #create(TrainerRequest)}. */
    @Transactional
    public Mono<TrainerResponse> update(Long id, TrainerRequest request) {
        return getOrThrow(id)
                .flatMap(trainer -> ensureEmailIsFree(request.email(), id)
                        .then(loadDisciplines(request.disciplineIds()))
                        .flatMap(disciplines -> {
                            TrainerMapper.apply(request, trainer, disciplines);
                            return saveWithDisciplines(trainer, disciplines);
                        }))
                .map(TrainerMapper::toResponse);
    }

    /** Связи trainer_disciplines удаляются каскадно на уровне БД (ON DELETE CASCADE). */
    public Mono<Void> delete(Long id) {
        return getOrThrow(id).flatMap(trainerRepository::delete);
    }

    private Mono<Trainer> saveWithDisciplines(Trainer trainer, Set<Discipline> disciplines) {
        Set<Long> disciplineIds = disciplines.stream().map(Discipline::getId).collect(Collectors.toSet());
        return Mono.fromSupplier(() -> validator.validate(trainer))
                .flatMap(trainerRepository::save)
                .flatMap(saved -> {
                    saved.setDisciplines(disciplines);
                    return trainerDisciplines.replace(saved.getId(), disciplineIds).thenReturn(saved);
                });
    }

    private Mono<Void> ensureEmailIsFree(String email, Long excludedId) {
        Mono<Long> count = excludedId == null
                ? trainerRepository.countByEmailIgnoreCase(email)
                : trainerRepository.countByEmailIgnoreCaseAndIdNot(email, excludedId);
        return count.filter(found -> found > 0)
                .flatMap(found -> Mono.<Void>error(
                        new ConflictException("Тренер с email " + email + " уже существует")));
    }

    private Mono<Set<Discipline>> loadDisciplines(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Mono.just(new HashSet<Discipline>());
        }
        return disciplineRepository.findAllById(ids).collectList()
                .<Set<Discipline>>flatMap(found -> {
                    Set<Long> foundIds = found.stream().map(Discipline::getId).collect(Collectors.toSet());
                    Optional<Long> missing = ids.stream().filter(id -> !foundIds.contains(id)).findFirst();
                    if (missing.isPresent()) {
                        return Mono.error(new ResourceNotFoundException("Направление", missing.get()));
                    }
                    return Mono.just(new HashSet<>(found));
                });
    }

    /** Загружает направления для всех тренеров одним запросом и кладёт их в transient-поле. */
    private Mono<Void> attachDisciplines(List<Trainer> trainers) {
        if (trainers.isEmpty()) {
            return Mono.empty();
        }
        List<Long> trainerIds = trainers.stream().map(Trainer::getId).toList();
        return trainerDisciplines.findDisciplinesByTrainerIds(trainerIds)
                .doOnNext(byTrainer -> trainers.forEach(trainer -> trainer.setDisciplines(
                        new HashSet<>(byTrainer.getOrDefault(trainer.getId(), List.of())))))
                .then();
    }

    private Mono<Trainer> getOrThrow(Long id) {
        return trainerRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Тренер", id)));
    }
}
