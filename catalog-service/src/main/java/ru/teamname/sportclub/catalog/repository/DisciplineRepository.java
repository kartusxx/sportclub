package ru.teamname.sportclub.catalog.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import ru.teamname.sportclub.catalog.entity.Discipline;

public interface DisciplineRepository extends ReactiveCrudRepository<Discipline, Long> {

    Mono<Long> countByNameIgnoreCase(String name);

    Mono<Long> countByNameIgnoreCaseAndIdNot(String name, Long id);
}
