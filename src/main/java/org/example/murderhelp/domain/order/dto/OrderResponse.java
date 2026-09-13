package org.example.murderhelp.domain.order.dto;

import lombok.Builder;
import org.example.murderhelp.domain.order.entity.Order;
import org.example.murderhelp.domain.order.entity.OrderItem;
import org.example.murderhelp.domain.order.entity.OrderStatus;
import org.example.murderhelp.domain.review.dto.ReviewResponse;
import org.example.murderhelp.domain.review.entity.Review;

import java.util.List;

@Builder
public record OrderResponse(
        String orderNumber,
        OrderStatus status,
        Long totalAmount,
        String receiverName,
        String receiverPhone,
        String deliveryAddress,
        String deliveryRequest,
        List<Item> items
) {

    public static OrderResponse from(Order order, List<Item> items) {
        return OrderResponse.builder()
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .deliveryAddress(order.getDeliveryAddress())
                .deliveryRequest(order.getDeliveryRequest())
                .items(items)
                .build();
    }

    public record Item(
            String productName,
            Long unitPrice,
            Integer quantity,
            ReviewResponse review
    ) {
        public static Item from(OrderItem orderItem) {
            return from(orderItem, null);
        }

        public static Item from(OrderItem orderItem, Review review) {
            return new Item(
                    orderItem.getProductName(),
                    orderItem.getUnitPrice(),
                    orderItem.getQuantity(),
                    review != null ? ReviewResponse.from(review) : null
            );
        }
    }

}
