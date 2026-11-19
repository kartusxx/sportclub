package ru.teamname.sportclub.exception;

import org.springframework.http.HttpStatus;

/** 404 — запрошенная сущность не найдена. */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String entity, Long id) {
        super(HttpStatus.NOT_FOUND, entity + " с id=" + id + " не найден(а)");
    }
}
