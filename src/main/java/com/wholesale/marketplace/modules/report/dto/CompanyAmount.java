package com.wholesale.marketplace.modules.report.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** JPQL aggregate projection: revenue + distinct order count for one company. */
public record CompanyAmount(UUID companyId, BigDecimal revenue, Long orderCount) {}
