package com.wholesale.marketplace.modules.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderCompanySplitRepository
        extends JpaRepository<OrderCompanySplit, UUID>, JpaSpecificationExecutor<OrderCompanySplit> {
    List<OrderCompanySplit> findByOrderId(UUID orderId);

    long countByPaymentStatus(SplitStatus paymentStatus);

    // ----- supplier-scoped (one split per order the company is part of) -----
    Page<OrderCompanySplit> findByCompanyId(UUID companyId, Pageable pageable);

    Page<OrderCompanySplit> findByCompanyIdAndPaymentStatus(UUID companyId, SplitStatus status, Pageable pageable);

    Optional<OrderCompanySplit> findByOrderIdAndCompanyId(UUID orderId, UUID companyId);

    /** Batch lookup of a company's splits for a page of orders (avoids N+1). */
    List<OrderCompanySplit> findByCompanyIdAndOrderIdIn(UUID companyId, Collection<UUID> orderIds);

    long countByCompanyId(UUID companyId);

    long countByCompanyIdAndPaymentStatus(UUID companyId, SplitStatus status);

    @Query("select coalesce(sum(s.subtotal), 0) from OrderCompanySplit s " +
            "where s.companyId = :companyId and s.paymentStatus = :status")
    BigDecimal sumSubtotalByCompanyAndStatus(@Param("companyId") UUID companyId,
                                             @Param("status") SplitStatus status);
}
