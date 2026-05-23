package com.wholesale.marketplace.modules.settlement;

import com.wholesale.marketplace.modules.order.OrderCompanySplit;
import com.wholesale.marketplace.modules.order.SplitStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

/** Filters for the admin settlement dashboard (Section 9.9). */
public final class SettlementSpecifications {

    private SettlementSpecifications() {}

    public static Specification<OrderCompanySplit> status(SplitStatus status) {
        return (root, q, cb) -> status == null ? null : cb.equal(root.get("paymentStatus"), status);
    }

    public static Specification<OrderCompanySplit> company(UUID companyId) {
        return (root, q, cb) -> companyId == null ? null : cb.equal(root.get("companyId"), companyId);
    }

    public static Specification<OrderCompanySplit> paidFrom(Instant from) {
        return (root, q, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("paidAt"), from);
    }

    public static Specification<OrderCompanySplit> paidTo(Instant to) {
        return (root, q, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("paidAt"), to);
    }
}
