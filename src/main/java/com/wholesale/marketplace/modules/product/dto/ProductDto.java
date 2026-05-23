package com.wholesale.marketplace.modules.product.dto;

import com.wholesale.marketplace.modules.product.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Catalog product view, enriched with resolved company/category names. */
public record ProductDto(
        UUID id,
        UUID companyId,
        String companyName,
        UUID categoryId,
        String categoryName,
        String name,
        String description,
        BigDecimal price,
        int minOrderQty,
        String unit,
        int stock,
        String imageUrl,
        ProductStatus status,
        Instant createdAt
) {}
