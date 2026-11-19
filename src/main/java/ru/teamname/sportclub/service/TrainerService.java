package ru.teamname.sportclub.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.teamname.sportclub.dto.TrainerRequest;
import ru.teamname.sportclub.dto.TrainerResponse;
import ru.teamname.sportclub.entity.Discipline;
import ru.teamname.sportclub.entity.Trainer;
import ru.teamname.sportclub.exception.ConflictException;
import ru.teamname.sportclub.exception.ResourceNotFoundException;
import ru.teamname.sportclub.mapper.TrainerMapper;
import ru.teamname.sportclub.repository.DisciplineRepository;
import ru.teamname.sportclub.repository.TrainerRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class TrainerService {

    private final TrainerRepository trainerRepository;
    private final DisciplineRepository disciplineRepository;

    public TrainerService(TrainerRepository trainerRepository, DisciplineRepository disciplineRepository) {
        this.trainerRepository = trainerRepository;
        this.disciplineRepository = disciplineRepository;
    }

    public Page<TrainerResponse> findAll(Pageable pageable) {
        return trainerRepository.findAll(pageable).map(TrainerMapper::toResponse);
    }

    public TrainerResponse findById(Long id) {
        return TrainerMapper.toResponse(getOrThrow(id));
    }

    @Transactional
    public TrainerResponse create(TrainerRequest request) {
        if (trainerRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Тренер с email " + request.email() + " уже существует");
        }
        Trainer trainer = new Trainer();
        TrainerMapper.apply(request, trainer, loadDisciplines(request.disciplineIds()));
        return TrainerMapper.toResponse(trainerRepository.save(trainer));
    }

    @Transactional
    public TrainerResponse update(Long id, TrainerRequest request) {
        Trainer trainer = getOrThrow(id);
        if (trainerRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw new ConflictException("Тренер с email " + request.email() + " уже существует");
        }
        TrainerMapper.apply(request, trainer, loadDisciplines(request.disciplineIds()));
        return TrainerMapper.toResponse(trainerRepository.saveAndFlush(trainer));
    }

    @Transactional
    public void delete(Long id) {
        Trainer trainer = getOrThrow(id);
        trainerRepository.delete(trainer);
        trainerRepository.flush();
    }

    private Set<Discipline> loadDisciplines(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }
        List<Discipline> found = disciplineRepository.findAllById(ids);
        if (found.size() != ids.size()) {
            Set<Long> foundIds = new HashSet<>();
            found.forEach(discipline -> foundIds.add(discipline.getId()));
            Long missing = ids.stream().filter(id -> !foundIds.contains(id)).findFirst().orElseThrow();
            throw new ResourceNotFoundException("Направление", missing);
        }
        return new HashSet<>(found);
    }

    private Trainer getOrThrow(Long id) {
        return trainerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Тренер", id));
    }
}
