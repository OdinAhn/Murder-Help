package org.example.murderhelp.domain.product.service;

import org.example.murderhelp.domain.product.dto.ProductSort;
import org.example.murderhelp.domain.product.entity.ProductTier;
import org.example.murderhelp.domain.product.repository.ProductRepository;
import org.example.murderhelp.global.config.cache.CacheNames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles({"test", "local"})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:product-search-caching-test;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
class ProductSearchCachingTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoSpyBean
    private ProductRepository productRepository;

    @BeforeEach
    void clearCache() {
        Cache cache = cacheManager.getCache(CacheNames.PRODUCT_SEARCH);
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void 같은_조건으로_v2_캐시_메서드를_반복_호출하면_저장소는_한_번만_조회한다() {
        for (int i = 0; i < 3; i++) {
            productService.searchProductsCached(
                    ProductTier.PURPLE, "pistol", ProductSort.POPULAR, PageRequest.of(0, 10)
            );
        }

        verify(productRepository, times(1))
                .searchProducts(any(), any(), any(), any());
    }

    @Test
    void 검색어_등급_페이지_정렬_중_하나라도_다르면_서로_다른_캐시로_취급한다() {
        productService.searchProductsCached(ProductTier.PURPLE, "pistol", ProductSort.POPULAR, PageRequest.of(0, 10));
        productService.searchProductsCached(ProductTier.PURPLE, "rifle", ProductSort.POPULAR, PageRequest.of(0, 10));
        productService.searchProductsCached(ProductTier.RED, "pistol", ProductSort.POPULAR, PageRequest.of(0, 10));
        productService.searchProductsCached(ProductTier.PURPLE, "pistol", ProductSort.POPULAR, PageRequest.of(1, 10));
        productService.searchProductsCached(ProductTier.PURPLE, "pistol", ProductSort.PRICE_ASC, PageRequest.of(0, 10));

        verify(productRepository, times(5))
                .searchProducts(any(), any(), any(), any());
    }

    @Test
    void v1_검색_메서드는_캐시가_적용되지_않아_매번_저장소를_조회한다() {
        for (int i = 0; i < 3; i++) {
            productService.searchProducts(
                    ProductTier.PURPLE, ProductTier.PURPLE, "pistol", ProductSort.POPULAR, PageRequest.of(0, 10)
            );
        }

        verify(productRepository, times(3))
                .searchProducts(any(), any(), any(), any());
    }
}
