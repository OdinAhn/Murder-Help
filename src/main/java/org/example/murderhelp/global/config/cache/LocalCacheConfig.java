package org.example.murderhelp.global.config.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "caffeine")
public class LocalCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.registerCustomCache(CacheNames.PRODUCT_SEARCH, Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(10))
                .maximumSize(500)
                .build());
        cacheManager.registerCustomCache(CacheNames.PRODUCT_DETAIL, Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .maximumSize(500)
                .build());
        cacheManager.registerCustomCache(CacheNames.PRODUCT_LIST, Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(3))
                .maximumSize(500)
                .build());
        cacheManager.registerCustomCache(CacheNames.CART_ITEMS, Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(1))
                .maximumSize(1_000)
                .build());
        return cacheManager;
    }
}
