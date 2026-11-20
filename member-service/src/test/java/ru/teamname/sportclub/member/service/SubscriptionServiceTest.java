package ru.teamname.sportclub.member.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;
import ru.teamname.sportclub.member.dto.SubscriptionRequest;
import ru.teamname.sportclub.member.dto.SubscriptionResponse;
import ru.teamname.sportclub.member.entity.Member;
import ru.teamname.sportclub.member.entity.MembershipPlan;
import ru.teamname.sportclub.member.entity.Subscription;
import ru.teamname.sportclub.member.enums.MemberStatus;
import ru.teamname.sportclub.member.enums.SubscriptionStatus;
import ru.teamname.sportclub.member.exception.BusinessRuleException;
import ru.teamname.sportclub.member.exception.ResourceNotFoundException;
import ru.teamname.sportclub.member.repository.MemberRepository;
import ru.teamname.sportclub.member.repository.MembershipPlanRepository;
import ru.teamname.sportclub.member.repository.SubscriptionRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-02T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 2);

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private MembershipPlanRepository planRepository;

    private SubscriptionService service;
    private Member member;
    private MembershipPlan plan;

    @BeforeEach
    void setUp() {
        service = new SubscriptionService(subscriptionRepository, memberRepository, planRepository, CLOCK);

        member = new Member();
        member.setId(1L);
        member.setFirstName("Ivan");
        member.setLastName("Petrov");
        member.setStatus(MemberStatus.ACTIVE);

        plan = new MembershipPlan();
        plan.setId(2L);
        plan.setName("Monthly");
        plan.setDurationDays(30);

        when(memberRepository.findForUpdateById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.existsById(1L)).thenReturn(true);
        when(planRepository.findById(2L)).thenReturn(Optional.of(plan));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Subscription subscription(SubscriptionStatus status) {
        Subscription subscription = new Subscription();
        subscription.setId(7L);
        subscription.setMember(member);
        subscription.setPlan(plan);
        subscription.setStartDate(TODAY);
        subscription.setEndDate(TODAY.plusDays(29));
        subscription.setStatus(status);
        when(subscriptionRepository.findById(7L)).thenReturn(Optional.of(subscription));
        return subscription;
    }

    private Subscription activeEndingOn(LocalDate endDate) {
        Subscription subscription = new Subscription();
        subscription.setMember(member);
        subscription.setPlan(plan);
        subscription.setStartDate(endDate.minusDays(29));
        subscription.setEndDate(endDate);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        return subscription;
    }

    @Test
    void purchaseStartsTodayWhenMemberHasNoActiveSubscription() {
        SubscriptionResponse response = service.purchase(new SubscriptionRequest(1L, 2L));

        assertThat(response.startDate()).isEqualTo(TODAY);
        assertThat(response.endDate()).isEqualTo(TODAY.plusDays(29));
        assertThat(response.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(response.planName()).isEqualTo("Monthly");
    }

    @Test
    void purchaseMarksOutdatedSubscriptionsAsExpired() {
        Subscription outdated = activeEndingOn(TODAY.minusDays(1));
        when(subscriptionRepository.findByMemberIdAndStatusAndEndDateBefore(1L, SubscriptionStatus.ACTIVE, TODAY))
                .thenReturn(List.of(outdated));

        service.purchase(new SubscriptionRequest(1L, 2L));

        assertThat(outdated.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        verify(subscriptionRepository).saveAll(List.of(outdated));
    }

    @Test
    void purchaseExtendsAfterCurrentActiveSubscription() {
        when(subscriptionRepository.findFirstByMemberIdAndStatusOrderByEndDateDesc(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(activeEndingOn(TODAY.plusDays(10))));

        SubscriptionResponse response = service.purchase(new SubscriptionRequest(1L, 2L));

        assertThat(response.startDate()).isEqualTo(TODAY.plusDays(11));
        assertThat(response.endDate()).isEqualTo(TODAY.plusDays(40));
    }

    @Test
    void purchaseIgnoresLatestEndDateInThePast() {
        when(subscriptionRepository.findFirstByMemberIdAndStatusOrderByEndDateDesc(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(activeEndingOn(TODAY.minusDays(3))));

        SubscriptionResponse response = service.purchase(new SubscriptionRequest(1L, 2L));

        assertThat(response.startDate()).isEqualTo(TODAY);
    }

    @Test
    void purchaseFailsForNonActiveMember() {
        member.setStatus(MemberStatus.FROZEN);

        assertThatThrownBy(() -> service.purchase(new SubscriptionRequest(1L, 2L)))
                .isInstanceOf(BusinessRuleException.class);
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void purchaseFailsForUnknownMember() {
        assertThatThrownBy(() -> service.purchase(new SubscriptionRequest(99L, 2L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void purchaseFailsForUnknownPlan() {
        assertThatThrownBy(() -> service.purchase(new SubscriptionRequest(1L, 99L)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void cancelChangesStatusOfActiveSubscription() {
        subscription(SubscriptionStatus.ACTIVE);

        SubscriptionResponse response = service.cancel(7L);

        assertThat(response.status()).isEqualTo(SubscriptionStatus.CANCELLED);
    }

    @Test
    void cancelFailsForNonActiveSubscription() {
        subscription(SubscriptionStatus.EXPIRED);

        assertThatThrownBy(() -> service.cancel(7L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void deleteRemovesSubscription() {
        Subscription subscription = subscription(SubscriptionStatus.ACTIVE);

        service.delete(7L);

        verify(subscriptionRepository).delete(subscription);
    }

    @Test
    void findByIdFailsForUnknownSubscription() {
        when(subscriptionRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(404L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findByMemberFailsForUnknownMember() {
        when(memberRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.findByMember(99L, Pageable.unpaged()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void hasCoverageIsTrueWhenActiveSubscriptionCoversDate() {
        when(subscriptionRepository.existsByMemberIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                1L, SubscriptionStatus.ACTIVE, TODAY, TODAY)).thenReturn(true);

        assertThat(service.hasCoverage(1L, TODAY)).isTrue();
    }

    @Test
    void hasCoverageIsFalseWhenNothingCoversDate() {
        when(subscriptionRepository.existsByMemberIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                1L, SubscriptionStatus.ACTIVE, TODAY, TODAY)).thenReturn(false);

        assertThat(service.hasCoverage(1L, TODAY)).isFalse();
    }

    @Test
    void hasCoverageFailsForUnknownMember() {
        when(memberRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.hasCoverage(99L, TODAY)).isInstanceOf(ResourceNotFoundException.class);
    }
}
