package ru.teamname.sportclub.exception;

import org.springframework.http.HttpStatus;

/** 422 — запрос корректен синтаксически, но нарушает бизнес-правило. */
public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
