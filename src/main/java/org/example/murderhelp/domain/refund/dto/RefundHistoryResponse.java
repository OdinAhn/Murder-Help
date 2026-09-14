package org.example.murderhelp.domain.refund.dto;

import org.example.murderhelp.domain.refund.entity.Refund;
import org.example.murderhelp.domain.refund.entity.RefundItem;

import java.time.LocalDateTime;
import java.util.List;

public record RefundHistoryResponse(
        Long refundId,
        LocalDateTime refundDate,
        String status,
        Long pgRefundAmount,
        List<RefundHistoryItemResponse> items
) {
    public static RefundHistoryResponse from(Refund refund, List<RefundItem> refundItems) {
        return new RefundHistoryResponse(
                refund.getId(),
                refund.getCreatedAt(),
                refund.getStatus().name(),
                refund.getPgRefundAmount(),
                refundItems.stream()
                        .map(RefundHistoryItemResponse::from)
                        .toList()
        );
    }
}