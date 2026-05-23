package com.wholesale.marketplace.modules.report.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** JPQL aggregate projection: order count + total spent for one merchant. */
public record MerchantActivity(UUID userId, Long orderCount, BigDecimal totalSpent) {}
