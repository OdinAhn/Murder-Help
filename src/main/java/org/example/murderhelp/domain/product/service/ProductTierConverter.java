package org.example.murderhelp.domain.product.service;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.example.murderhelp.domain.product.entity.ProductTier;

// 준비된 상품 데이터의 tier가 소문자로 준비되어 있습니다.
@Converter
public class ProductTierConverter implements AttributeConverter<ProductTier, String> {

    @Override
    public String convertToDatabaseColumn(ProductTier attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public ProductTier convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ProductTier.fromValue(dbData);
    }
}
