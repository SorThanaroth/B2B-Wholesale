package com.wholesale.marketplace.modules.order.dto;

import com.wholesale.marketplace.modules.order.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull(message = "status is required") OrderStatus status
) {}
