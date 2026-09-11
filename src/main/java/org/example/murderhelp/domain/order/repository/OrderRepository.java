package org.example.murderhelp.domain.order.repository;

import org.example.murderhelp.domain.order.entity.Order;
import org.example.murderhelp.domain.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
    SELECT o FROM Order o
    WHERE (:from IS NULL OR o.createdAt >= :from)
    AND   (:status IS NULL OR o.status = :status)
    AND   o.member.id = :memberId
    """)
    Page<Order> findAllListPage(
            @Param("memberId") Long memberId,
            @Param("from") LocalDateTime from,
            @Param("status") OrderStatus status,
            Pageable pageable
    );

}
