package ru.teamname.sportclub.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.teamname.sportclub.member.dto.SubscriptionRequest;
import ru.teamname.sportclub.member.dto.SubscriptionResponse;
import ru.teamname.sportclub.member.entity.Member;
import ru.teamname.sportclub.member.entity.MembershipPlan;
import ru.teamname.sportclub.member.entity.Subscription;
import ru.teamname.sportclub.member.enums.MemberStatus;
import ru.teamname.sportclub.member.enums.SubscriptionStatus;
import ru.teamname.sportclub.member.exception.BusinessRuleException;
import ru.teamname.sportclub.member.exception.ResourceNotFoundException;
import ru.teamname.sportclub.member.mapper.SubscriptionMapper;
import ru.teamname.sportclub.member.repository.MemberRepository;
import ru.teamname.sportclub.member.repository.MembershipPlanRepository;
import ru.teamname.sportclub.member.repository.SubscriptionRepository;

import java.time.Clock;
import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final MemberRepository memberRepository;
    private final MembershipPlanRepository planRepository;
    private final Clock clock;

    public Page<SubscriptionResponse> findAll(Pageable pageable) {
        return subscriptionRepository.findAll(pageable).map(SubscriptionMapper::toResponse);
    }

    public Page<SubscriptionResponse> findByMember(Long memberId, Pageable pageable) {
        if (!memberRepository.existsById(memberId)) {
            throw new ResourceNotFoundException("Участник", memberId);
        }
        return subscriptionRepository.findByMemberId(memberId, pageable).map(SubscriptionMapper::toResponse);
    }

    /** Есть ли у участника действующий абонемент, покрывающий дату. Для booking-service (внутренний API). */
    public boolean hasCoverage(Long memberId, LocalDate date) {
        if (!memberRepository.existsById(memberId)) {
            throw new ResourceNotFoundException("Участник", memberId);
        }
        return subscriptionRepository.countCovering(memberId, SubscriptionStatus.ACTIVE, date) > 0;
    }

    public SubscriptionResponse findById(Long id) {
        return SubscriptionMapper.toResponse(getOrThrow(id));
    }

    /**
     * Оформление абонемента — составная операция, поэтому выполняется в одной транзакции:
     * блокировка участника, перевод просроченных абонементов в EXPIRED, расчёт даты начала
     * (продление после действующего абонемента) и создание нового абонемента.
     * Блокировка строки участника не даёт двум параллельным покупкам получить одинаковую дату начала,
     * а откат при ошибке не оставит базу в промежуточном состоянии.
     */
    @Transactional
    public SubscriptionResponse purchase(SubscriptionRequest request) {
        Member member = memberRepository.findByIdForUpdate(request.memberId())
                .orElseThrow(() -> new ResourceNotFoundException("Участник", request.memberId()));
        ensureMemberIsActive(member);
        MembershipPlan plan = planRepository.findById(request.planId())
                .orElseThrow(() -> new ResourceNotFoundException("Тариф", request.planId()));

        LocalDate today = LocalDate.now(clock);
        subscriptionRepository.expireOutdated(member.getId(), SubscriptionStatus.ACTIVE,
                SubscriptionStatus.EXPIRED, today);

        LocalDate start = calculateStartDate(member, today);
        return SubscriptionMapper.toResponse(subscriptionRepository.save(newSubscription(member, plan, start)));
    }

    private void ensureMemberIsActive(Member member) {
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessRuleException("Нельзя оформить абонемент: статус участника " + member.getStatus());
        }
    }

    /** Продление начинается на следующий день после действующего абонемента, иначе — сегодня. */
    private LocalDate calculateStartDate(Member member, LocalDate today) {
        return subscriptionRepository.findLatestEndDate(member.getId(), SubscriptionStatus.ACTIVE)
                .filter(latestEnd -> !latestEnd.isBefore(today))
                .map(latestEnd -> latestEnd.plusDays(1))
                .orElse(today);
    }

    private Subscription newSubscription(Member member, MembershipPlan plan, LocalDate start) {
        Subscription subscription = new Subscription();
        subscription.setMember(member);
        subscription.setPlan(plan);
        subscription.setStartDate(start);
        subscription.setEndDate(start.plusDays(plan.getDurationDays() - 1L));
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        return subscription;
    }

    @Transactional
    public SubscriptionResponse cancel(Long id) {
        Subscription subscription = getOrThrow(id);
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new BusinessRuleException("Отменить можно только действующий абонемент");
        }
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        return SubscriptionMapper.toResponse(subscriptionRepository.save(subscription));
    }

    @Transactional
    public void delete(Long id) {
        Subscription subscription = getOrThrow(id);
        subscriptionRepository.delete(subscription);
    }

    private Subscription getOrThrow(Long id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Абонемент", id));
    }
}
