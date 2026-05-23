package com.wholesale.marketplace.modules.product;

import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.UUID;

/** Composable JPA predicates backing the catalog filters (Section 9.5 GET /products). */
public final class ProductSpecifications {

    private ProductSpecifications() {}

    public static Specification<Product> status(ProductStatus status) {
        return (root, q, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Product> company(UUID companyId) {
        return (root, q, cb) -> companyId == null ? null : cb.equal(root.get("companyId"), companyId);
    }

    public static Specification<Product> category(UUID categoryId) {
        return (root, q, cb) -> categoryId == null ? null : cb.equal(root.get("categoryId"), categoryId);
    }

    /** Case-insensitive match across name and description. */
    public static Specification<Product> search(String term) {
        return (root, q, cb) -> {
            if (term == null || term.isBlank()) return null;
            String like = "%" + term.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(cb.coalesce(root.get("description"), "")), like)
            );
        };
    }

    public static Specification<Product> minPrice(BigDecimal min) {
        return (root, q, cb) -> min == null ? null : cb.greaterThanOrEqualTo(root.get("price"), min);
    }

    public static Specification<Product> maxPrice(BigDecimal max) {
        return (root, q, cb) -> max == null ? null : cb.lessThanOrEqualTo(root.get("price"), max);
    }
}
