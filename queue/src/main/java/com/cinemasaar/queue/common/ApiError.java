package com.cinemasaar.queue.common;

public record ApiError(String code, String message, Object details) {

    public ApiError(String code, String message) {
        this(code, message, null);
    }
}
