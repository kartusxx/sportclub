package ru.teamname.sportclub.booking.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.teamname.sportclub.booking.entity.Booking;
import ru.teamname.sportclub.booking.enums.BookingStatus;

import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Page<Booking> findByClassId(Long classId, Pageable pageable);

    Optional<Booking> findByMemberIdAndClassId(Long memberId, Long classId);

    long countByClassIdAndStatus(Long classId, BookingStatus status);
}
