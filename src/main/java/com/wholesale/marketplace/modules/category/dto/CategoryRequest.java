package com.wholesale.marketplace.modules.category.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CategoryRequest(
        @NotBlank(message = "Category name is required") String name,
        String description,
        UUID parentId
) {}
