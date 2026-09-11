package org.example.murderhelp.global.config.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;

class RedisConfigTest {

    @Test
    void shouldSerializeAndDeserializeJavaTimeValue() {
        GenericJacksonJsonRedisSerializer serializer =
                new RedisConfig().redisValueSerializer();
        CachedValue original = new CachedValue(
                "product",
                LocalDateTime.of(2026, 9, 11, 12, 30)
        );

        byte[] serialized = serializer.serialize(original);
        Object deserialized = serializer.deserialize(serialized);

        assertThat(deserialized).isEqualTo(original);
    }

    record CachedValue(String name, LocalDateTime cachedAt) {
    }
}
