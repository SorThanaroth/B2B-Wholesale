package com.wholesale.marketplace.modules.category.dto;

import com.wholesale.marketplace.modules.category.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Category node; {@code children} is populated when returned as a tree. */
public record CategoryDto(
        UUID id,
        String name,
        String description,
        UUID parentId,
        List<CategoryDto> children
) {
    public static CategoryDto flat(Category c) {
        return new CategoryDto(c.getId(), c.getName(), c.getDescription(), c.getParentId(), new ArrayList<>());
    }
}
