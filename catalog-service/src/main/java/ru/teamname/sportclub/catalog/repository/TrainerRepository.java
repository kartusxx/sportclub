package ru.teamname.sportclub.catalog.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import ru.teamname.sportclub.catalog.entity.Trainer;

public interface TrainerRepository extends ReactiveCrudRepository<Trainer, Long> {

    Mono<Long> countByEmailIgnoreCase(String email);

    Mono<Long> countByEmailIgnoreCaseAndIdNot(String email, Long id);
}
