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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import ru.teamname.sportclub.enums.ClassLevel;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "training_classes")
public class TrainingClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String title;

    @Size(max = 1000)
    @Column(length = 1000)
    private String description;

    /** Many to One: тренер проводит много занятий. */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trainer_id", nullable = false)
    private Trainer trainer;

    /** Many to One: по одному направлению проводится много занятий. */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "discipline_id", nullable = false)
    private Discipline discipline;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String room;

    @NotNull
    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @NotNull
    @Min(15)
    @Max(240)
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @NotNull
    @Min(1)
    @Max(200)
    @Column(nullable = false)
    private Integer capacity;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClassLevel level;
}
