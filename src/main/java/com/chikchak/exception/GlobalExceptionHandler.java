// Global Exception Handler

package com.chikchak.exception;

import com.chikchak.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ChikChakException.class)
    public ResponseEntity<ErrorResponse> handleChikChakException(ChikChakException ex, WebRequest request) {
        log.error("ChikChak exception: {}", ex.getMessage(), ex);
        return build(ex.getStatus(), ex.getMessage(), request);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceededException(
            RateLimitExceededException ex, WebRequest request) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());
        return build(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = error instanceof FieldError fe ? fe.getField() : error.getObjectName();
            fieldErrors.put(fieldName, error.getDefaultMessage());
        });

        ErrorResponse error = ErrorResponse.builder()
                .error("Validation Failed")
                .message("One or more fields are invalid")
                .timestamp(LocalDateTime.now().toString())
                .status(HttpStatus.BAD_REQUEST.value())
                .path(path(request))
                .fieldErrors(fieldErrors)
                .build();

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // @RequestParam / @PathVariable constraint violations (e.g. @Min on a method param)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, "One or more parameters are invalid", request);
    }

    // Malformed / unparseable JSON body
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        log.warn("Malformed request body: {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.BAD_REQUEST, "Malformed request body", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Missing required parameter: " + ex.getParameterName(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Invalid value for parameter: " + ex.getName(), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, WebRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not supported for this endpoint", request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUpload(MaxUploadSizeExceededException ex, WebRequest request) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file is too large", request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        log.warn("Bad credentials");
        return build(HttpStatus.UNAUTHORIZED, "Authentication Failed", "Invalid email or password", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "Access Denied", "You don't have permission to access this resource", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, WebRequest request) {
        return build(status, status.getReasonPhrase(), message, request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String errorLabel, String message, WebRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .error(errorLabel)
                .message(message)
                .timestamp(LocalDateTime.now().toString())
                .status(status.value())
                .path(path(request))
                .build();
        return new ResponseEntity<>(error, status);
    }

    private String path(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
