package run.ikaros.server.cache;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
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
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        Jackson2JsonRedisSerializer<Object> jsonRedisSerializer =
            new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        builder.key(stringRedisSerializer);
        builder.value(jsonRedisSerializer);
        builder.hashKey(stringRedisSerializer);
        builder.hashValue(jsonRedisSerializer);
        return new ReactiveRedisTemplate<>(connectionFactory, builder.build());
    }
}
