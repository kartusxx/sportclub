package ru.teamname.sportclub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.teamname.sportclub.entity.Booking;
import ru.teamname.sportclub.enums.BookingStatus;

import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Override
    @EntityGraph(attributePaths = {"member", "trainingClass"})
    Page<Booking> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"member", "trainingClass"})
    Page<Booking> findByTrainingClassId(Long classId, Pageable pageable);

    Optional<Booking> findByMemberIdAndTrainingClassId(Long memberId, Long classId);

    long countByTrainingClassIdAndStatus(Long classId, BookingStatus status);

    long deleteByTrainingClassId(Long classId);
}
