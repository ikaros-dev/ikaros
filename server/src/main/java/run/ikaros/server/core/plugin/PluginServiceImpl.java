package run.ikaros.server.core.plugin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.File;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginRuntimeException;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import run.ikaros.api.exception.NotFoundException;
import run.ikaros.api.exception.PluginInstallRuntimeException;
import run.ikaros.server.plugin.IkarosPluginManager;

@Slf4j
@Service
public class PluginServiceImpl implements PluginService {
    private final IkarosPluginManager pluginManager;

    public PluginServiceImpl(IkarosPluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    @Override
    public Mono<PluginState> operateState(@NotBlank String pluginId,
                                          @NotNull PluginStateOperate operate) {
        Assert.hasText(pluginId, "'pluginId' must has text.");
        Assert.notNull(operate, "'operate' must not null.");
        switch (operate) {
            case LOAD -> pluginManager.loadPlugin(pluginId);
            case LOAD_ALL -> pluginManager.loadPlugins();
            case RELOAD -> pluginManager.reloadPlugin(pluginId);
            case RELOAD_ALL -> pluginManager.reloadPlugins();
            case RELOAD_ALL_STARTED -> pluginManager.reloadStartedPlugins();
            case UNLOAD -> pluginManager.unloadPlugin(pluginId);
            case ENABLE -> pluginManager.enablePlugin(pluginId);
            case DISABLE -> pluginManager.disablePlugin(pluginId);
            case START -> pluginManager.startPlugin(pluginId);
            case STOP -> pluginManager.stopPlugin(pluginId);
            case DELETE -> pluginManager.deletePlugin(pluginId);
            default -> throw new PluginRuntimeException("No support operate for id(name): "
                + pluginId);
        }
        if (pluginManager.getPlugins().isEmpty() || pluginId == null
            || "ALL".equalsIgnoreCase(pluginId)) {
            log.warn("Skip get plugin state operate. pluginId: [{}], manager plugins: [{}]",
                pluginId, pluginManager.getPlugins());
            return Mono.empty();
        }
        return Mono.just(pluginManager.getPlugin(pluginId))
            .switchIfEmpty(Mono.error(
                new PluginRuntimeException("Not found plugin in manager for id: " + pluginId)))
            .map(PluginWrapper::getPluginState)
            .onErrorResume(NullPointerException.class, e -> Mono.empty());
    }

    @Override
    public Mono<PluginState> start(@NotBlank String pluginId) {
        Assert.hasText(pluginId, "'pluginId' must has text");
        PluginWrapper pluginWrapper = pluginManager.getPlugin(pluginId);
        if (pluginWrapper == null) {
            return Mono.error(new NotFoundException("Not found plugin for id: " + pluginId));
        }
        PluginState pluginState = pluginManager.startPlugin(pluginId);
        return Mono.just(pluginState);
    }

    @Override
    public Mono<PluginState> stop(@NotBlank String pluginId) {
        Assert.hasText(pluginId, "'pluginId' must has text");
        PluginWrapper pluginWrapper = pluginManager.getPlugin(pluginId);
        if (pluginWrapper == null) {
            return Mono.error(new NotFoundException("Not found plugin for id: " + pluginId));
        }
        PluginState pluginState = pluginManager.stopPlugin(pluginId);
        return Mono.just(pluginState);
    }

    @Override
    public Mono<PluginState> reload(String pluginId) {
        Assert.hasText(pluginId, "'pluginId' must has text");
        PluginWrapper pluginWrapper = pluginManager.getPlugin(pluginId);
        if (pluginWrapper == null) {
            return Mono.error(new NotFoundException("Not found plugin for id: " + pluginId));
        }
        PluginState pluginState = pluginManager.reloadPlugin(pluginId);
        return Mono.just(pluginState);
    }

    @Override
    public Mono<Void> install(@NotNull FilePart filePart) {
        Assert.notNull(filePart, "'filePart' must not null.");
        String pluginDir = System.getProperty("pf4j.pluginsDir");
        try {
            File pluginDirFile = new File(pluginDir);
            if (!pluginDirFile.exists()) {
                pluginDirFile.mkdirs();
            }
            Path destPath = Path.of(pluginDirFile.toURI()).resolve(filePart.filename());

            return filePart.transferTo(destPath.toFile())
                .doOnSuccess(unused -> log.debug("Upload plugin file [{}] to plugin dir [{}].",
                    filePart.filename(), destPath))
                .then(Mono.just(pluginManager.loadPlugin(destPath)))
                .doOnSuccess(pluginId ->
                    log.debug("Load plugin by path success, pluginId: [{}].", pluginId))
                .then();
        } catch (Exception e) {
            throw new PluginInstallRuntimeException(e);
        }
    }
}
