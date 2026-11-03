package ru.teamname.sportclub.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.teamname.sportclub.dto.DisciplineRequest;
import ru.teamname.sportclub.dto.DisciplineResponse;
import ru.teamname.sportclub.entity.Discipline;
import ru.teamname.sportclub.exception.ConflictException;
import ru.teamname.sportclub.exception.ResourceNotFoundException;
import ru.teamname.sportclub.mapper.DisciplineMapper;
import ru.teamname.sportclub.repository.DisciplineRepository;

@Service
@Transactional(readOnly = true)
public class DisciplineService {

    private final DisciplineRepository disciplineRepository;

    public DisciplineService(DisciplineRepository disciplineRepository) {
        this.disciplineRepository = disciplineRepository;
    }

    public Page<DisciplineResponse> findAll(Pageable pageable) {
        return disciplineRepository.findAll(pageable).map(DisciplineMapper::toResponse);
    }

    public DisciplineResponse findById(Long id) {
        return DisciplineMapper.toResponse(getOrThrow(id));
    }

    @Transactional
    public DisciplineResponse create(DisciplineRequest request) {
        if (disciplineRepository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("Направление '" + request.name() + "' уже существует");
        }
        Discipline saved = disciplineRepository.save(DisciplineMapper.toEntity(request));
        return DisciplineMapper.toResponse(saved);
    }

    @Transactional
    public DisciplineResponse update(Long id, DisciplineRequest request) {
        Discipline discipline = getOrThrow(id);
        if (disciplineRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new ConflictException("Направление '" + request.name() + "' уже существует");
        }
        DisciplineMapper.apply(request, discipline);
        return DisciplineMapper.toResponse(disciplineRepository.saveAndFlush(discipline));
    }

    @Transactional
    public void delete(Long id) {
        Discipline discipline = getOrThrow(id);
        disciplineRepository.delete(discipline);
        disciplineRepository.flush();
    }

    private Discipline getOrThrow(Long id) {
        return disciplineRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Направление", id));
    }
}
