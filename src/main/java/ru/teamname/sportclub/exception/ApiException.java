package ru.teamname.sportclub.exception;

import org.springframework.http.HttpStatus;

/** Базовое исключение прикладного уровня: знает, с каким HTTP-статусом нужно ответить клиенту. */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
