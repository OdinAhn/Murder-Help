package org.example.murderhelp.domain.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartItemAddRequest(
        @NotNull(message = "상품 ID는 필수입니다.")
        @Positive(message = "상품 ID는 1 이상이어야 합니다.")
        Long productId,

        @NotNull(message = "장바구니 수량은 필수입니다.")
        @Min(value = 1, message = "장바구니 수량은 1 이상이어야 합니다.")
        Integer quantity
) {
}
