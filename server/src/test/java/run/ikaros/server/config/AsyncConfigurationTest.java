package run.ikaros.server.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class AsyncConfigurationTest {

    private AsyncConfiguration asyncConfiguration;

    @BeforeEach
    void setUp() {
        asyncConfiguration = new AsyncConfiguration();
    }

    @Test
    void taskExecutor_returnsThreadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = asyncConfiguration.taskExecutor();

        assertThat(executor).isNotNull();
        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
    }

    @Test
    void taskExecutor_hasCorrectCorePoolSize() {
        ThreadPoolTaskExecutor executor = asyncConfiguration.taskExecutor();

        assertThat(executor.getCorePoolSize()).isEqualTo(5);
    }

    @Test
    void taskExecutor_hasCorrectMaxPoolSize() {
        ThreadPoolTaskExecutor executor = asyncConfiguration.taskExecutor();

        assertThat(executor.getMaxPoolSize()).isEqualTo(20);
    }

    @Test
    void taskExecutor_hasCorrectQueueCapacity() {
        ThreadPoolTaskExecutor executor = asyncConfiguration.taskExecutor();
        executor.initialize();

        assertThat(executor.getThreadPoolExecutor().getQueue().size()).isEqualTo(0);
        // Verify the max queue capacity through the thread pool executor
        ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();
        assertThat(threadPoolExecutor.getQueue().remainingCapacity()).isEqualTo(200000);
    }

    @Test
    void taskExecutor_hasCorrectKeepAliveSeconds() {
        ThreadPoolTaskExecutor executor = asyncConfiguration.taskExecutor();

        assertThat(executor.getKeepAliveSeconds()).isEqualTo(200);
    }

    @Test
    void taskExecutor_hasCorrectThreadNamePrefix() {
        ThreadPoolTaskExecutor executor = asyncConfiguration.taskExecutor();

        assertThat(executor.getThreadNamePrefix()).isEqualTo("ikaros-async-thread-pool-");
    }

    @Test
    void taskExecutor_hasCallerRunsPolicy() {
        ThreadPoolTaskExecutor executor = asyncConfiguration.taskExecutor();
        executor.initialize();

        ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();
        assertThat(threadPoolExecutor.getRejectedExecutionHandler())
            .isInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class);
    }

}
