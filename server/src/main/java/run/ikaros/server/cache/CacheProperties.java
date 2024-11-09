package run.ikaros.server.cache;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import run.ikaros.api.cache.CacheType;

@Data
@Validated
@ConfigurationProperties(prefix = "ikaros.cache")
public class CacheProperties {
    private CacheType type = CacheType.Memory;
}
