package com.wholesale.marketplace.modules.product.dto;

import java.util.List;

/** Outcome of a CSV bulk import (Section 9.5 POST /products/import). */
public record ImportResult(
        int imported,
        int failed,
        List<String> errors
) {}
