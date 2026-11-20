package ru.teamname.sportclub.booking.client;

/** Данные участника, которые нужны booking-service (получаются от member-service). */
public record MemberInfo(Long id, String fullName, String status) {

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}
