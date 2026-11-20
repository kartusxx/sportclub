package ru.teamname.sportclub.member.dto;

import ru.teamname.sportclub.member.enums.MemberStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MemberResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        LocalDate birthDate,
        MemberStatus status,
        LocalDateTime createdAt) {
}
