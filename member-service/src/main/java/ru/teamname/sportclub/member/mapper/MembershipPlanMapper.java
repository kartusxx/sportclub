package ru.teamname.sportclub.member.mapper;

import ru.teamname.sportclub.member.dto.MembershipPlanRequest;
import ru.teamname.sportclub.member.dto.MembershipPlanResponse;
import ru.teamname.sportclub.member.entity.MembershipPlan;

public final class MembershipPlanMapper {

    private MembershipPlanMapper() {
    }

    public static MembershipPlanResponse toResponse(MembershipPlan plan) {
        return new MembershipPlanResponse(plan.getId(), plan.getName(), plan.getType(), plan.getPrice(),
                plan.getDurationDays());
    }

    public static MembershipPlan toEntity(MembershipPlanRequest request) {
        MembershipPlan plan = new MembershipPlan();
        apply(request, plan);
        return plan;
    }

    public static void apply(MembershipPlanRequest request, MembershipPlan plan) {
        plan.setName(request.name());
        plan.setType(request.type());
        plan.setPrice(request.price());
        plan.setDurationDays(request.durationDays());
    }
}
