package ru.teamname.sportclub.member.dto;

/** Краткие данные участника для других сервисов (внутренний API). */
public record MemberSummary(Long id, String fullName, String status) {
}
