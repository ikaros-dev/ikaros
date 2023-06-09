package run.ikaros.server.core.plugin;

import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
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
