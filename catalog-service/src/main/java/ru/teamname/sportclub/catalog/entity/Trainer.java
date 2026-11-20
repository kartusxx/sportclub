package ru.teamname.sportclub.catalog.entity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Table("trainers")
public class Trainer {

    @Id
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column("first_name")
    private String firstName;

    @NotBlank
    @Size(max = 100)
    @Column("last_name")
    private String lastName;

    @NotBlank
    @Email
    @Size(max = 255)
    @Column("email")
    private String email;

    @NotNull
    @Min(0)
    @Max(60)
    @Column("experience_years")
    private Integer experienceYears = 0;

    @Size(max = 1000)
    @Column("bio")
    private String bio;

    /**
     * Many to Many: тренер ведёт несколько направлений, направление ведут несколько тренеров.
     * Spring Data R2DBC не умеет маппить связи, поэтому поле не сохраняется автоматически:
     * строки таблицы trainer_disciplines читает и пишет {@code TrainerDisciplineRepository}.
     */
    @Transient
    private Set<Discipline> disciplines = new HashSet<>();

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
