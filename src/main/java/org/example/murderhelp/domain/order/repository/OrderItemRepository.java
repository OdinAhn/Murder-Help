package org.example.murderhelp.domain.order.repository;

import org.example.murderhelp.domain.order.entity.OrderItem;
import org.example.murderhelp.domain.order.entity.OrderStatus;
import org.example.murderhelp.domain.product.entity.ProductTier;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("""
    SELECT oi FROM OrderItem oi
    WHERE oi.order.id IN :orderIds 
    ORDER BY oi.id ASC
    """)
    List<OrderItem> findAllByOrderIdIn(@Param("orderIds") List<Long> orderIds);

    List<OrderItem> findAllByOrder_Id(Long orderId);

    @Query("""
    SELECT oi.product.id AS productId, SUM(oi.quantity) AS totalSales 
    FROM OrderItem oi 
    WHERE oi.order.createdAt >= :startDate 
      AND oi.order.status = :status 
      AND oi.product.tier IN :allowedTiers
    GROUP BY oi.product.id 
    ORDER BY totalSales DESC
    """)
    List<Object[]> findTopSellingProductsByTiersSince(
        @Param("startDate") LocalDateTime startDate,
        @Param("status") OrderStatus status,
        @Param("allowedTiers") List<ProductTier> allowedTiers,
        Pageable pageable
    );
}