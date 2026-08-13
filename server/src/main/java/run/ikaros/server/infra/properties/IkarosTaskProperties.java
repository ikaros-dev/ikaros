package run.ikaros.server.infra.properties;

import lombok.Data;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "ikaros.task")
public class IkarosTaskProperties {
    private @Nullable Integer corePoolSize;
    private @Nullable Integer maximumPoolSize;
    private @Nullable Long keepAliveTime;
    private @Nullable Integer queueCount;
}
