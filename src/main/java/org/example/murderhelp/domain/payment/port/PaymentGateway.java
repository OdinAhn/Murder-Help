package org.example.murderhelp.domain.payment.port;

public interface PaymentGateway {

    // PG사에서 실제 결제 정보 조회 (금액 검증용)
    PaymentGatewayResponse getPayment(String paymentId);

    // 결제 취소 (amount가 null이면 전액 취소, 값이 있으면 부분 취소)
    void cancelPayment(String paymentId, String reason, Integer amount);
}