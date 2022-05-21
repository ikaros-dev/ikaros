package cn.liguohao.ikaros.config;

import java.util.Collections;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author li-guohao
 */
@EnableCaching
@Configuration
public class CacheConfig {

    public static final String APP_CACHE_NAME = "RepositoryDbCache";

    /**
     * @return 注册一个缓存管理器
     */
    @Bean
    public CacheManager cacheManager() {

        Cache cache = new ConcurrentMapCache(APP_CACHE_NAME);

        var manager = new SimpleCacheManager();
        manager.setCaches(Collections.singletonList(cache));

        return manager;
    }

}
