package run.ikaros.server.plugin.listener;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.exception.NotFoundException;
import run.ikaros.api.plugin.custom.Plugin;
import run.ikaros.server.plugin.event.IkarosPluginStateChangedEvent;

@Slf4j
@Component
public class PluginStateChangedListener
    implements ApplicationListener<IkarosPluginStateChangedEvent> {
    private final ReactiveCustomClient reactiveCustomClient;

    public PluginStateChangedListener(ReactiveCustomClient reactiveCustomClient) {
        this.reactiveCustomClient = reactiveCustomClient;
    }

    @Override
    public void onApplicationEvent(IkarosPluginStateChangedEvent event) {
        PluginWrapper pluginWrapper = event.getPlugin();
        PluginState state = pluginWrapper.getPluginState();
        String pluginId = pluginWrapper.getPluginId();
        reactiveCustomClient.findOne(Plugin.class, pluginId)
            .switchIfEmpty(
                Mono.error(new NotFoundException("Not found for plugin id(name): " + pluginId)))
            .filter(plugin -> !state.equals(plugin.getState()))
            .map(plugin -> {
                plugin.setState(state);
                return plugin;
            })
            .flatMap(reactiveCustomClient::update)
            .then().block();
        log.debug("Update plugin [{}] state to [{}]", pluginId, state);
    }
}
