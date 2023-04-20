package run.ikaros.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@Configuration(proxyBeanMethods = false)
public class AsyncConfiguration {
}
