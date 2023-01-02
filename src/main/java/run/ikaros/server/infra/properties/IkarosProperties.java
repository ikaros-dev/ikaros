package run.ikaros.server.infra.properties;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Ikaros base properties.
 *
 * @author: li-guohao
 */
@Validated
@ConfigurationProperties(prefix = "ikaros")
public class IkarosProperties {
    @NotNull
    private Path workDir;
    @NotNull
    private URI externalUrl;

    public Path getWorkDir() {
        return workDir;
    }

    public IkarosProperties setWorkDir(Path workDir) {
        this.workDir = workDir;
        return this;
    }

    public URI getExternalUrl() {
        return externalUrl;
    }

    public IkarosProperties setExternalUrl(URI externalUrl) {
        this.externalUrl = externalUrl;
        return this;
    }
}
