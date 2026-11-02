package com.cinemasaar.recommendationservice.exception;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.*;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;

// Returns safe errors without leaking provider payloads or credentials.
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<?> invalid(Exception e, HttpServletRequest request) { return error(HttpStatus.BAD_REQUEST, "Invalid preferences", request); }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> unavailable(Exception e, HttpServletRequest request) { return error(HttpStatus.SERVICE_UNAVAILABLE, "Movie catalogue temporarily unavailable", request); }
    private ResponseEntity<?> error(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(Map.of("timestamp", Instant.now(), "status", status.value(),
                "error", status.name(), "message", message, "path", request.getRequestURI()));
    }
}
