package ru.teamname.sportclub.member.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;

import java.time.LocalDateTime;
import java.util.List;

/** Превращает исключения контроллеров в человекочитаемые ответы {@link ApiError} (WebFlux-версия). */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex, ServerHttpRequest request) {
        return build(ex.getStatus(), ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiError> handleBodyValidation(WebExchangeBindException ex, ServerHttpRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Ошибка валидации входных данных", request, details);
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ApiError> handleUnreadableInput(ServerWebInputException ex, ServerHttpRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "Тело запроса или параметр имеет неверный формат (проверьте JSON, даты, числа и значения enum)",
                request, List.of());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleEntityValidation(ConstraintViolationException ex,
                                                           ServerHttpRequest request) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .sorted()
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Ошибка валидации данных", request, details);
    }

    /** JPA оборачивает нарушение валидации сущности при коммите в TransactionSystemException. */
    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<ApiError> handleTransactionFailure(TransactionSystemException ex,
                                                             ServerHttpRequest request) {
        Throwable root = ex.getRootCause();
        if (root instanceof ConstraintViolationException violation) {
            return handleEntityValidation(violation, request);
        }
        log.error("Ошибка транзакции", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось завершить операцию", request, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex,
                                                        ServerHttpRequest request) {
        log.warn("Нарушение целостности данных: {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT,
                "Операция нарушает ограничения данных: запись дублируется или на неё ссылаются другие записи",
                request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, ServerHttpRequest request) {
        // Стандартные ошибки Spring (404 для неизвестного URL, 405, 415 и т.д.) сохраняют свой статус
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatus status = HttpStatus.valueOf(errorResponse.getStatusCode().value());
            String detail = errorResponse.getBody().getDetail();
            return build(status, detail != null ? detail : status.getReasonPhrase(), request, List.of());
        }
        log.error("Непредвиденная ошибка", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера", request, List.of());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, ServerHttpRequest request,
                                           List<String> details) {
        ApiError body = new ApiError(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message,
                request.getPath().value(), details);
        return ResponseEntity.status(status).body(body);
    }
}
