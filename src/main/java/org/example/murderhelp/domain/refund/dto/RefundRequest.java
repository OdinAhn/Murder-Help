package org.example.murderhelp.domain.refund.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RefundRequest(
        @NotNull(message = "결제 ID는 필수입니다.") Long paymentId,
        @NotBlank(message = "환불 사유는 필수입니다.") String cancelReason,
        @NotEmpty(message = "환불할 상품을 하나 이상 선택해야 합니다.")
        @Valid List<RefundItemRequest> items
) {
    // 상품별 요청 데이터를 담기 위한 내부 레코드
    public record RefundItemRequest(
            @NotNull(message = "주문 상품 ID는 필수입니다.") Long orderItemId,
            @Min(value = 1, message = "환불 수량은 1개 이상이어야 합니다.") int requestQuantity
    ) {}
}
