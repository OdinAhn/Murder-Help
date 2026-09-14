package org.example.murderhelp.domain.refund.dto;

import org.example.murderhelp.domain.order.entity.OrderItem;

public record RefundableItemResponse(
        Long orderItemId,
        String productName,
        Long orderPrice,
        int originalQuantity,
        int remainQuantity
) {
    public static RefundableItemResponse from(OrderItem item, int remainQuantity) {
        return new RefundableItemResponse(
                item.getId(),
                item.getProductName(),
                item.getUnitPrice(),
                item.getQuantity(),
                remainQuantity
        );
    }
}
