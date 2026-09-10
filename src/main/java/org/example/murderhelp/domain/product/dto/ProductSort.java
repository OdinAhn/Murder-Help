package org.example.murderhelp.domain.product.dto;

import org.springframework.data.domain.Sort;

import java.util.Locale;

public enum ProductSort {
    POPULAR,
    PRICE_ASC,
    PRICE_DESC,
    NEWEST;

    public static ProductSort fromValue(String value) {
        if (value == null || value.isBlank()) {
            return POPULAR;
        }

        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("지원하지 않는 상품 정렬 방식입니다: " + value);
        }
    }

    public Sort toSort() {
        return switch (this) {
            // 판매량 데이터가 추가되기 전까지 기존 카탈로그의 상품 코드 순서를 유지한다.
            case POPULAR -> Sort.by(Sort.Order.asc("productCode"));
            case PRICE_ASC -> Sort.by(
                    Sort.Order.asc("price"),
                    Sort.Order.asc("productCode")
            );
            case PRICE_DESC -> Sort.by(
                    Sort.Order.desc("price"),
                    Sort.Order.asc("productCode")
            );
            // 별도 생성일 컬럼이 없으므로 증가하는 PK를 등록 순서로 사용한다.
            case NEWEST -> Sort.by(Sort.Order.desc("id"));
        };
    }
}
