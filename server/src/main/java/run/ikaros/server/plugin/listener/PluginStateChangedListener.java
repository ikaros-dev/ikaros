package run.ikaros.server.plugin.listener;

import static run.ikaros.server.plugin.listener.PluginDatabaseUtils.savePluginToDatabase;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginState;
import org.pf4j.PluginStateEvent;
import org.pf4j.PluginStateListener;
import org.pf4j.PluginWrapper;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.exception.NotFoundException;
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
        // 插件加载后，保存插件数据到数据库
        if (PluginState.RESOLVED.equals(state)) {
            savePluginToDatabase(pluginId, ikarosPluginManager, reactiveCustomClient)
                .doOnSuccess(plugin ->
                    log.debug("Save plugin info to database for id: [{}].", pluginId))
                .subscribeOn(Schedulers.boundedElastic()).subscribe();
        }

        // 更新插件状态
        reactiveCustomClient
            .updateOneMeta(Plugin.class, pluginId, "state",
                JsonUtils.obj2Bytes(state))
            .doOnSuccess(unused ->
                log.debug("Update plugin [{}] state to [{}]", pluginId, state))
            .onErrorResume(NotFoundException.class, e -> {
                log.warn("Skip first update plugin [{}] state.", pluginId);
                return Mono.empty();
            })
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe();
    }
}
