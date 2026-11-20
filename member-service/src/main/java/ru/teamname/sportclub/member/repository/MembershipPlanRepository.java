package ru.teamname.sportclub.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.teamname.sportclub.member.entity.MembershipPlan;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
