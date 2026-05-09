package run.ikaros.server.core.migration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "ikaros.migration")
public class MigrationProperties {
    private Boolean enable = false;
    private R2dbc r2dbc;

    @Data
    public static class R2dbc {
        private String url = "";
        private String username = "";
        private String password = "";
    }
}
