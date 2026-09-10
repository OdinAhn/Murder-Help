package org.example.murderhelp.domain.product.dto;

import org.example.murderhelp.domain.product.entity.ProductSpec;

public record ProductSpecResponse(
        String name,
        String value,
        int sortOrder
) {

    public static ProductSpecResponse from(ProductSpec productSpec) {
        return new ProductSpecResponse(
                productSpec.getSpecName(),
                productSpec.getSpecValue(),
                productSpec.getSortOrder()
        );
    }
}
