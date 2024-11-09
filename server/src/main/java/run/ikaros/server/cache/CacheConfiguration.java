package run.ikaros.server.cache;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfiguration {
    /**
     * 内存缓存配置.
     */
    @Bean
    public CacheManager simpleCacheManager() {
        return new ConcurrentMapCacheManager();
    }

}
