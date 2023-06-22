package run.ikaros.server.plugin.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.setting.ConfigMap;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.plugin.custom.Plugin;
import run.ikaros.server.plugin.event.IkarosPluginDeleteEvent;

@Slf4j
@Component
public class IkarosPluginDeleteEventListener {
    private final ReactiveCustomClient reactiveCustomClient;

    public IkarosPluginDeleteEventListener(ReactiveCustomClient reactiveCustomClient) {
        this.reactiveCustomClient = reactiveCustomClient;
    }

    /**
     * Handle plugin delete event.
     */
    @EventListener(IkarosPluginDeleteEvent.class)
    public Mono<Void> handle(IkarosPluginDeleteEvent event) {
        String pluginId = event.getPluginId();
        return reactiveCustomClient.delete(Plugin.class, pluginId)
            .doOnSuccess(plugin ->
                log.debug("Delete plugin record in db for pluginId [{}].", plugin.getName()))
            .then(reactiveCustomClient.delete(ConfigMap.class, pluginId)
                .doOnSuccess(configMap ->
                    log.debug("Delete plugin config map record in db for pluginId [{}].", pluginId))
                .then());
    }
}
