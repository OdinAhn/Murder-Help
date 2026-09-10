package org.example.murderhelp.domain.order.repository;

import org.example.murderhelp.domain.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

//  TODO: LEFT JOIN reviews r ON oi.id = r.order_item_id 추가
    @Query("""
    SELECT oi FROM OrderItem oi
    WHERE oi.order.id IN :orderIds
    ORDER BY oi.id ASC
    """)
    List<OrderItem> findAllWithReviewByOrderIdIn(@Param("orderIds") List<Long> orderIds);

    List<OrderItem> findAllByOrder_Id(Long orderId);
}