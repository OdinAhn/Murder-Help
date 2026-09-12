package org.example.murderhelp.domain.order.dto;

import org.example.murderhelp.domain.order.entity.Order;
import org.example.murderhelp.domain.order.entity.OrderStatus;

public record CreateOrderResponse(
        Long orderId,
        String orderNumber,
        OrderStatus status,
        Long totalAmount
) {
    public static CreateOrderResponse from(Order order) {
        return new CreateOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getTotalAmount()
        );
    }
}
