package com.wholesale.marketplace.modules.cart.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Cart lines grouped by supplier company, with that company's subtotal (Section 7.1). */
public record CartCompanyGroup(
        UUID companyId,
        String companyName,
        BigDecimal subtotal,
        List<CartItemDto> items
) {}
