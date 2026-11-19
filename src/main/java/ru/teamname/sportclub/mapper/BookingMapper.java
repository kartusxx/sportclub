package ru.teamname.sportclub.mapper;

import ru.teamname.sportclub.dto.BookingResponse;
import ru.teamname.sportclub.entity.Booking;

public final class BookingMapper {

    private BookingMapper() {
    }

    public static BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getMember().getId(),
                booking.getMember().getFullName(),
                booking.getTrainingClass().getId(),
                booking.getTrainingClass().getTitle(),
                booking.getBookedAt(),
                booking.getStatus(),
                booking.getNote());
    }
}
