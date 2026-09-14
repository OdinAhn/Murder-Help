package org.example.murderhelp.domain.refund.repository.dto;

public record RefundedQuantity(
        Long orderItemId,
        Long refundedQuantity
) {}