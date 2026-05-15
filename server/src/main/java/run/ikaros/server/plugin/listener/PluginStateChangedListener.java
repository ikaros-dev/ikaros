package run.ikaros.server.plugin.listener;

import static run.ikaros.server.plugin.listener.PluginDatabaseUtils.savePluginToDatabase;

import java.nio.file.Files;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginState;
import org.pf4j.PluginStateEvent;
import org.pf4j.PluginStateListener;
import org.pf4j.PluginWrapper;
import reactor.core.publisher.Mono;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.infra.exception.NotFoundException;
import run.ikaros.api.plugin.custom.Plugin;
import run.ikaros.server.infra.utils.JsonUtils;
import run.ikaros.server.plugin.IkarosPluginManager;

@Slf4j
public class PluginStateChangedListener implements PluginStateListener {
    private final ReactiveCustomClient reactiveCustomClient;
    private IkarosPluginManager ikarosPluginManager;

    public PluginStateChangedListener(ReactiveCustomClient reactiveCustomClient) {
        this.reactiveCustomClient = reactiveCustomClient;
    }

    public void setIkarosPluginManager(IkarosPluginManager ikarosPluginManager) {
        this.ikarosPluginManager = ikarosPluginManager;
    }

    @Override
    public void pluginStateChanged(PluginStateEvent event) {
        PluginWrapper pluginWrapper = event.getPlugin();
        String pluginId = pluginWrapper.getPluginId();
        PluginState state = pluginWrapper.getPluginState();

        if (!Files.exists(pluginWrapper.getPluginPath())) {
            return;
        }

        Mono.just(Files.exists(pluginWrapper.getPluginPath()))
            .filter(exists -> exists)
            .filter(ex -> PluginState.RESOLVED.equals(state))
            .flatMap(eq -> savePluginToDatabase(pluginId, ikarosPluginManager, reactiveCustomClient)
                .doOnSuccess(plugin ->
                    log.debug("Save plugin info to database for id: [{}], plugin: [{}]", pluginId,
                        plugin))
            )

            .then(reactiveCustomClient.findOne(Plugin.class, pluginId))
            .onErrorResume(NotFoundException.class, e -> Mono.empty())
            .flatMap(plugin -> reactiveCustomClient.updateOneMeta(Plugin.class, pluginId, "state",
                    JsonUtils.obj2Bytes(state))
                .doOnSuccess(unused ->
                    log.debug("Update plugin [{}] state to [{}]", pluginId, state))
                .onErrorResume(NotFoundException.class, e -> {
                    log.warn("Skip first update plugin [{}] state.", pluginId);
                    return Mono.empty();
                }))
            .subscribe();
    }
}
