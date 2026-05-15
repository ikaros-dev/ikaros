package run.ikaros.server.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import run.ikaros.server.infra.properties.IkarosTaskProperties;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IkarosTaskProperties.class)
public class TaskConfiguration {

    /**
     * Task thread pool.
     */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService threadPoolExecutor(IkarosTaskProperties ikarosTaskProperties) {
        ThreadFactory namedThreadFactory = new CustomizableThreadFactory("task-thread-");
        return new ThreadPoolExecutor(
            ikarosTaskProperties.getCorePoolSize(),
            ikarosTaskProperties.getMaximumPoolSize(),
            ikarosTaskProperties.getKeepAliveTime(),
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(ikarosTaskProperties.getQueueCount()),
            namedThreadFactory,
            new ThreadPoolExecutor.AbortPolicy()
        );
    }
}
