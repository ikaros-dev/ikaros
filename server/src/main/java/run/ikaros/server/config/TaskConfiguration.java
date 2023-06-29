package run.ikaros.server.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

@Configuration(proxyBeanMethods = false)
public class TaskConfiguration {

    /**
     * Task thread pool.
     */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService threadPoolExecutor() {
        ThreadFactory namedThreadFactory = new CustomizableThreadFactory("task-pool-%d");

        return new ThreadPoolExecutor(
            4,
            40,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(4096),
            namedThreadFactory,
            new ThreadPoolExecutor.AbortPolicy()
        );
    }
}
