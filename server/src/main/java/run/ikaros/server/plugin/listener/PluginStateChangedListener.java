package run.ikaros.server.plugin.listener;

import static run.ikaros.server.plugin.listener.PluginDatabaseUtils.savePluginToDatabase;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginState;
import org.pf4j.PluginStateEvent;
import org.pf4j.PluginStateListener;
import org.pf4j.PluginWrapper;
import reactor.core.scheduler.Schedulers;
import run.ikaros.api.custom.ReactiveCustomClient;
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
        // Mono.fromCallable(() -> {
        //     // 下面这行日志可以正常打印
        //     log.info("[info]update plugin state");
        //     log.debug("[debug]update plugin state");
        //     Logger logger = log;
        //     byte[] bytes = JsonUtils.obj2Bytes(state);
        //     return reactiveCustomClient.updateOneMeta(Plugin.class, pluginId, "state", bytes)
        //         .doOnError(throwable ->
        //             logger.warn("Skip first update plugin [{}] state.", pluginId, throwable))
        //         .onErrorResume(NotFoundException.class, e -> Mono.empty())
        //         // 这个日志无法正常打印
        //         .doOnSuccess(unused ->
        //             logger.debug("Update plugin [{}] state to [{}]", pluginId, state));
        // }).subscribeOn(Schedulers.boundedElastic()).subscribe();
        reactiveCustomClient
            .updateOneMeta(Plugin.class, pluginId, "state",
                JsonUtils.obj2Bytes(state))
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError(throwable ->
                log.warn("Skip first update plugin [{}] state.", pluginId, throwable))
            .doOnSuccess(unused ->
                log.debug("Update plugin [{}] state to [{}]", pluginId, state))
            .subscribe();
    }
}
