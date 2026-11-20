package ru.teamname.sportclub.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import ru.teamname.sportclub.entity.TrainingClass;

import java.util.Optional;

public interface TrainingClassRepository extends JpaRepository<TrainingClass, Long> {

    /** Slice не выполняет count-запрос — подходит для бесконечной прокрутки. */
    @EntityGraph(attributePaths = {"trainer", "discipline"})
    Slice<TrainingClass> findBy(Pageable pageable);

    /** Блокирует занятие до конца транзакции, чтобы две записи не заняли последнее место одновременно. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TrainingClass> findLockedById(Long id);
}
