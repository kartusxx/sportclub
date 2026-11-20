package ru.teamname.sportclub.catalog.exception;

import org.springframework.http.HttpStatus;

/** 503 — другой сервис, нужный для операции, недоступен. */
public class ServiceUnavailableException extends ApiException {

    public ServiceUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
