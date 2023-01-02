package run.ikaros.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import run.ikaros.server.infra.properties.IkarosProperties;

/**
 * Ikaros entry class.
 *
 * @author: li-guohao
 */
@EnableConfigurationProperties({IkarosProperties.class})
@SpringBootApplication(scanBasePackages = "run.ikaros.server")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
