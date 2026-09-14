package org.example.murderhelp.domain.refund.dto;


import org.example.murderhelp.domain.refund.entity.RefundItem;

public record RefundHistoryItemResponse(
        Long refundItemId,
        String productName,
        int refundQuantity,
        Long itemRefundAmount
) {
    public static RefundHistoryItemResponse from(RefundItem item) {
        return new RefundHistoryItemResponse(
                item.getId(),
                item.getOrderItem().getProduct().getName(),
                item.getRefundQuantity(),
                item.getPgRefundAmount()
        );
    }
}
