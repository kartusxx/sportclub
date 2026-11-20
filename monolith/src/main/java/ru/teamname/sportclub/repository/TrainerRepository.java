package ru.teamname.sportclub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.teamname.sportclub.entity.Trainer;

public interface TrainerRepository extends JpaRepository<Trainer, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
