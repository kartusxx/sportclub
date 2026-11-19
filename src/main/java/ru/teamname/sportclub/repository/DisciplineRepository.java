package ru.teamname.sportclub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.teamname.sportclub.entity.Discipline;

public interface DisciplineRepository extends JpaRepository<Discipline, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
