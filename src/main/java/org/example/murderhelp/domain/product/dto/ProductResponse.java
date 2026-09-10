package org.example.murderhelp.domain.product.dto;

import org.example.murderhelp.domain.product.entity.Product;
import org.example.murderhelp.domain.product.entity.ProductStatus;

public record ProductResponse(
        Long id,
        String productCode,
        String name,
        String description,
        String category,
        String subCategory,
        long price,
        String tier,
        String imageUrl,
        ProductStatus status
) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getProductCode(),
                product.getName(),
                product.getDescription(),
                product.getCategory().getParent().getName(),
                product.getCategory().getName(),
                product.getPrice(),
                product.getTier().getValue(),
                product.getImageUrl(),
                product.getStatus()
        );
    }
}
