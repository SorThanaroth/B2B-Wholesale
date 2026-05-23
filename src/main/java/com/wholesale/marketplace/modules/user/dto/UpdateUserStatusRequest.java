package com.wholesale.marketplace.modules.user.dto;

import com.wholesale.marketplace.modules.auth.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull(message = "status is required") UserStatus status
) {}
