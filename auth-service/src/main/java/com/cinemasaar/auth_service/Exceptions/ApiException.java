package com.cinemasaar.auth_service.Exceptions;

import org.springframework.http.HttpStatus;

/** Throw this from services to return a specific HTTP status + message. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
