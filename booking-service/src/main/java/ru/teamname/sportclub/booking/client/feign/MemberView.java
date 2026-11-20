package ru.teamname.sportclub.booking.client.feign;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Ответ GET /internal/members/{id} (member-service). Лишние поля игнорируются. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MemberView(Long id, String fullName, String status) {
}
