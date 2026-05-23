package com.wholesale.marketplace.modules.order;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * The core settlement record (Section 6): one row per company per order, holding
 * the exact amount owed to that company and its payout state.
 */
@Entity
@Table(name = "order_company_splits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCompanySplit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    private UUID companyId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SplitStatus paymentStatus = SplitStatus.PENDING;

    private Instant paidAt;

    private Instant settledAt;
}
