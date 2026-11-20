package ru.teamname.sportclub.member.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import ru.teamname.sportclub.member.entity.Member;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    /** Блокирует строку участника до конца транзакции (защита от гонок при оформлении абонемента). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Member> findForUpdateById(Long id);
}
