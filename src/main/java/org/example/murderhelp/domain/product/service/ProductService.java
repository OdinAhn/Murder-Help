package org.example.murderhelp.domain.product.service;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.product.dto.ProductDetailResponse;
import org.example.murderhelp.domain.product.dto.ProductResponse;
import org.example.murderhelp.domain.product.dto.ProductSort;
import org.example.murderhelp.domain.product.entity.ProductStatus;
import org.example.murderhelp.domain.product.entity.ProductTier;
import org.example.murderhelp.domain.product.entity.Product;
import org.example.murderhelp.domain.product.repository.ProductRepository;
import org.example.murderhelp.global.config.cache.LocalCacheConfig;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.example.murderhelp.global.response.PageResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public ProductDetailResponse getProduct(ProductTier memberTier, Long productId) {
        Product product = productRepository
                .findProductDetail(productId, ProductStatus.DISCONTINUED)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!memberTier.canAccess(product.getTier())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "접근할 수 없는 상품 등급입니다.");
        }

        return ProductDetailResponse.from(product);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProducts(
            ProductTier memberTier,
            ProductTier requestedTier,
            String category,
            String subCategory,
            ProductSort sort,
            Pageable pageable
    ) {
        assertProductTierAccess(memberTier, requestedTier);

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

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> searchProducts(
            ProductTier memberTier,
            ProductTier requestedTier,
            String keyword,
            ProductSort sort,
            Pageable pageable
    ) {
        assertProductTierAccess(memberTier, requestedTier);

        return doSearch(requestedTier, keyword, sort, pageable);
    }

    /**
     * v2 전용 캐시 조회. {@code @Cacheable}은 캐시 히트 시 메서드 본문을 통째로
     * 건너뛰므로, 등급 접근 검증은 이 메서드 안이 아니라 호출부(v2 컨트롤러)에서
     * {@link #assertProductTierAccess}로 매 요청마다 먼저 수행해야 한다.
     * 같은 빈 안에서 이 메서드를 호출하면 프록시를 우회해 캐시가 동작하지 않으므로,
     * 반드시 다른 빈(컨트롤러)이 직접 호출해야 한다.
     */
    @Cacheable(
            cacheNames = LocalCacheConfig.PRODUCT_SEARCH_CACHE,
            key = "'keyword:' + #keyword + ':tier:' + #tier"
                    + " + ':page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize"
                    + " + ':sort:' + #sort"
    )
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> searchProductsCached(
            ProductTier tier,
            String keyword,
            ProductSort sort,
            Pageable pageable
    ) {
        return doSearch(tier, keyword, sort, pageable);
    }

    public void assertProductTierAccess(ProductTier memberTier, ProductTier requestedTier) {
        if (!memberTier.canAccess(requestedTier)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "접근할 수 없는 상품 등급입니다.");
        }
    }

    private PageResponse<ProductResponse> doSearch(
            ProductTier tier,
            String keyword,
            ProductSort sort,
            Pageable pageable
    ) {
        String normalizedKeyword = normalizeRequiredKeyword(keyword);
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort.toSort()
        );

        return PageResponse.from(
                productRepository.searchProducts(
                                normalizedKeyword,
                                tier,
                                ProductStatus.DISCONTINUED,
                                sortedPageable
                        )
                        .map(ProductResponse::from)
        );
    }

    private String normalizeRequiredKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("검색어는 필수입니다.");
        }
        return keyword.trim();
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
