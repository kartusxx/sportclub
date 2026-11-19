package ru.teamname.sportclub.exception;

import java.time.LocalDateTime;
import java.util.List;

/** Тело ответа при ошибке. */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details) {
}
