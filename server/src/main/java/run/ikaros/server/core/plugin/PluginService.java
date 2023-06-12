package run.ikaros.server.core.plugin;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.pf4j.PluginState;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

public interface PluginService {

    Mono<PluginState> operateState(@NotBlank String pluginId, @NotNull PluginStateOperate operate);

    Mono<PluginState> start(@NotBlank String pluginId);

    Mono<PluginState> stop(@NotBlank String pluginId);

    Mono<PluginState> reload(@NotBlank String pluginId);

    Mono<Void> install(@Nonnull FilePart filePart);

    // Mono<Boolean> upgrade(String pluginId);
}
