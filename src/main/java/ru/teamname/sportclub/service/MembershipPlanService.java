package ru.teamname.sportclub.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.teamname.sportclub.dto.MembershipPlanRequest;
import ru.teamname.sportclub.dto.MembershipPlanResponse;
import ru.teamname.sportclub.entity.MembershipPlan;
import ru.teamname.sportclub.exception.ConflictException;
import ru.teamname.sportclub.exception.ResourceNotFoundException;
import ru.teamname.sportclub.mapper.MembershipPlanMapper;
import ru.teamname.sportclub.repository.MembershipPlanRepository;

@Service
@Transactional(readOnly = true)
public class MembershipPlanService {

    private final MembershipPlanRepository planRepository;

    public MembershipPlanService(MembershipPlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    public Page<MembershipPlanResponse> findAll(Pageable pageable) {
        return planRepository.findAll(pageable).map(MembershipPlanMapper::toResponse);
    }

    public MembershipPlanResponse findById(Long id) {
        return MembershipPlanMapper.toResponse(getOrThrow(id));
    }

    @Transactional
    public MembershipPlanResponse create(MembershipPlanRequest request) {
        if (planRepository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("Тариф '" + request.name() + "' уже существует");
        }
        MembershipPlan saved = planRepository.save(MembershipPlanMapper.toEntity(request));
        return MembershipPlanMapper.toResponse(saved);
    }

    @Transactional
    public MembershipPlanResponse update(Long id, MembershipPlanRequest request) {
        MembershipPlan plan = getOrThrow(id);
        if (planRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new ConflictException("Тариф '" + request.name() + "' уже существует");
        }
        MembershipPlanMapper.apply(request, plan);
        return MembershipPlanMapper.toResponse(planRepository.saveAndFlush(plan));
    }

    @Transactional
    public void delete(Long id) {
        MembershipPlan plan = getOrThrow(id);
        planRepository.delete(plan);
        planRepository.flush();
    }

    private MembershipPlan getOrThrow(Long id) {
        return planRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Тариф", id));
    }
}
