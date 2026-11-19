package ru.teamname.sportclub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.teamname.sportclub.entity.Subscription;
import ru.teamname.sportclub.enums.SubscriptionStatus;

import java.time.LocalDate;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    @Override
    @EntityGraph(attributePaths = {"member", "plan"})
    Page<Subscription> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"member", "plan"})
    Page<Subscription> findByMemberId(Long memberId, Pageable pageable);

    /** Самая поздняя дата окончания среди абонементов участника с указанным статусом. */
    @Query("select max(s.endDate) from Subscription s where s.member.id = :memberId and s.status = :status")
    Optional<LocalDate> findLatestEndDate(@Param("memberId") Long memberId,
                                          @Param("status") SubscriptionStatus status);

    /** Количество абонементов, покрывающих указанную дату. */
    @Query("""
            select count(s) from Subscription s
            where s.member.id = :memberId and s.status = :status
              and s.startDate <= :date and s.endDate >= :date
            """)
    long countCovering(@Param("memberId") Long memberId,
                       @Param("status") SubscriptionStatus status,
                       @Param("date") LocalDate date);

    /** Массово переводит просроченные абонементы в статус EXPIRED. */
    @Modifying(flushAutomatically = true)
    @Query("""
            update Subscription s set s.status = :expired
            where s.member.id = :memberId and s.status = :active and s.endDate < :today
            """)
    int expireOutdated(@Param("memberId") Long memberId,
                       @Param("active") SubscriptionStatus active,
                       @Param("expired") SubscriptionStatus expired,
                       @Param("today") LocalDate today);
}
