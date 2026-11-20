package ru.teamname.sportclub.catalog.support;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Валидация на уровне Entity. В JPA её запускает Hibernate перед сохранением, а Spring Data R2DBC
 * Bean Validation сам не вызывает, поэтому сервисы проверяют сущность явно перед записью в БД.
 * Нарушения превращаются в ответ 400 в {@code GlobalExceptionHandler}.
 */
@Component
public class EntityValidator {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    public <T> T validate(T entity) {
        Set<ConstraintViolation<T>> violations = validator.validate(entity);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return entity;
    }
}
