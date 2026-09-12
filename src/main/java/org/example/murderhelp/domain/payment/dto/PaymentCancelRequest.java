package org.example.murderhelp.domain.payment.dto;

import jakarta.validation.constraints.Size;

public record PaymentCancelRequest(
        @Size(max = 500, message = "취소 사유는 500자 이내여야 합니다")
        String reason
) {}