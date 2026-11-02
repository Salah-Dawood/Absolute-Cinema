package com.cinemasaar.auth_service.Model.Responses;

import com.cinemasaar.auth_service.Model.Role;
import com.cinemasaar.auth_service.Model.User;

import java.time.Instant;

/** What we return to clients — never includes the password hash. */
public record UserResponse(
        String id,
        String name,
        String email,
        Role role,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
