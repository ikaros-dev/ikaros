package run.ikaros.server.plugin.listener;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginWrapper;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.server.plugin.IkarosPluginManager;
import run.ikaros.server.plugin.event.IkarosPluginLoadedEvent;

@Slf4j
@Component
public class IkarosPluginLoadEventListener {
    private final ReactiveCustomClient reactiveCustomClient;
    private final IkarosPluginManager ikarosPluginManager;

    public IkarosPluginLoadEventListener(ReactiveCustomClient reactiveCustomClient,
                                         IkarosPluginManager ikarosPluginManager) {
        this.reactiveCustomClient = reactiveCustomClient;
        this.ikarosPluginManager = ikarosPluginManager;
    }

    /**
     * Handle plugin delete event.
     */
    @EventListener(IkarosPluginLoadedEvent.class)
    public Mono<Void> handle(IkarosPluginLoadedEvent event) {
        PluginWrapper pluginWrapper = event.getPluginWrapper();
        String pluginId = pluginWrapper.getPluginId();
        return PluginDatabaseUtils.savePluginToDatabase(pluginId, ikarosPluginManager,
                reactiveCustomClient)
            .doOnSuccess(plugin ->
                log.debug("Save plugin info to database for id: [{}].", pluginId))
            .then();
    }
}
