package org.example.murderhelp.domain.payment.port;

public record PaymentGatewayResponse(
        String id,
        String status,
        Long totalAmount,
        Long cancelledAmount // 총 취소 금액
) {}