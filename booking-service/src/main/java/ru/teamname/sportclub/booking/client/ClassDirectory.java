package ru.teamname.sportclub.booking.client;

/** Порт к catalog-service (см. {@link MemberDirectory}). */
public interface ClassDirectory {

    /** @throws ru.teamname.sportclub.booking.exception.ResourceNotFoundException если занятия нет */
    ClassInfo getClassInfo(Long classId);
}
