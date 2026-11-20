package ru.teamname.sportclub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.teamname.sportclub.entity.Subscription;
import ru.teamname.sportclub.enums.SubscriptionStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    @Override
    @EntityGraph(attributePaths = {"member", "plan"})
    Page<Subscription> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"member", "plan"})
    Page<Subscription> findByMemberId(Long memberId, Pageable pageable);

    /** Абонемент участника с указанным статусом, который заканчивается позже всех. */
    Optional<Subscription> findFirstByMemberIdAndStatusOrderByEndDateDesc(Long memberId, SubscriptionStatus status);

    /** Есть ли абонемент, действующий на дату: startDate <= date и endDate >= date. */
    boolean existsByMemberIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long memberId, SubscriptionStatus status, LocalDate startDateTo, LocalDate endDateFrom);

    /** Абонементы участника с указанным статусом, закончившиеся раньше даты. */
    List<Subscription> findByMemberIdAndStatusAndEndDateBefore(Long memberId, SubscriptionStatus status,
                                                               LocalDate date);
}
