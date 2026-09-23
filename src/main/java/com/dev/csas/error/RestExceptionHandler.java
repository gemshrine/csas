package com.dev.csas.error;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(CatalogNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(CatalogNotFoundException ex) {
        return respond(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(CatalogConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(CatalogConflictException ex) {
        return respond(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(CatalogBadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(CatalogBadRequestException ex) {
        return respond(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return respond(HttpStatus.BAD_REQUEST, "Ошибка валидации", fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return respond(HttpStatus.BAD_REQUEST, "Некорректный формат запроса", null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleIntegrity(DataIntegrityViolationException ex) {
        return respond(
                HttpStatus.CONFLICT,
                "Нельзя выполнить операцию: нарушено ограничение уникальности или связи",
                null
        );
    }

    private static ResponseEntity<ApiErrorResponse> respond(
            HttpStatus status,
            String message,
            Map<String, String> fields
    ) {
        return ResponseEntity.status(status).body(
                new ApiErrorResponse(status.value(), status.getReasonPhrase(), message, fields)
        );
    }
}
