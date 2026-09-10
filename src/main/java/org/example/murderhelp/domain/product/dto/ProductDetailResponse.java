package org.example.murderhelp.domain.product.dto;

import org.example.murderhelp.domain.product.entity.Product;
import org.example.murderhelp.domain.product.entity.ProductStatus;

import java.util.List;

public record ProductDetailResponse(
        Long id,
        String productCode,
        String name,
        String description,
        String category,
        String subCategory,
        long price,
        int stockQuantity,
        String tier,
        String imageUrl,
        ProductStatus status,
        List<ProductSpecResponse> specs
) {

    public static ProductDetailResponse from(Product product) {
        return new ProductDetailResponse(
                product.getId(),
                product.getProductCode(),
                product.getName(),
                product.getDescription(),
                product.getCategory().getParent().getName(),
                product.getCategory().getName(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getTier().getValue(),
                product.getImageUrl(),
                product.getStatus(),
                product.getSpecs().stream()
                        .map(ProductSpecResponse::from)
                        .toList()
        );
    }
}
