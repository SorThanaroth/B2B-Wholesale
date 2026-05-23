package com.wholesale.marketplace.modules.supplier.dto;

import com.wholesale.marketplace.modules.product.dto.ProductRequest;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Product create/update from the supplier portal. Deliberately omits companyId —
 * the company is taken from the authenticated supplier so it can't be spoofed.
 */
public record SupplierProductRequest(
        UUID categoryId,
        @NotBlank(message = "Product name is required") String name,
        String description,
        @NotNull @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive") BigDecimal price,
        @Min(value = 1, message = "Minimum order quantity must be at least 1") int minOrderQty,
        @NotBlank String unit,
        @Min(value = 0, message = "Stock cannot be negative") int stock,
        String imageUrl
) {
    /** Bind to the shared ProductRequest with the supplier's own company. */
    public ProductRequest toProductRequest(UUID companyId) {
        return new ProductRequest(companyId, categoryId, name, description, price,
                minOrderQty, unit, stock, imageUrl);
    }
}
