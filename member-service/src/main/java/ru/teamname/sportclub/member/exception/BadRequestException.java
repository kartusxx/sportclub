package ru.teamname.sportclub.member.exception;

import org.springframework.http.HttpStatus;

/** 400 — некорректные параметры запроса (например, сортировка по несуществующему полю). */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
