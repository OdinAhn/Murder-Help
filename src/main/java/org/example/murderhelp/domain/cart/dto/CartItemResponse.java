package org.example.murderhelp.domain.cart.dto;

import org.example.murderhelp.domain.cart.entity.CartItem;

public record CartItemResponse(
        Long id,
        Long productId,
        int quantity
) {

    public static CartItemResponse from(CartItem cartItem) {
        return new CartItemResponse(
                cartItem.getId(),
                cartItem.getProduct().getId(),
                cartItem.getQuantity()
        );
    }
}
