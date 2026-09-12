package org.example.murderhelp.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.example.murderhelp.global.config.cache.CacheNames;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ProductCacheEvictionServiceTest {

    @Autowired
    private ProductCacheEvictionService productCacheEvictionService;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void 상품_데이터가_변경되면_상세_목록_검색_캐시를_모두_삭제한다() {
        Cache productDetailCache = cacheManager.getCache(CacheNames.PRODUCT_DETAIL);
        Cache productListCache = cacheManager.getCache(CacheNames.PRODUCT_LIST);
        Cache productSearchCache = cacheManager.getCache(CacheNames.PRODUCT_SEARCH);

        productDetailCache.put("detail", "value");
        productListCache.put("list", "value");
        productSearchCache.put("search", "value");

        productCacheEvictionService.evictProductCaches();

        assertThat(productDetailCache.get("detail")).isNull();
        assertThat(productListCache.get("list")).isNull();
        assertThat(productSearchCache.get("search")).isNull();
    }
}
