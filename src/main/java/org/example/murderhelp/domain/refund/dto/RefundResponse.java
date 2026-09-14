package org.example.murderhelp.domain.refund.dto;

import org.example.murderhelp.domain.refund.entity.Refund;
import org.example.murderhelp.domain.refund.entity.RefundStatus;

import java.time.LocalDateTime;

public record RefundResponse(
        Long refundId,
        RefundStatus status,
        Long pgRefundAmount,
        LocalDateTime refundedAt
) {
    public static RefundResponse from(Refund refund) {
        return new RefundResponse(
                refund.getId(),
                refund.getStatus(),
                refund.getPgRefundAmount(),
                refund.getCreatedAt()
        );
    }
}
