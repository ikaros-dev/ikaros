package run.ikaros.server.cache;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import run.ikaros.server.cache.condition.CacheMemoryEnableCondition;
import run.ikaros.server.cache.condition.CacheRedisEnableCondition;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CacheProperties.class)
public class CacheConfiguration {


    @Bean
    @Conditional(CacheMemoryEnableCondition.class)
    public ReactiveCacheManager memoryReactiveCacheManager() {
        return new MemoryReactiveCacheManager();
    }

    @Bean
    @Conditional(CacheRedisEnableCondition.class)
    public ReactiveCacheManager redisReactiveCacheManager(
        ReactiveRedisTemplate<String, Object> reactiveRedisTemplate
    ) {
        return new RedisReactiveCacheManager(reactiveRedisTemplate);
    }

    /**
     * Redis reactive template.
     */
    @Bean
    @Conditional(CacheRedisEnableCondition.class)
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
