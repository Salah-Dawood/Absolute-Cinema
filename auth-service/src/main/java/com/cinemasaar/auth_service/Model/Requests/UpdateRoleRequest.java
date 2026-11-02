package com.cinemasaar.auth_service.Model.Requests;

import com.cinemasaar.auth_service.Model.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(
        @NotNull(message = "Role is required")
        Role role
) {
}
