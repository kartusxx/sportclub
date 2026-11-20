package ru.teamname.sportclub.booking.support;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

/**
 * Блокировка записи на занятие. Занятие хранится в БД catalog-service, поэтому заблокировать его строку
 * ({@code SELECT ... FOR UPDATE}, как в монолите) здесь нельзя. Вместо этого берётся консультативная
 * блокировка PostgreSQL по идентификатору занятия: записи на одно занятие выстраиваются в очередь,
 * записи на разные занятия не мешают друг другу. Блокировка снимается автоматически при завершении
 * транзакции, поэтому вызывать метод нужно внутри неё.
 */
@Component
@RequiredArgsConstructor
public class ClassLock {

    private final JdbcTemplate jdbcTemplate;

    public void acquire(Long classId) {
        // Результат (тип void) не читается; JdbcTemplate работает в той же транзакции, что и JPA
        jdbcTemplate.query("SELECT pg_advisory_xact_lock(?)", (ResultSetExtractor<Object>) rs -> null, classId);
    }
}
