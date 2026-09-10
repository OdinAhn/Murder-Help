package org.example.murderhelp.domain.product.service;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.product.dto.ProductResponse;
import org.example.murderhelp.domain.product.dto.ProductSort;
import org.example.murderhelp.domain.product.entity.ProductStatus;
import org.example.murderhelp.domain.product.entity.ProductTier;
import org.example.murderhelp.domain.product.repository.ProductRepository;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.example.murderhelp.global.response.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProducts(
            ProductTier memberTier,
            ProductTier requestedTier,
            String category,
            String subCategory,
            ProductSort sort,
            Pageable pageable
    ) {
        if (!memberTier.canAccess(requestedTier)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "접근할 수 없는 상품 등급입니다.");
        }

        String normalizedCategory = normalizeRequiredCategory(category);
        String normalizedSubCategory = normalizeSubCategory(subCategory);
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort.toSort()
        );

        return PageResponse.from(
                productRepository.findProducts(
                                normalizedCategory,
                                normalizedSubCategory,
                                requestedTier,
                                ProductStatus.DISCONTINUED,
                                sortedPageable
                        )
                        .map(ProductResponse::from)
        );
    }

    private String normalizeRequiredCategory(String category) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("카테고리는 필수입니다.");
        }
        return category.trim();
    }

    private String normalizeSubCategory(String subCategory) {
        if (subCategory == null || subCategory.isBlank() || "전체".equals(subCategory.trim())) {
            return null;
        }
        return subCategory.trim();
    }
}
