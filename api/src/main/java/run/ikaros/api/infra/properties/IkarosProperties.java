package run.ikaros.api.infra.properties;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.nio.file.Path;
import lombok.Data;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Ikaros base properties.
 *
 * @author: chivehao
 */
@Data
@Validated
@ConfigurationProperties(prefix = "ikaros")
public class IkarosProperties {
    @NotNull
    private @Nullable Path workDir;
    @NotNull
    private @Nullable URI externalUrl;
    private @Nullable Boolean showTheme;
}
