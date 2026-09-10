package org.example.murderhelp.domain.product.controller;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.product.dto.ProductDetailResponse;
import org.example.murderhelp.domain.product.dto.ProductResponse;
import org.example.murderhelp.domain.product.dto.ProductSort;
import org.example.murderhelp.domain.product.entity.ProductTier;
import org.example.murderhelp.domain.product.service.ProductService;
import org.example.murderhelp.domain.product.service.ProductTierAuthorityResolver;
import org.example.murderhelp.global.response.ApiResponse;
import org.example.murderhelp.global.response.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductTierAuthorityResolver productTierAuthorityResolver;

    @GetMapping("/api/products/{productId}")
    public ApiResponse<ProductDetailResponse> getProduct(
            Authentication authentication,
            @PathVariable Long productId
    ) {
        ProductTier memberTier = productTierAuthorityResolver.resolve(authentication);
        return ApiResponse.ok(productService.getProduct(memberTier, productId));
    }

    @GetMapping("/api/products")
    public ApiResponse<PageResponse<ProductResponse>> getProducts(
            Authentication authentication,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String subCategory,
            @RequestParam(required = false) String tier,
            @RequestParam(required = false) String sort,
            Pageable pageable
    ) {
        ProductTier memberTier = productTierAuthorityResolver.resolve(authentication);
        ProductTier requestedTier = ProductTier.fromValue(tier);
        ProductSort productSort = ProductSort.fromValue(sort);

        return ApiResponse.ok(
                productService.getProducts(
                        memberTier,
                        requestedTier,
                        category,
                        subCategory,
                        productSort,
                        pageable
                )
        );
    }

    @GetMapping("/api/v1/products/search")
    public ApiResponse<PageResponse<ProductResponse>> searchProducts(
            Authentication authentication,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tier,
            @RequestParam(required = false) String sort,
            Pageable pageable
    ) {
        ProductTier memberTier = productTierAuthorityResolver.resolve(authentication);
        ProductTier requestedTier = ProductTier.fromValue(tier);
        ProductSort productSort = ProductSort.fromValue(sort);

        return ApiResponse.ok(
                productService.searchProducts(
                        memberTier,
                        requestedTier,
                        keyword,
                        productSort,
                        pageable
                )
        );
    }

    /**
     * v1과 동일한 요청/응답 계약에 Caffeine 캐시(productSearch)를 적용한 검색 API.
     * 등급 접근 검증은 캐시 히트 여부와 무관하게 매 요청마다 먼저 수행한 뒤,
     * 실제 조회는 {@link ProductService#searchProductsCached}(다른 빈에서 호출해야
     * {@code @Cacheable} 프록시가 걸린다)에 위임한다.
     */
    @GetMapping("/api/v2/products/search")
    public ApiResponse<PageResponse<ProductResponse>> searchProductsV2(
            Authentication authentication,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tier,
            @RequestParam(required = false) String sort,
            Pageable pageable
    ) {
        ProductTier memberTier = productTierAuthorityResolver.resolve(authentication);
        ProductTier requestedTier = ProductTier.fromValue(tier);
        ProductSort productSort = ProductSort.fromValue(sort);

        productService.assertProductTierAccess(memberTier, requestedTier);

        return ApiResponse.ok(
                productService.searchProductsCached(requestedTier, keyword, productSort, pageable)
        );
    }
}
