package com.director_appraisal.form_data_service.exception;

import com.director_appraisal.form_data_service.config.MdcLoggingFilter;
import com.director_appraisal.form_data_service.dto.config.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String SERVICE_NAME = "form-data-service";

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiErrorResponse> handleSecurity(SecurityException e, HttpServletRequest req) {
        log.warn("[SECURITY_VIOLATION] path={} message={}", req.getRequestURI(), e.getMessage());
        return build(HttpStatus.FORBIDDEN, "ACCESS_DENIED", e.getMessage(), req, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest req) {
        log.warn("[BAD_REQUEST] path={} message={}", req.getRequestURI(), e.getMessage());
        return build(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", e.getMessage(), req, null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(IllegalStateException e, HttpServletRequest req) {
        log.warn("[INVALID_STATE] path={} message={}", req.getRequestURI(), e.getMessage());
        return build(HttpStatus.BAD_REQUEST, "INVALID_STATE", e.getMessage(), req, null);
    }

    @ExceptionHandler({NoSuchElementException.class, org.springframework.web.servlet.resource.NoResourceFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleNotFound(Exception e, HttpServletRequest req) {
        log.warn("[RESOURCE_NOT_FOUND] path={} message={}", req.getRequestURI(), e.getMessage());
        return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", e.getMessage(), req, null);
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(org.springframework.web.HttpRequestMethodNotSupportedException e, HttpServletRequest req) {
        log.warn("[METHOD_NOT_SUPPORTED] path={} method={}", req.getRequestURI(), e.getMethod());
        return build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", e.getMessage(), req, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException e, HttpServletRequest req) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        log.warn("[VALIDATION_ERROR] path={} errors={}", req.getRequestURI(), errors);
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed.", req, errors);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException e, HttpServletRequest req) {
        Throwable rootCause = getRootCause(e);
        log.error("[DATABASE_CONSTRAINT_VIOLATION] path={} message={} rootCause={}",
                req.getRequestURI(), e.getMessage(), rootCause.getMessage(), e);
        return build(HttpStatus.CONFLICT, "DATABASE_CONSTRAINT_VIOLATION", "Database constraint or duplicate entity code.", req, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneralException(Exception e, HttpServletRequest req) {
        Throwable rootCause = getRootCause(e);
        log.error("[UNHANDLED_ERROR] path={} exception={} message={} rootCauseClass={} rootCauseMsg={}",
                req.getRequestURI(), e.getClass().getName(), e.getMessage(),
                rootCause.getClass().getName(), rootCause.getMessage(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                "An unexpected internal error occurred. Please contact system support.", req, null);
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String code, String message,
                                                   HttpServletRequest req, Object details) {
        String correlationId = MDC.get(MdcLoggingFilter.MDC_CORRELATION_ID);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = req.getHeader(MdcLoggingFilter.CORRELATION_ID_HEADER);
        }

        ApiErrorResponse response = ApiErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(status.value())
                .error(status.getReasonPhrase())
                .code(code)
                .message(message)
                .service(SERVICE_NAME)
                .path(req != null ? req.getRequestURI() : "unknown")
                .correlationId(correlationId)
                .details(details)
                .build();

        return ResponseEntity.status(status).body(response);
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }
}
