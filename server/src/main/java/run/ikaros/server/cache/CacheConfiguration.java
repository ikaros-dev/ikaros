package run.ikaros.server.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CacheProperties.class)
public class CacheConfiguration {


    @Bean
    @ConditionalOnProperty(value = "ikaros.cache.type", havingValue = "memory")
    public ReactiveCacheManager memoryReactiveCacheManager() {
        return new MemoryReactiveCacheManager();
    }

    @Bean
    @ConditionalOnProperty(value = "ikaros.cache.type", havingValue = "redis")
    public ReactiveCacheManager redisReactiveCacheManager(
        ReactiveRedisTemplate<String, Object> reactiveRedisTemplate
    ) {
        return new RedisReactiveCacheManager(reactiveRedisTemplate);
    }

    /**
     * Redis reactive template.
     */
    @Bean
    @ConditionalOnProperty(value = "ikaros.cache.type", havingValue = "redis")
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
        ReactiveRedisConnectionFactory connectionFactory) {
        RedisSerializationContext.RedisSerializationContextBuilder<String, Object> builder =
            RedisSerializationContext.newSerializationContext();
        GenericJackson2JsonRedisSerializer objectSerializer =
            new GenericJackson2JsonRedisSerializer();
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        builder.key(stringRedisSerializer);
        builder.value(objectSerializer);
        builder.hashKey(stringRedisSerializer);
        builder.hashValue(objectSerializer);
        return new ReactiveRedisTemplate<>(connectionFactory, builder.build());
    }

}
