package com.wholesale.marketplace.modules.order;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

/** Predicates for the admin order list (Section 9.7 GET /admin/orders). */
public final class OrderSpecifications {

    private OrderSpecifications() {}

    public static Specification<Order> status(OrderStatus status) {
        return (root, q, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Order> createdFrom(Instant from) {
        return (root, q, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<Order> createdTo(Instant to) {
        return (root, q, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    /** Orders that include at least one line from the given company. */
    public static Specification<Order> containsCompany(UUID companyId) {
        return (root, query, cb) -> {
            if (companyId == null) return null;
            Subquery<UUID> sub = query.subquery(UUID.class);
            Root<OrderItem> item = sub.from(OrderItem.class);
            sub.select(item.get("orderId")).where(cb.equal(item.get("companyId"), companyId));
            return root.get("id").in(sub);
        };
    }
}
