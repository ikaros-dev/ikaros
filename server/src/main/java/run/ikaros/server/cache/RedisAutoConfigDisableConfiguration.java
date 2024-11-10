package run.ikaros.server.cache;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import run.ikaros.server.cache.condition.CacheRedisDisableCondition;

@Configuration(proxyBeanMethods = false)
@Conditional(CacheRedisDisableCondition.class)
@EnableAutoConfiguration(exclude = {RedisAutoConfiguration.class})
public class RedisAutoConfigDisableConfiguration {
}
