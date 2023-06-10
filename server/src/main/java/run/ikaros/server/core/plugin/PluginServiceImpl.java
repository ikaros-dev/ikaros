package run.ikaros.server.core.plugin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginRuntimeException;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import run.ikaros.api.exception.NotFoundException;
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
            default -> throw new PluginRuntimeException("No support operate for id(name): "
                + pluginId);
        }
        return Mono.just(pluginManager.getPlugin(pluginId).getPluginState());
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
}
