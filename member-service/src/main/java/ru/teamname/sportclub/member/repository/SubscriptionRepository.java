package ru.teamname.sportclub.member.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.teamname.sportclub.member.entity.Subscription;
import ru.teamname.sportclub.member.enums.SubscriptionStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    @Override
    @EntityGraph(attributePaths = {"member", "plan"})
    Page<Subscription> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"member", "plan"})
    Page<Subscription> findByMemberId(Long memberId, Pageable pageable);

    /** Абонемент участника с указанным статусом, который заканчивается позже всех остальных. */
    Optional<Subscription> findFirstByMemberIdAndStatusOrderByEndDateDesc(Long memberId, SubscriptionStatus status);

    /** Есть ли абонемент с указанным статусом, начавшийся не позже {@code startedBefore} и действующий не меньше чем до {@code endsAfter}. */
    boolean existsByMemberIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long memberId, SubscriptionStatus status, LocalDate startedBefore, LocalDate endsAfter);

    /** Абонементы участника с указанным статусом, закончившиеся раньше указанной даты. */
    List<Subscription> findByMemberIdAndStatusAndEndDateBefore(Long memberId, SubscriptionStatus status,
                                                               LocalDate date);
}
