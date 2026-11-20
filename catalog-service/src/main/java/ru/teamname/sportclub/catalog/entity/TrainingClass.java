package ru.teamname.sportclub.catalog.entity;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import ru.teamname.sportclub.catalog.enums.ClassLevel;

import java.time.LocalDateTime;

/**
 * Занятие. Связи с тренером и направлением (Many to One) хранятся как внешние ключи,
 * потому что Spring Data R2DBC не загружает связанные сущности: имена подтягивает сервис.
 */
@Getter
@Setter
@Table("training_classes")
public class TrainingClass {

    @Id
    private Long id;

    @NotBlank
    @Size(max = 150)
    @Column("title")
    private String title;

    @Size(max = 1000)
    @Column("description")
    private String description;

    @NotNull
    @Column("trainer_id")
    private Long trainerId;

    @NotNull
    @Column("discipline_id")
    private Long disciplineId;

    @NotBlank
    @Size(max = 50)
    @Column("room")
    private String room;

    @NotNull
    @Column("starts_at")
    private LocalDateTime startsAt;

    @NotNull
    @Min(15)
    @Max(240)
    @Column("duration_minutes")
    private Integer durationMinutes;

    @NotNull
    @Min(1)
    @Max(200)
    @Column("capacity")
    private Integer capacity;

    @NotNull
    @Column("level")
    private ClassLevel level;
}
