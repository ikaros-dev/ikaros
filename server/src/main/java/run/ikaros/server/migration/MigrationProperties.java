package run.ikaros.server.migration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "ikaros.migration")
public class MigrationProperties {
    private boolean enable = false;
    private final Datasource datasource = new Datasource();

    @Data
    public static class Datasource {
        private String url;
        private String username;
        private String password;
    }
}
