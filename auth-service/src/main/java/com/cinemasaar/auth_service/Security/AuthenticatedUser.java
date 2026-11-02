package com.cinemasaar.auth_service.Security;

/** The user extracted from a valid JWT; available in controllers via @AuthenticationPrincipal. */
public record AuthenticatedUser(String userId, String email, String role) {
}
