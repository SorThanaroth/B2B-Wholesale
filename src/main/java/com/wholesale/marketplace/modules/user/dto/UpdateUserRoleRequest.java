package com.wholesale.marketplace.modules.user.dto;

import com.wholesale.marketplace.modules.auth.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
        @NotNull(message = "role is required") Role role
) {}
