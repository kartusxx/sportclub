package ru.teamname.sportclub.exception;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.ErrorResponseException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");

    @Test
    void apiExceptionKeepsItsStatusAndMessage() {
        ResponseEntity<ApiError> response = handler.handleApiException(new ResourceNotFoundException("Тест", 1L), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().path()).isEqualTo("/api/test");
        assertThat(response.getBody().message()).contains("1");
    }

    @Test
    void businessRuleAndConflictMapToExpectedStatuses() {
        assertThat(handler.handleApiException(new BusinessRuleException("rule"), request).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(handler.handleApiException(new ConflictException("conflict"), request).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void entityValidationFailureMapsTo400() {
        ResponseEntity<ApiError> response = handler.handleEntityValidation(
                new ConstraintViolationException("invalid", Set.of()), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void transactionFailureCausedByValidationMapsTo400() {
        TransactionSystemException exception = new TransactionSystemException("commit failed",
                new ConstraintViolationException("invalid", Set.of()));

        assertThat(handler.handleTransactionFailure(exception, request).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void otherTransactionFailureMapsTo500() {
        TransactionSystemException exception = new TransactionSystemException("commit failed",
                new IllegalStateException("boom"));

        assertThat(handler.handleTransactionFailure(exception, request).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void dataIntegrityViolationMapsTo409() {
        ResponseEntity<ApiError> response = handler.handleDataIntegrity(
                new DataIntegrityViolationException("fk violation"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void springMvcErrorsKeepTheirOwnStatus() {
        ResponseEntity<ApiError> response = handler.handleUnexpected(
                new ErrorResponseException(HttpStatus.METHOD_NOT_ALLOWED), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    void unexpectedErrorMapsTo500WithoutLeakingDetails() {
        ResponseEntity<ApiError> response = handler.handleUnexpected(new RuntimeException("secret"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).doesNotContain("secret");
    }
}
