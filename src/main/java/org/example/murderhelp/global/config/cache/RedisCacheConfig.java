package org.example.murderhelp.global.config.cache;

import java.time.Duration;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
public class RedisCacheConfig {

    private static final Duration PRODUCT_SEARCH_TTL = Duration.ofMinutes(10);
    private static final Duration PRODUCT_DETAIL_TTL = Duration.ofMinutes(5);
    private static final Duration PRODUCT_LIST_TTL = Duration.ofMinutes(3);
    private static final Duration CART_ITEMS_TTL = Duration.ofMinutes(1);

    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            GenericJacksonJsonRedisSerializer redisValueSerializer
    ) {
        RedisCacheConfiguration defaultConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(redisValueSerializer)
                );
        RedisCacheConfiguration productSearchConfiguration = defaultConfiguration.entryTtl(PRODUCT_SEARCH_TTL);
        RedisCacheConfiguration productDetailConfiguration = defaultConfiguration.entryTtl(PRODUCT_DETAIL_TTL);
        RedisCacheConfiguration productListConfiguration = defaultConfiguration.entryTtl(PRODUCT_LIST_TTL);
        RedisCacheConfiguration cartItemsConfiguration = defaultConfiguration.entryTtl(CART_ITEMS_TTL);

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(productSearchConfiguration)
                .withInitialCacheConfigurations(
                        Map.of(
                                CacheNames.PRODUCT_SEARCH, productSearchConfiguration,
                                CacheNames.PRODUCT_DETAIL, productDetailConfiguration,
                                CacheNames.PRODUCT_LIST, productListConfiguration,
                                CacheNames.CART_ITEMS, cartItemsConfiguration
                        )
                )
                .build();
    }
}
