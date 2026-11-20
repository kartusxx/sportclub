package ru.teamname.sportclub.booking.client.feign;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

/**
 * Нужные booking-service поля ответа GET /api/classes/{id} (catalog-service).
 * Остальные поля занятия игнорируются, поэтому изменения в catalog-service их не ломают.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ClassView(Long id, String title, LocalDateTime startsAt, Integer capacity) {
}
