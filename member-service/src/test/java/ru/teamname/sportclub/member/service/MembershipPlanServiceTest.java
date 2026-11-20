package ru.teamname.sportclub.member.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.teamname.sportclub.member.dto.MembershipPlanRequest;
import ru.teamname.sportclub.member.dto.MembershipPlanResponse;
import ru.teamname.sportclub.member.entity.MembershipPlan;
import ru.teamname.sportclub.member.enums.PlanType;
import ru.teamname.sportclub.member.exception.ConflictException;
import ru.teamname.sportclub.member.exception.ResourceNotFoundException;
import ru.teamname.sportclub.member.repository.MembershipPlanRepository;

import java.math.BigDecimal;
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
class MembershipPlanServiceTest {

    @Mock
    private MembershipPlanRepository planRepository;

    private MembershipPlanService service;
    private MembershipPlan plan;

    private static MembershipPlanRequest request(String name) {
        return new MembershipPlanRequest(name, PlanType.MONTHLY, new BigDecimal("1500.00"), 30);
    }

    @BeforeEach
    void setUp() {
        service = new MembershipPlanService(planRepository);
        plan = new MembershipPlan();
        plan.setId(1L);
        plan.setName("Monthly");
        plan.setType(PlanType.MONTHLY);
        plan.setPrice(new BigDecimal("1000.00"));
        plan.setDurationDays(30);

        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(planRepository.save(any(MembershipPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(planRepository.saveAndFlush(any(MembershipPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void findAllAndFindByIdReturnResponses() {
        when(planRepository.findAll(PageRequest.of(0, 5))).thenReturn(new PageImpl<>(List.of(plan)));

        assertThat(service.findAll(PageRequest.of(0, 5)).getContent())
                .extracting(MembershipPlanResponse::name).containsExactly("Monthly");
        assertThat(service.findById(1L).durationDays()).isEqualTo(30);
    }

    @Test
    void createSavesPlan() {
        MembershipPlanResponse response = service.create(request("Gold"));

        assertThat(response.name()).isEqualTo("Gold");
        assertThat(response.type()).isEqualTo(PlanType.MONTHLY);
    }

    @Test
    void createFailsForDuplicateName() {
        when(planRepository.existsByNameIgnoreCase("Gold")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("Gold"))).isInstanceOf(ConflictException.class);
        verify(planRepository, never()).save(any());
    }

    @Test
    void updateAppliesRequestAndChecksNameUniqueness() {
        assertThat(service.update(1L, request("Platinum")).name()).isEqualTo("Platinum");

        when(planRepository.existsByNameIgnoreCaseAndIdNot("Taken", 1L)).thenReturn(true);
        assertThatThrownBy(() -> service.update(1L, request("Taken"))).isInstanceOf(ConflictException.class);
    }

    @Test
    void deleteRemovesAndFlushes() {
        service.delete(1L);

        verify(planRepository).delete(plan);
        verify(planRepository).flush();
    }

    @Test
    void findByIdFailsForUnknownPlan() {
        when(planRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(404L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
