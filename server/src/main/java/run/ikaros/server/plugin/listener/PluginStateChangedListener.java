package run.ikaros.server.plugin.listener;

import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.plugin.custom.Plugin;
import run.ikaros.server.infra.utils.JsonUtils;
import run.ikaros.server.plugin.event.IkarosPluginStateChangedEvent;

@Slf4j
@Component
public class PluginStateChangedListener {
    private final ReactiveCustomClient reactiveCustomClient;

    public PluginStateChangedListener(ReactiveCustomClient reactiveCustomClient) {
        this.reactiveCustomClient = reactiveCustomClient;
    }

    /**
     * Ikaros plugin state changed event listen handler.
     */
    @EventListener(IkarosPluginStateChangedEvent.class)
    public Mono<Void> onApplicationEvent(IkarosPluginStateChangedEvent event) {
        PluginWrapper pluginWrapper = event.getPlugin();
        PluginState state = pluginWrapper.getPluginState();
        String pluginId = pluginWrapper.getPluginId();
        byte[] bytes = JsonUtils.obj2Bytes(state);
        return reactiveCustomClient.updateOneMeta(Plugin.class, pluginId, "state", bytes)
            .doOnSuccess(unused -> log.debug("Update plugin [{}] state to [{}]", pluginId, state));
    }
}
