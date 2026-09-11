package org.example.murderhelp.domain.cart.dto;

import org.example.murderhelp.domain.cart.entity.CartItem;
import org.example.murderhelp.domain.product.entity.Product;
import org.example.murderhelp.domain.product.entity.ProductStatus;

public record CartItemDetailResponse(
        Long id,
        Long productId,
        String productCode,
        String name,
        String category,
        String subCategory,
        long price,
        String tier,
        String imageUrl,
        ProductStatus status,
        int stockQuantity,
        int quantity
) {

    public static CartItemDetailResponse from(CartItem cartItem) {
        Product product = cartItem.getProduct();

        return new CartItemDetailResponse(
                cartItem.getId(),
                product.getId(),
                product.getProductCode(),
                product.getName(),
                product.getCategory().getParent().getName(),
                product.getCategory().getName(),
                product.getPrice(),
                product.getTier().getValue(),
                product.getImageUrl(),
                product.getStatus(),
                product.getStockQuantity(),
                cartItem.getQuantity()
        );
    }
}
