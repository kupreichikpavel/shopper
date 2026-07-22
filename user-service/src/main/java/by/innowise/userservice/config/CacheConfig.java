package by.innowise.userservice.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@EnableCaching
@Configuration(proxyBeanMethods = false)
public class CacheConfig {

    private static final Duration CACHE_TTL =
            Duration.ofMinutes(10);

    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {
        GenericJacksonJsonRedisSerializer serializer =
                GenericJacksonJsonRedisSerializer
                        .builder()
                        .build();

        RedisSerializationContext.SerializationPair<Object>
                valueSerializationPair =
                RedisSerializationContext
                        .fromSerializer(serializer)
                        .getValueSerializationPair();

        return RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(CACHE_TTL)
                .disableCachingNullValues()
                .serializeValuesWith(valueSerializationPair);
    }
}