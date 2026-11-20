package ru.teamname.sportclub.catalog.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@Table("disciplines")
public class Discipline {

    @Id
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column("name")
    private String name;

    @Size(max = 500)
    @Column("description")
    private String description;
}
