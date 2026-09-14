package org.example.murderhelp.domain.order.dto;

import org.example.murderhelp.domain.order.entity.Order;
import org.example.murderhelp.domain.order.entity.OrderStatus;
import org.example.murderhelp.domain.payment.entity.Payment;

public record CreateOrderResponse(
        Long orderId,
        Long paymentId,
        String portonePaymentId,
        String orderNumber,
        OrderStatus status,
        Long totalAmount
) {
    public static CreateOrderResponse from(Order order, Payment payment) {
        return new CreateOrderResponse(
                order.getId(),
                payment.getId(),
                payment.getPortonePaymentId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getTotalAmount()
        );
    }
}
