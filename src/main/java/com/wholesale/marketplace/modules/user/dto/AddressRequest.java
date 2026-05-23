package com.wholesale.marketplace.modules.user.dto;

import jakarta.validation.constraints.NotBlank;

public record AddressRequest(
        String label,
        @NotBlank(message = "Street is required") String street,
        @NotBlank(message = "City is required") String city,
        String province,
        boolean isDefault
) {}
