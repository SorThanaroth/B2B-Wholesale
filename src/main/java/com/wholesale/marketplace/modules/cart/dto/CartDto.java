package com.wholesale.marketplace.modules.cart.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartDto(
        UUID cartId,
        List<CartCompanyGroup> companies,
        BigDecimal grandTotal,
        int totalItems
) {}
