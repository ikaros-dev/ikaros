package run.ikaros.server.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import run.ikaros.api.infra.properties.IkarosProperties;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IkarosProperties.class)
public class AppConfiguration {
}
