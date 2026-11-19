package ru.teamname.sportclub.dto;

import ru.teamname.sportclub.enums.BookingStatus;

import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        Long memberId,
        String memberName,
        Long classId,
        String classTitle,
        LocalDateTime bookedAt,
        BookingStatus status,
        String note) {
}
