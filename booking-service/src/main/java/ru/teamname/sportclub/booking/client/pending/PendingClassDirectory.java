package ru.teamname.sportclub.booking.client.pending;

import org.springframework.stereotype.Component;
import ru.teamname.sportclub.booking.client.ClassDirectory;
import ru.teamname.sportclub.booking.client.ClassInfo;
import ru.teamname.sportclub.booking.exception.ServiceUnavailableException;

/** ВРЕМЕННАЯ заглушка: заменяется Feign-клиентом на шаге 5 (см. {@link PendingMemberDirectory}). */
@Component
public class PendingClassDirectory implements ClassDirectory {

    @Override
    public ClassInfo getClassInfo(Long classId) {
        throw new ServiceUnavailableException("Интеграция с catalog-service ещё не подключена");
    }
}
