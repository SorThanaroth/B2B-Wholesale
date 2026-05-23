package com.wholesale.marketplace.modules.product.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductRequest(
        @NotNull(message = "companyId is required") UUID companyId,
        UUID categoryId,
        @NotBlank(message = "Product name is required") String name,
        String description,
        @NotNull @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive") BigDecimal price,
        @Min(value = 1, message = "Minimum order quantity must be at least 1") int minOrderQty,
        @NotBlank String unit,
        @Min(value = 0, message = "Stock cannot be negative") int stock,
        String imageUrl
) {}
