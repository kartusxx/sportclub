package ru.teamname.sportclub.booking.support;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ClassLockTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    @Test
    void takesAdvisoryTransactionLockByClassId() {
        new ClassLock(jdbcTemplate).acquire(42L);

        verify(jdbcTemplate).query(eq("SELECT pg_advisory_xact_lock(?)"), any(ResultSetExtractor.class), eq(42L));
    }
}
