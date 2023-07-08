package run.ikaros.server.infra.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "ikaros.task")
public class IkarosTaskProperties {
    private Integer corePoolSize;
    private Integer maximumPoolSize;
    private Long keepAliveTime;
    private Integer queueCount;
}
