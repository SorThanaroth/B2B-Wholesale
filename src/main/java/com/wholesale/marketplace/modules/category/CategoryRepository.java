package com.wholesale.marketplace.modules.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    boolean existsByParentId(UUID parentId);

    List<Category> findByParentId(UUID parentId);
}
