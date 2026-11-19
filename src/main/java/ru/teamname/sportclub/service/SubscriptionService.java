package ru.teamname.sportclub.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.teamname.sportclub.dto.SubscriptionRequest;
import ru.teamname.sportclub.dto.SubscriptionResponse;
import ru.teamname.sportclub.entity.Member;
import ru.teamname.sportclub.entity.MembershipPlan;
import ru.teamname.sportclub.entity.Subscription;
import ru.teamname.sportclub.enums.MemberStatus;
import ru.teamname.sportclub.enums.SubscriptionStatus;
import ru.teamname.sportclub.exception.BusinessRuleException;
import ru.teamname.sportclub.exception.ResourceNotFoundException;
import ru.teamname.sportclub.mapper.SubscriptionMapper;
import ru.teamname.sportclub.repository.MemberRepository;
import ru.teamname.sportclub.repository.MembershipPlanRepository;
import ru.teamname.sportclub.repository.SubscriptionRepository;

import java.time.Clock;
import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final MemberRepository memberRepository;
    private final MembershipPlanRepository planRepository;
    private final Clock clock;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               MemberRepository memberRepository,
                               MembershipPlanRepository planRepository,
                               Clock clock) {
        this.subscriptionRepository = subscriptionRepository;
        this.memberRepository = memberRepository;
        this.planRepository = planRepository;
        this.clock = clock;
    }

    public Page<SubscriptionResponse> findAll(Pageable pageable) {
        return subscriptionRepository.findAll(pageable).map(SubscriptionMapper::toResponse);
    }

    public Page<SubscriptionResponse> findByMember(Long memberId, Pageable pageable) {
        if (!memberRepository.existsById(memberId)) {
            throw new ResourceNotFoundException("Участник", memberId);
        }
        return subscriptionRepository.findByMemberId(memberId, pageable).map(SubscriptionMapper::toResponse);
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
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessRuleException("Нельзя оформить абонемент: статус участника " + member.getStatus());
        }
        MembershipPlan plan = planRepository.findById(request.planId())
                .orElseThrow(() -> new ResourceNotFoundException("Тариф", request.planId()));

        LocalDate today = LocalDate.now(clock);
        subscriptionRepository.expireOutdated(member.getId(), SubscriptionStatus.ACTIVE,
                SubscriptionStatus.EXPIRED, today);

        LocalDate start = subscriptionRepository.findLatestEndDate(member.getId(), SubscriptionStatus.ACTIVE)
                .filter(latestEnd -> !latestEnd.isBefore(today))
                .map(latestEnd -> latestEnd.plusDays(1))
                .orElse(today);

        Subscription subscription = new Subscription();
        subscription.setMember(member);
        subscription.setPlan(plan);
        subscription.setStartDate(start);
        subscription.setEndDate(start.plusDays(plan.getDurationDays() - 1L));
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        return SubscriptionMapper.toResponse(subscriptionRepository.save(subscription));
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
