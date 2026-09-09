package org.example.murderhelp.global.config.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class LocalCacheConfigTest {

    @Autowired
    private CacheManager cacheManager;

    @Test
    void shouldConfigureCaffeineCacheForProductSearch() {
        assertThat(cacheManager).isInstanceOf(CaffeineCacheManager.class);
        assertThat(cacheManager.getCache(LocalCacheConfig.PRODUCT_SEARCH_CACHE)).isNotNull();
    }
}
