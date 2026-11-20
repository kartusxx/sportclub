package ru.teamname.sportclub.booking.client.pending;

import org.springframework.stereotype.Component;
import ru.teamname.sportclub.booking.client.MemberDirectory;
import ru.teamname.sportclub.booking.client.MemberInfo;
import ru.teamname.sportclub.booking.exception.ServiceUnavailableException;

import java.time.LocalDate;

/**
 * ВРЕМЕННАЯ заглушка: Feign-клиент появится на шаге 5, и этот класс будет удалён.
 * Нужна, чтобы сервис запускался уже сейчас.
 */
@Component
public class PendingMemberDirectory implements MemberDirectory {

    private static final String MESSAGE = "Интеграция с member-service ещё не подключена";

    @Override
    public MemberInfo getMember(Long memberId) {
        throw new ServiceUnavailableException(MESSAGE);
    }

    @Override
    public boolean hasActiveSubscription(Long memberId, LocalDate date) {
        throw new ServiceUnavailableException(MESSAGE);
    }
}
