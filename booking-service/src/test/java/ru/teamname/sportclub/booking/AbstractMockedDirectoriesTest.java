package ru.teamname.sportclub.booking;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.teamname.sportclub.booking.client.ClassDirectory;
import ru.teamname.sportclub.booking.client.ClassInfo;
import ru.teamname.sportclub.booking.client.MemberDirectory;
import ru.teamname.sportclub.booking.client.MemberInfo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Интеграционные тесты логики записи: база настоящая, а member-service и catalog-service заменены
 * заглушками портов {@link MemberDirectory} и {@link ClassDirectory}. Идентификаторы участников и занятий
 * уникальны на весь прогон, поэтому тесты не мешают друг другу (внешних ключей в booking_db нет).
 */
public abstract class AbstractMockedDirectoriesTest extends AbstractIntegrationTest {

    private static final AtomicLong IDS = new AtomicLong(System.currentTimeMillis() % 100_000_000L * 100);

    @MockitoBean
    protected MemberDirectory memberDirectory;

    @MockitoBean
    protected ClassDirectory classDirectory;

    protected long stubMember(String status, boolean hasSubscription) {
        long id = IDS.incrementAndGet();
        when(memberDirectory.getMember(id)).thenReturn(new MemberInfo(id, "Ivan Petrov", status));
        when(memberDirectory.hasActiveSubscription(eq(id), any(LocalDate.class))).thenReturn(hasSubscription);
        return id;
    }

    protected long newMember() {
        return stubMember("ACTIVE", true);
    }

    protected long newClass(int capacity) {
        return newClass(capacity, LocalDateTime.now().plusDays(1));
    }

    protected long newClass(int capacity, LocalDateTime startsAt) {
        long id = IDS.incrementAndGet();
        stubClass(id, capacity, startsAt);
        return id;
    }

    protected void stubClass(long id, int capacity, LocalDateTime startsAt) {
        when(classDirectory.getClassInfo(id)).thenReturn(new ClassInfo(id, "Yoga", startsAt, capacity));
    }
}
