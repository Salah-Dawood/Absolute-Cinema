package com.cinemasaar.auth_service.Model.Responses;

import com.cinemasaar.auth_service.Model.Role;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMs,
        String userId,
        String name,
        String email,
        Role role
) {
}
