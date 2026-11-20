package ru.teamname.sportclub.catalog.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import ru.teamname.sportclub.catalog.entity.TrainingClass;

public interface TrainingClassRepository extends ReactiveCrudRepository<TrainingClass, Long> {
}
