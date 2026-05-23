package com.wholesale.marketplace.modules.category;

import com.wholesale.marketplace.common.exception.BadRequestException;
import com.wholesale.marketplace.common.exception.ResourceNotFoundException;
import com.wholesale.marketplace.modules.category.dto.CategoryDto;
import com.wholesale.marketplace.modules.category.dto.CategoryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /** Returns the full category forest (root nodes with nested children). */
    @Transactional(readOnly = true)
    public List<CategoryDto> tree() {
        List<Category> all = categoryRepository.findAll();
        Map<UUID, CategoryDto> byId = new LinkedHashMap<>();
        for (Category c : all) {
            byId.put(c.getId(), CategoryDto.flat(c));
        }
        List<CategoryDto> roots = new ArrayList<>();
        for (Category c : all) {
            CategoryDto node = byId.get(c.getId());
            if (c.getParentId() != null && byId.containsKey(c.getParentId())) {
                byId.get(c.getParentId()).children().add(node);
            } else {
                roots.add(node);
            }
        }
        return roots;
    }

    @Transactional(readOnly = true)
    public CategoryDto get(UUID id) {
        return CategoryDto.flat(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Category getEntity(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    @Transactional
    public CategoryDto create(CategoryRequest req) {
        validateParent(req.parentId(), null);
        Category category = Category.builder()
                .name(req.name())
                .description(req.description())
                .parentId(req.parentId())
                .build();
        return CategoryDto.flat(categoryRepository.save(category));
    }

    @Transactional
    public CategoryDto update(UUID id, CategoryRequest req) {
        Category category = getEntity(id);
        validateParent(req.parentId(), id);
        category.setName(req.name());
        category.setDescription(req.description());
        category.setParentId(req.parentId());
        return CategoryDto.flat(categoryRepository.save(category));
    }

    @Transactional
    public void delete(UUID id) {
        getEntity(id);
        if (categoryRepository.existsByParentId(id)) {
            throw new BadRequestException("Cannot delete a category that has sub-categories");
        }
        categoryRepository.deleteById(id);
    }

    private void validateParent(UUID parentId, UUID selfId) {
        if (parentId == null) return;
        if (parentId.equals(selfId)) {
            throw new BadRequestException("A category cannot be its own parent");
        }
        if (!categoryRepository.existsById(parentId)) {
            throw new BadRequestException("Parent category does not exist");
        }
    }
}
