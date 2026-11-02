package com.cinemasaar.moviesservice.exception;

import java.time.Instant;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.*;
import org.springframework.web.multipart.MultipartException;

// Returns consistent, safe errors from all catalogue endpoints.
@RestControllerAdvice
public class GlobalExceptionHandler {
    private ResponseEntity<?> error(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(Map.of("timestamp", Instant.now(), "status", status.value(),
                "error", status.name(), "message", message, "path", request.getRequestURI()));
    }
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> api(ApiException e, HttpServletRequest r) { return error(e.getStatus(), e.getMessage(), r); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validation(MethodArgumentNotValidException e, HttpServletRequest r) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage()).collect(java.util.stream.Collectors.joining(", "));
        return error(HttpStatus.BAD_REQUEST, message, r);
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            ServletRequestBindingException.class, IllegalArgumentException.class, MultipartException.class})
    public ResponseEntity<?> invalid(Exception e, HttpServletRequest r) { return error(HttpStatus.BAD_REQUEST, "Invalid request", r); }
    @ExceptionHandler({DuplicateKeyException.class, ConcurrencyFailureException.class})
    public ResponseEntity<?> conflict(Exception e, HttpServletRequest r) { return error(HttpStatus.CONFLICT, "Conflicting change; reload and retry", r); }
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<?> unsupportedMediaType(Exception e, HttpServletRequest r) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type must be application/json", r);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> unexpected(Exception e, HttpServletRequest r) {
        // Mongo transaction conflicts can be wrapped by Spring's transaction manager.
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof com.mongodb.MongoException mongo && mongo.hasErrorLabel("TransientTransactionError"))
                return error(HttpStatus.CONFLICT, "Concurrent catalogue change; reload and retry", r);
        }
        return error(HttpStatus.SERVICE_UNAVAILABLE, "Service temporarily unavailable", r);
    }
}
