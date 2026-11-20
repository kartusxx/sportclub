package ru.teamname.sportclub.booking.mapper;

import ru.teamname.sportclub.booking.dto.BookingResponse;
import ru.teamname.sportclub.booking.entity.Booking;

public final class BookingMapper {

    private BookingMapper() {
    }

    public static BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getMemberId(),
                booking.getClassId(),
                booking.getBookedAt(),
                booking.getStatus(),
                booking.getNote());
    }
}
