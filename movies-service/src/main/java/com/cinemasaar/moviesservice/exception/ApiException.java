package com.cinemasaar.moviesservice.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;

// Carries an expected API error without exposing infrastructure details.
@Getter
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    public ApiException(HttpStatus status, String message) { super(message); this.status = status; }
    public static ApiException notFound(String name) { return new ApiException(HttpStatus.NOT_FOUND, name + " not found"); }
    public static ApiException conflict(String message) { return new ApiException(HttpStatus.CONFLICT, message); }
    public static ApiException badRequest(String message) { return new ApiException(HttpStatus.BAD_REQUEST, message); }
}
