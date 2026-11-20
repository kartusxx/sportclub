package ru.teamname.sportclub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import ru.teamname.sportclub.enums.BookingStatus;

import java.time.LocalDateTime;

/**
 * Many to Many с дополнительными полями (bookedAt, status, note):
 * участник может записаться на много занятий, на занятие записывается много участников.
 */
@Getter
@Setter
@Entity
@Table(
        name = "bookings",
        uniqueConstraints = @UniqueConstraint(name = "uq_bookings_member_class", columnNames = {"member_id", "class_id"}))
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private TrainingClass trainingClass;

    @NotNull
    @Column(name = "booked_at", nullable = false)
    private LocalDateTime bookedAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.BOOKED;

    @Size(max = 255)
    @Column(length = 255)
    private String note;
}
