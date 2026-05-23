package com.wholesale.marketplace.modules.report.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** JPQL aggregate projection: units sold + revenue for one product. */
public record ProductAmount(UUID productId, String productName, Long quantitySold, BigDecimal revenue) {}
