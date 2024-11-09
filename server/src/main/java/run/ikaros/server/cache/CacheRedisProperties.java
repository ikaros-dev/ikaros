package run.ikaros.server.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Ikaros base properties.
 *
 * @author: chivehao
 */
@Data
@Validated
@ConfigurationProperties(prefix = "ikaros.cache.redis")
public class CacheRedisProperties {
    private boolean enable = false;
    private String host = "localhost";
    private int port = 6379;
    private int timeout = 10000;
}
