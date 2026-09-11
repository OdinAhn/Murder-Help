package org.example.murderhelp.domain.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemUpdateRequest(
        @NotNull(message = "장바구니 수량은 필수입니다.")
        @Min(value = 1, message = "장바구니 수량은 1 이상이어야 합니다.")
        Integer quantity
) {
}
