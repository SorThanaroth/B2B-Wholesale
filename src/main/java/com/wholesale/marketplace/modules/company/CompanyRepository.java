package com.wholesale.marketplace.modules.company;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {
    Page<Company> findByStatus(CompanyStatus status, Pageable pageable);

    long countByStatus(CompanyStatus status);
}
