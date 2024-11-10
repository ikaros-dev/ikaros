package run.ikaros.server.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(value = "ikaros.enable-redis", havingValue = "true")
public class RedisConfiguration {

    /**
     * Redis reactive template.
     */
    @Bean
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

    @Bean
    public ReactiveCacheManager reactiveCacheManager(
        ReactiveRedisTemplate<String, Object> reactiveRedisTemplate
    ) {
        return new RedisReactiveCacheManager(reactiveRedisTemplate);
    }
}
