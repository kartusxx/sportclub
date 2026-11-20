package ru.teamname.sportclub.booking.client.feign;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Ответ GET /internal/members/{id}/coverage: есть ли действующий абонемент на дату. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CoverageView(boolean covered) {
}
