package ru.teamname.sportclub.booking.dto;

import ru.teamname.sportclub.booking.enums.BookingStatus;

import java.time.LocalDateTime;

/** Участник и занятие принадлежат другим сервисам, поэтому в ответе только их идентификаторы. */
public record BookingResponse(
        Long id,
        Long memberId,
        Long classId,
        LocalDateTime bookedAt,
        BookingStatus status,
        String note) {
}
