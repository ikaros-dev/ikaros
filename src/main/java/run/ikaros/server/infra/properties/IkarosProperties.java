package run.ikaros.server.infra.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.nio.file.Path;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Ikaros base properties.
 *
 * @author: li-guohao
 */
@Data
@Validated
@ConfigurationProperties(prefix = "ikaros")
public class IkarosProperties {
    @NotNull
    private Path workDir;
    @NotNull
    private URI externalUrl;
    @Valid
    private final SecurityProperties security = new SecurityProperties();
    @Valid
    private final ConsoleProperties console = new ConsoleProperties();
}
