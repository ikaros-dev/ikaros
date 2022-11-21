package run.ikaros.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static run.ikaros.server.constants.HttpConst.HTTP_PROXY_HOST;
import static run.ikaros.server.constants.HttpConst.HTTP_PROXY_PORT;

/**
 * @author guohao
 * @date 2022/10/20
 */
@Configuration
public class RegisterBeanConfig {

    @Bean
    public RestTemplate restTemplate() {
        Proxy proxy =
            new Proxy(Proxy.Type.HTTP, new InetSocketAddress(HTTP_PROXY_HOST, HTTP_PROXY_PORT));
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setProxy(proxy);
        return new RestTemplate(requestFactory);
    }

    @Bean
    public ScheduledThreadPoolExecutor scheduledThreadPoolExecutor() {
        ThreadFactory threadFactory = new CustomizableThreadFactory("Ikaros-Scheduled-Task-");
        return new ScheduledThreadPoolExecutor(5, threadFactory,
            new ThreadPoolExecutor.AbortPolicy());
    }

    @Bean
    ThreadPoolExecutor threadPoolExecutor() {
        ThreadFactory threadFactory = new CustomizableThreadFactory("Ikaros-Task-");
        return new ThreadPoolExecutor(5, 100, 300, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(2000), threadFactory, new ThreadPoolExecutor.AbortPolicy());
    }

}
