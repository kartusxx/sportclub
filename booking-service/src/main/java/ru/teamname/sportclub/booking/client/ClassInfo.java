package ru.teamname.sportclub.booking.client;

import java.time.LocalDateTime;

/** Данные занятия, которые нужны booking-service (получаются от catalog-service). */
public record ClassInfo(Long id, String title, LocalDateTime startsAt, int capacity) {
}
