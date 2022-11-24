package run.ikaros.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author guohao
 * @date 2022/10/20
 */
@Configuration
public class RegisterBeanConfig {

    @Bean(destroyMethod = "shutdown")
    public ScheduledThreadPoolExecutor scheduledThreadPoolExecutor() {
        ThreadFactory threadFactory = new CustomizableThreadFactory("Ikaros-Scheduled-Task-");
        return new ScheduledThreadPoolExecutor(5, threadFactory,
            new ThreadPoolExecutor.AbortPolicy());
    }

    @Bean(destroyMethod = "shutdown")
    ThreadPoolExecutor threadPoolExecutor() {
        ThreadFactory threadFactory = new CustomizableThreadFactory("Ikaros-Task-");
        return new ThreadPoolExecutor(5, 100, 300, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(2000), threadFactory, new ThreadPoolExecutor.AbortPolicy());
    }

}
