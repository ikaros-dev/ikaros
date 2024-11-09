package run.ikaros.server.cache;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "ikaros.cache.redis.enable", havingValue = "false")
@EnableAutoConfiguration(
    exclude = {RedisAutoConfiguration.class, RedisRepositoriesAutoConfiguration.class}
)
public class ConditionalRedisConfiguration {
}
