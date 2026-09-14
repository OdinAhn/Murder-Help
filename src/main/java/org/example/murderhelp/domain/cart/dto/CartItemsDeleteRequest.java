package org.example.murderhelp.domain.cart.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.UniqueElements;

import java.util.List;

public record CartItemsDeleteRequest(
        @NotEmpty(message = "삭제할 장바구니 상품을 하나 이상 선택해야 합니다.")
        @UniqueElements(message = "장바구니 상품 ID는 중복될 수 없습니다.")
        List<@NotNull(message = "장바구니 상품 ID는 필수입니다.")
                @Positive(message = "장바구니 상품 ID는 1 이상이어야 합니다.") Long> cartItemIds
) {
}
