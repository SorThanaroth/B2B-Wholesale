package com.wholesale.marketplace.modules.order.dto;

import com.wholesale.marketplace.modules.order.FulfillmentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateFulfillmentRequest(
        @NotNull(message = "status is required") FulfillmentStatus status
) {}
