package com.wholesale.marketplace.modules.cart.dto;

import jakarta.validation.constraints.Min;

public record UpdateItemRequest(
        @Min(value = 1, message = "Quantity must be at least 1") int quantity
) {}
