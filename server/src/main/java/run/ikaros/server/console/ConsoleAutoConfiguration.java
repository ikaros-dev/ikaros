package run.ikaros.server.console;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ConsoleProperties.class)
public class ConsoleAutoConfiguration {
}
