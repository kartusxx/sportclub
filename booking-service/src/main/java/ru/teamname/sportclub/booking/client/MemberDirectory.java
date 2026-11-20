package ru.teamname.sportclub.booking.client;

import java.time.LocalDate;

/**
 * Порт к member-service. Бизнес-логика зависит от интерфейса, а не от HTTP-клиента:
 * на шаге 5 его реализует Feign-клиент с Circuit Breaker, в тестах подставляется мок.
 */
public interface MemberDirectory {

    /** @throws ru.teamname.sportclub.booking.exception.ResourceNotFoundException если участника нет */
    MemberInfo getMember(Long memberId);

    /** Есть ли у участника действующий абонемент, покрывающий указанную дату. */
    boolean hasActiveSubscription(Long memberId, LocalDate date);
}
