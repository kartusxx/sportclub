package ru.teamname.sportclub.catalog.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import ru.teamname.sportclub.catalog.entity.Discipline;

public interface DisciplineRepository extends ReactiveCrudRepository<Discipline, Long> {

    @Query("SELECT COUNT(*) FROM disciplines WHERE LOWER(name) = LOWER(:name)")
    Mono<Long> countByName(@Param("name") String name);

    @Query("SELECT COUNT(*) FROM disciplines WHERE LOWER(name) = LOWER(:name) AND id <> :id")
    Mono<Long> countByNameExcludingId(@Param("name") String name, @Param("id") Long id);
}
