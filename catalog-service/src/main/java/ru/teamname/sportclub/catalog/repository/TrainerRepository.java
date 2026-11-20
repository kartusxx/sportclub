package ru.teamname.sportclub.catalog.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import ru.teamname.sportclub.catalog.entity.Trainer;

public interface TrainerRepository extends ReactiveCrudRepository<Trainer, Long> {

    @Query("SELECT COUNT(*) FROM trainers WHERE LOWER(email) = LOWER(:email)")
    Mono<Long> countByEmail(@Param("email") String email);

    @Query("SELECT COUNT(*) FROM trainers WHERE LOWER(email) = LOWER(:email) AND id <> :id")
    Mono<Long> countByEmailExcludingId(@Param("email") String email, @Param("id") Long id);
}
