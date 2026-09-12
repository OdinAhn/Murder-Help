package org.example.murderhelp.infra.portone.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOnePaymentResponse(
        String id,                            // 결제 건 ID (우리가 생성한 paymentId)
        String status,                        // 결제 상태: READY, PAID, FAILED, CANCELLED, PARTIAL_CANCELLED
        PaymentAmount amount                  // 결제 금액 세부 정보
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaymentAmount(
            int total,     // 총 결제 금액
            int cancelled  // 총 취소(환불) 금액
    ) {}
}