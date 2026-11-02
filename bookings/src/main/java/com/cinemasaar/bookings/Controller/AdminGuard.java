package com.cinemasaar.bookings.Controller;

import com.cinemasaar.bookings.Exception.ApiException;
import org.springframework.http.HttpStatus;

public final class AdminGuard {

    private AdminGuard() {}

    public static void requireAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }
}