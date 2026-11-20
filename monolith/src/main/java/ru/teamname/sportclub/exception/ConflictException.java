package ru.teamname.sportclub.exception;

import org.springframework.http.HttpStatus;

/** 409 — операция конфликтует с текущим состоянием данных (дубликат, нет мест и т.п.). */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
