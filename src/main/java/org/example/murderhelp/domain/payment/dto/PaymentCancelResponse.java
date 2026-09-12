package org.example.murderhelp.domain.payment.dto;


import org.example.murderhelp.domain.payment.entity.Payment;

public record PaymentCancelResponse(
        Long paymentId,
        Long orderId,
        String portonePaymentId,
        String paymentStatus,
        String orderStatus,
        String message
) {
    public static PaymentCancelResponse from(Payment payment, String message) {
        return new PaymentCancelResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getPortonePaymentId(),
                payment.getStatus().name(),
                payment.getOrder().getStatus().name(),
                message
        );
    }
}