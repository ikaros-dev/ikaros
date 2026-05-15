package run.ikaros.server.console;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "ikaros.console")
public class ConsoleProperties {
    private String location = "classpath:/console/";
}
