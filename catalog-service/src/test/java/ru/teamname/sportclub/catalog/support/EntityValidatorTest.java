package ru.teamname.sportclub.catalog.support;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import ru.teamname.sportclub.catalog.entity.Discipline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** R2DBC не запускает Bean Validation сам, поэтому сервисы вызывают EntityValidator перед записью. */
class EntityValidatorTest {

    private final EntityValidator validator = new EntityValidator();

    @Test
    void returnsValidEntityAsIs() {
        Discipline discipline = new Discipline();
        discipline.setName("Йога");

        assertThat(validator.validate(discipline)).isSameAs(discipline);
    }

    @Test
    void throwsOnConstraintViolation() {
        Discipline discipline = new Discipline();
        discipline.setName(" ");

        assertThatThrownBy(() -> validator.validate(discipline)).isInstanceOf(ConstraintViolationException.class);
    }
}
