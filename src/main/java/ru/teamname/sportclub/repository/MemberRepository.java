package ru.teamname.sportclub.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import ru.teamname.sportclub.entity.Member;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    /** Блокирует строку участника до конца транзакции (защита от гонок при оформлении абонемента). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Member> findLockedById(Long id);
}
