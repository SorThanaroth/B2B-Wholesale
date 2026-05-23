package com.wholesale.marketplace.modules.report;

import com.wholesale.marketplace.modules.order.Order;
import com.wholesale.marketplace.modules.report.dto.CompanyAmount;
import com.wholesale.marketplace.modules.report.dto.MerchantActivity;
import com.wholesale.marketplace.modules.report.dto.ProductAmount;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-only analytics queries (Section 9.11). Bound to {@link Order} as a
 * convenient domain type, but the JPQL spans order, items, and splits.
 * Only PAID orders count toward revenue.
 */
@Repository
public interface ReportRepository extends org.springframework.data.jpa.repository.JpaRepository<Order, UUID> {

    @Query("""
            select sum(o.totalAmount) from Order o
            where o.userId = :userId
              and o.paymentStatus = com.wholesale.marketplace.modules.payment.PaymentStatus.PAID
            """)
    BigDecimal sumPaidByUser(@Param("userId") UUID userId);

    @Query("""
            select sum(o.totalAmount) from Order o
            where o.paymentStatus = com.wholesale.marketplace.modules.payment.PaymentStatus.PAID
            """)
    BigDecimal sumTotalRevenue();

    @Query("""
            select new com.wholesale.marketplace.modules.report.dto.CompanyAmount(
                       oi.companyId, sum(oi.subtotal), count(distinct oi.orderId))
            from OrderItem oi, Order o
            where o.id = oi.orderId
              and o.userId = :userId
              and o.paymentStatus = com.wholesale.marketplace.modules.payment.PaymentStatus.PAID
            group by oi.companyId
            order by sum(oi.subtotal) desc
            """)
    List<CompanyAmount> spendingByCompanyForUser(@Param("userId") UUID userId);

    @Query("""
            select new com.wholesale.marketplace.modules.report.dto.CompanyAmount(
                       oi.companyId, sum(oi.subtotal), count(distinct oi.orderId))
            from OrderItem oi, Order o
            where o.id = oi.orderId
              and o.paymentStatus = com.wholesale.marketplace.modules.payment.PaymentStatus.PAID
              and o.createdAt between :from and :to
            group by oi.companyId
            order by sum(oi.subtotal) desc
            """)
    List<CompanyAmount> revenueByCompany(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select new com.wholesale.marketplace.modules.report.dto.ProductAmount(
                       oi.productId, max(oi.productName), sum(oi.quantity), sum(oi.subtotal))
            from OrderItem oi, Order o
            where o.id = oi.orderId
              and o.paymentStatus = com.wholesale.marketplace.modules.payment.PaymentStatus.PAID
            group by oi.productId
            order by sum(oi.quantity) desc
            """)
    List<ProductAmount> topProducts(Pageable pageable);

    @Query("""
            select new com.wholesale.marketplace.modules.report.dto.MerchantActivity(
                       o.userId, count(o.id), sum(o.totalAmount))
            from Order o
            where o.paymentStatus = com.wholesale.marketplace.modules.payment.PaymentStatus.PAID
            group by o.userId
            order by sum(o.totalAmount) desc
            """)
    List<MerchantActivity> merchantActivity(Pageable pageable);
}
