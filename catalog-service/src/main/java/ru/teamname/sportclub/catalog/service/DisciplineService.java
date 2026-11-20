package ru.teamname.sportclub.catalog.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.teamname.sportclub.catalog.dto.DisciplineRequest;
import ru.teamname.sportclub.catalog.dto.DisciplineResponse;
import ru.teamname.sportclub.catalog.entity.Discipline;
import ru.teamname.sportclub.catalog.exception.ConflictException;
import ru.teamname.sportclub.catalog.exception.ResourceNotFoundException;
import ru.teamname.sportclub.catalog.mapper.DisciplineMapper;
import ru.teamname.sportclub.catalog.repository.DisciplineRepository;
import ru.teamname.sportclub.catalog.support.EntityValidator;
import ru.teamname.sportclub.catalog.support.PagedReader;
import ru.teamname.sportclub.catalog.web.PageQuery;

@Service
@RequiredArgsConstructor
public class DisciplineService {

    private final DisciplineRepository disciplineRepository;
    private final PagedReader pagedReader;
    private final EntityValidator validator;

    public Mono<Page<DisciplineResponse>> findAll(PageQuery query) {
        return pagedReader.readPage(Discipline.class, query)
                .map(page -> page.map(DisciplineMapper::toResponse));
    }

    public Mono<DisciplineResponse> findById(Long id) {
        return getOrThrow(id).map(DisciplineMapper::toResponse);
    }

    public Mono<DisciplineResponse> create(DisciplineRequest request) {
        return ensureNameIsFree(request.name(), null)
                .then(Mono.defer(() -> save(DisciplineMapper.toEntity(request))))
                .map(DisciplineMapper::toResponse);
    }

    public Mono<DisciplineResponse> update(Long id, DisciplineRequest request) {
        return getOrThrow(id)
                .flatMap(discipline -> ensureNameIsFree(request.name(), id)
                        .then(Mono.defer(() -> {
                            DisciplineMapper.apply(request, discipline);
                            return save(discipline);
                        })))
                .map(DisciplineMapper::toResponse);
    }

    public Mono<Void> delete(Long id) {
        return getOrThrow(id).flatMap(disciplineRepository::delete);
    }

    private Mono<Void> ensureNameIsFree(String name, Long excludedId) {
        Mono<Long> count = excludedId == null
                ? disciplineRepository.countByName(name)
                : disciplineRepository.countByNameExcludingId(name, excludedId);
        return count.filter(found -> found > 0)
                .flatMap(found -> Mono.<Void>error(
                        new ConflictException("Направление '" + name + "' уже существует")));
    }

    private Mono<Discipline> save(Discipline discipline) {
        return Mono.fromSupplier(() -> validator.validate(discipline)).flatMap(disciplineRepository::save);
    }

    private Mono<Discipline> getOrThrow(Long id) {
        return disciplineRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Направление", id)));
    }
}
