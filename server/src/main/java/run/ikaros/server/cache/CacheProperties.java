package run.ikaros.server.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "ikaros.cache")
public class CacheProperties {
    private CacheType type;
    private Redis redis = new Redis();

    @Data
    public static class Redis {
        private String host = "localhost";
        private int port = 6379;
        private String password = "";
        private int timeout = 10000;
        private Long expirationTime = (long) (3 * 24 * 60 * 60 * 1000); // 3day
    }

    private Boolean enabled = false;

}
