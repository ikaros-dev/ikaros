package run.ikaros.server.cache;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(value = "ikaros.enable-redis", havingValue = "false")
@EnableAutoConfiguration(exclude = {RedisAutoConfiguration.class})
public class RedisAutoConfigDisableConfiguration {
}
