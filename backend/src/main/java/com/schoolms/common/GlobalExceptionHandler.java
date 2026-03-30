package com.schoolms.common;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Object>> handleApp(AppException ex) {
        return ResponseEntity.status(ex.getStatus()).body(new ApiResponse<>(false, ex.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a, b) -> a));
        return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Validation failed", errors));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleBind(BindException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a, b) -> a));
        return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Validation failed", errors));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(401).body(new ApiResponse<>(false, "Invalid credentials", null));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String detail = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        String normalized = detail == null ? "" : detail.toLowerCase();
        String message;
        HttpStatus status;
        if (normalized.contains("admission_number")) {
            message = "Admission number already exists";
            status = HttpStatus.CONFLICT;
        } else if (normalized.contains("school_class_id")) {
            message = "Selected class does not exist";
            status = HttpStatus.BAD_REQUEST;
        } else if (normalized.contains("grade")) {
            message = "Selected class is missing a grade mapping";
            status = HttpStatus.BAD_REQUEST;
        } else if (normalized.contains("status")) {
            message = "Status is required";
            status = HttpStatus.BAD_REQUEST;
        } else if (normalized.contains("not-null")) {
            if (normalized.contains("admission_number")) {
                message = "Admission number is required";
            } else if (normalized.contains("first_name")) {
                message = "First name is required";
            } else if (normalized.contains("last_name")) {
                message = "Last name is required";
            } else if (normalized.contains("gender")) {
                message = "Gender is required";
            } else if (normalized.contains("school_class_id")) {
                message = "Class is required";
            } else if (normalized.contains("enrollment_date")) {
                message = "Enrollment date is required";
            } else if (normalized.contains("grade")) {
                message = "Grade is required";
            } else if (normalized.contains("status")) {
                message = "Status is required";
            } else {
                message = "A required field is missing";
            }
            status = HttpStatus.BAD_REQUEST;
        } else {
            message = "Data integrity violation";
            status = HttpStatus.CONFLICT;
        }
        return ResponseEntity.status(status).body(new ApiResponse<>(false, message, null));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        String detail = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        String normalized = detail == null ? "" : detail.toLowerCase();
        String message;
        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof InvalidFormatException invalidFormatException && !invalidFormatException.getPath().isEmpty()) {
            String field = invalidFormatException.getPath().get(0).getFieldName();
            if (normalized.contains("localdate")) {
                message = field + " must be in yyyy-MM-dd format";
            } else {
                message = field + " has an invalid value";
            }
        } else if (normalized.contains("localdate")) {
            message = "Date value must be in yyyy-MM-dd format";
        } else if (normalized.contains("studentstatus")) {
            message = "Invalid status value";
        } else if (normalized.contains("gender")) {
            message = "Invalid gender value. Allowed values: MALE, FEMALE, OTHER";
        } else if (normalized.contains("guardianrelationship")) {
            message = "Invalid guardian relationship value";
        } else if (normalized.contains("required request body is missing")) {
            message = "Request body is required";
        } else if (normalized.contains("json parse error")) {
            message = "Malformed request payload";
        } else {
            message = "Request body is invalid";
        }
        return ResponseEntity.badRequest().body(new ApiResponse<>(false, message, null));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Validation failed for parameter: " + ex.getName(), null));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(item -> item.getMessage())
                .orElse("Validation failed");
        return ResponseEntity.badRequest().body(new ApiResponse<>(false, message, null));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleEntityNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>(false, ex.getMessage(), null));
    }
}
