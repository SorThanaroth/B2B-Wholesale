package com.wholesale.marketplace.modules.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {
    Page<Order> findByUserId(UUID userId, Pageable pageable);

    Optional<Order> findByIdAndUserId(UUID id, UUID userId);

    Optional<Order> findByQrToken(String qrToken);

    long countByUserId(UUID userId);

    long countByUserIdAndPaymentStatus(UUID userId, com.wholesale.marketplace.modules.payment.PaymentStatus status);

    long countByPaymentStatus(com.wholesale.marketplace.modules.payment.PaymentStatus status);
}
