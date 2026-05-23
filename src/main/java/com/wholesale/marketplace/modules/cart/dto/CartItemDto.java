package com.wholesale.marketplace.modules.cart.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemDto(
        UUID id,
        UUID productId,
        String productName,
        String unit,
        BigDecimal unitPrice,
        int quantity,
        int minOrderQty,
        BigDecimal subtotal
) {}
