package run.ikaros.server.search;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for indices.
 *
 * @see IndicesConfiguration
 */
@Data
@ConfigurationProperties(prefix = "ikaros.indices")
public class IndicesProperties {
    private final Initializer initializer = new Initializer();

    @Data
    public static class Initializer {
        private boolean enabled = true;
    }
}
