package org.example.murderhelp.global.config.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import org.example.murderhelp.global.config.redis.RedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;

class RedisCacheConfigTest {

    @Test
    void shouldConfigureRedisCachesForProductSearchAndDetail() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        GenericJacksonJsonRedisSerializer valueSerializer =
                new RedisConfig().redisValueSerializer();

        RedisCacheManager cacheManager = (RedisCacheManager) new RedisCacheConfig()
                .cacheManager(connectionFactory, valueSerializer);
        cacheManager.afterPropertiesSet();
        RedisCacheConfiguration cacheConfiguration = cacheManager.getCacheConfigurations()
                .get(CacheNames.PRODUCT_SEARCH);

        assertThat(cacheConfiguration).isNotNull();
        assertThat(cacheConfiguration.getTtlFunction().getTimeToLive(null, null))
                .isEqualTo(Duration.ofMinutes(10));

        RedisCacheConfiguration productDetailConfiguration = cacheManager.getCacheConfigurations()
                .get(CacheNames.PRODUCT_DETAIL);
        assertThat(productDetailConfiguration).isNotNull();
        assertThat(productDetailConfiguration.getTtlFunction().getTimeToLive(null, null))
                .isEqualTo(Duration.ofMinutes(5));

        RedisCacheConfiguration productListConfiguration = cacheManager.getCacheConfigurations()
                .get(CacheNames.PRODUCT_LIST);
        assertThat(productListConfiguration).isNotNull();
        assertThat(productListConfiguration.getTtlFunction().getTimeToLive(null, null))
                .isEqualTo(Duration.ofMinutes(3));
    }
}
