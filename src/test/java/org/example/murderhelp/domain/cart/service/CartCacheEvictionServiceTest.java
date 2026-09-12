package org.example.murderhelp.domain.cart.service;

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
class CartCacheEvictionServiceTest {

    @Autowired
    private CartCacheEvictionService cartCacheEvictionService;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void 장바구니가_변경되면_해당_회원의_장바구니_캐시만_삭제한다() {
        Cache cartItemsCache = cacheManager.getCache(CacheNames.CART_ITEMS);
        cartItemsCache.put("member:1", "first-member");
        cartItemsCache.put("member:2", "second-member");

        cartCacheEvictionService.evictCartItems(1L);

        assertThat(cartItemsCache.get("member:1")).isNull();
        assertThat(cartItemsCache.get("member:2").get()).isEqualTo("second-member");
    }
}
