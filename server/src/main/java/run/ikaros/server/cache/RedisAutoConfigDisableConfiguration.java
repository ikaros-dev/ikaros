package run.ikaros.server.cache;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.health.DataRedisReactiveHealthContributorAutoConfiguration;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import run.ikaros.server.cache.condition.CacheRedisDisableCondition;

@Configuration(proxyBeanMethods = false)
@Conditional(CacheRedisDisableCondition.class)
@EnableAutoConfiguration(exclude = {DataRedisReactiveAutoConfiguration.class,
    DataRedisReactiveHealthContributorAutoConfiguration.class})
public class RedisAutoConfigDisableConfiguration {
}
