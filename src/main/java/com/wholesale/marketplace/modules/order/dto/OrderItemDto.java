package com.wholesale.marketplace.modules.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemDto(
        UUID id,
        UUID productId,
        String productName,
        UUID companyId,
        String companyName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}
