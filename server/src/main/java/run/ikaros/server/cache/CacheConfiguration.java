package run.ikaros.server.cache;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import run.ikaros.api.cache.CacheType;

@Configuration
@EnableConfigurationProperties(CacheProperties.class)
public class CacheConfiguration {
    /**
     * 内存缓存配置.
     */
    @Bean
    public ReactiveCacheManager reactiveCacheManager(CacheProperties cacheProperties) {
        if (CacheType.Redis.equals(cacheProperties.getType())) {
            return new RedisReactiveCacheManager();
        }
        return new MemoryReactiveCacheManager();
    }

}
