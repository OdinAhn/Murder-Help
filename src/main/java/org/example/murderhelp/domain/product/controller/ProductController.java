package org.example.murderhelp.domain.product.controller;

import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductTierAuthorityResolver productTierAuthorityResolver;

    @GetMapping
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
}
