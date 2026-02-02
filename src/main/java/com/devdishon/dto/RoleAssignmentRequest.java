package com.devdishon.dto;

import com.devdishon.entity.RoleName;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record RoleAssignmentRequest(
        @NotNull(message = "User ID is required")
        Long userId,

        @NotEmpty(message = "At least one role must be specified")
        Set<RoleName> roles
) {
}
