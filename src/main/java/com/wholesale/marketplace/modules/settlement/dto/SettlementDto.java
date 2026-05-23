package com.wholesale.marketplace.modules.settlement.dto;

import com.wholesale.marketplace.modules.order.SplitStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One company's payout obligation for one order, with destination bank account. */
public record SettlementDto(
        UUID splitId,
        UUID orderId,
        UUID companyId,
        String companyName,
        String bankAccount,
        BigDecimal subtotal,
        SplitStatus status,
        Instant paidAt,
        Instant settledAt
) {}
