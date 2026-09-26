package com.dev.csas.api.error;

import com.dev.csas.attendance.AttendanceSheetNotFoundException;
import com.dev.csas.attendance.DuplicateLectureException;
import com.dev.csas.attendance.InvalidRegisterException;
import com.dev.csas.attendance.LectureNotFoundException;
import com.dev.csas.control.ControlSheetException;
import com.dev.csas.control.IncompleteControlSheetException;
import com.dev.csas.control.InvalidControlResultException;
import com.dev.csas.control.UnauthorizedTeacherException;
import com.dev.csas.course.NonExistentStudentException;
import com.dev.csas.error.CatalogBadRequestException;
import com.dev.csas.error.CatalogConflictException;
import com.dev.csas.error.CatalogNotFoundException;
import com.dev.csas.security.ActorAccessDeniedException;
import com.dev.csas.shared.SheetStatusException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice(basePackages = {
        "com.dev.csas.api",
        "com.dev.csas.group",
        "com.dev.csas.student",
        "com.dev.csas.teacher",
        "com.dev.csas.discipline",
        "com.dev.csas.semester"
})
public class ApiExceptionHandler {

    @ExceptionHandler({AttendanceSheetNotFoundException.class, LectureNotFoundException.class,
            NonExistentStudentException.class, com.dev.csas.control.ControlSheetNotFoundException.class,
            CatalogNotFoundException.class})
    public ResponseEntity<ApiErrorResponse> notFound(RuntimeException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler({CatalogConflictException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ApiErrorResponse> catalogConflict(RuntimeException exception, HttpServletRequest request) {
        String message = exception instanceof DataIntegrityViolationException
                ? "Нельзя выполнить операцию: нарушено ограничение уникальности или связи"
                : exception.getMessage();
        return error(HttpStatus.CONFLICT, message, request);
    }

    @ExceptionHandler(CatalogBadRequestException.class)
    public ResponseEntity<ApiErrorResponse> catalogBadRequest(
            CatalogBadRequestException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler({ActorAccessDeniedException.class, UnauthorizedTeacherException.class})
    public ResponseEntity<ApiErrorResponse> forbidden(RuntimeException exception, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, exception.getMessage(), request);
    }

    @ExceptionHandler({SheetStatusException.class, IncompleteControlSheetException.class,
            InvalidRegisterException.class, DuplicateLectureException.class})
    public ResponseEntity<ApiErrorResponse> conflict(RuntimeException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler({InvalidControlResultException.class})
    public ResponseEntity<ApiErrorResponse> invalidResult(RuntimeException exception, HttpServletRequest request) {
        return error(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage(), request);
    }

    @ExceptionHandler(ControlSheetException.class)
    public ResponseEntity<ApiErrorResponse> controlSheetError(ControlSheetException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> invalidRequest(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> unreadableRequest(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.BAD_REQUEST, "Некорректное тело запроса", request);
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        ));
    }
}
