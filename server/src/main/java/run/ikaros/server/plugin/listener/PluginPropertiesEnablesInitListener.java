package run.ikaros.server.plugin.listener;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.setting.ConfigMap;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.infra.exception.NotFoundException;
import run.ikaros.api.plugin.custom.Plugin;
import run.ikaros.server.plugin.IkarosPluginManager;
import run.ikaros.server.plugin.PluginProperties;

@Slf4j
@Component
public class PluginPropertiesEnablesInitListener {

    private final IkarosPluginManager ikarosPluginManager;
    private final PluginProperties pluginProperties;
    private final ReactiveCustomClient reactiveCustomClient;

    /**
     * Construct.
     */
    public PluginPropertiesEnablesInitListener(IkarosPluginManager ikarosPluginManager,
                                               PluginProperties pluginProperties,
                                               ReactiveCustomClient reactiveCustomClient) {
        this.ikarosPluginManager = ikarosPluginManager;
        this.pluginProperties = pluginProperties;
        this.reactiveCustomClient = reactiveCustomClient;
    }

    /**
     * init start enable plugin after construct.
     */
    @EventListener(ApplicationReadyEvent.class)
    public Mono<Void> initialize() {
        // Load all plugins after application ready.
        ikarosPluginManager.loadPlugins();

        // Enable some plugins
        for (String pluginId : pluginProperties.getEnabledPlugins()) {
            PluginWrapper pluginWrapper = ikarosPluginManager.getPlugin(pluginId);
            if (pluginWrapper != null) {
                ikarosPluginManager.enablePlugin(pluginId);
            } else {
                log.warn("Skip enable plugin operate,"
                    + "Current config enabled plugin [{}] not exist.", pluginId);
            }
        }

        // Disable some plugin
        for (String disabledPlugin : pluginProperties.getDisabledPlugins()) {
            PluginWrapper pluginWrapper = ikarosPluginManager.getPlugin(disabledPlugin);
            if (pluginWrapper != null) {
                ikarosPluginManager.disablePlugin(disabledPlugin);
            } else {
                log.warn("Skip disable plugin operate,"
                    + "Current config disabled plugin [{}] not exist.", disabledPlugin);
            }

        }

        // Auto start plugin that state is RESOLVED.
        if (pluginProperties.isAutoStartPlugin()) {
            ikarosPluginManager.getPlugins().stream()
                .filter(
                    pluginWrapper -> PluginState.RESOLVED.equals(pluginWrapper.getPluginState()))
                .map(PluginWrapper::getPluginId)
                .forEach(pluginId -> reactiveCustomClient.findOne(Plugin.class, pluginId)
                    .filter(plugin -> PluginState.DISABLED != plugin.getState())
                    .onErrorResume(NotFoundException.class, e -> Mono.empty())
                    .subscribe(plugin -> ikarosPluginManager.startPlugin(plugin.getName())));
        }

        // Sync plugin records for manager and database.
        // Save all plugin db records after load all plugins.
        // return Flux.fromStream(ikarosPluginManager.getPlugins().stream())
        //     .map(PluginWrapper::getPluginId)
        //     .flatMap(pluginId ->
        //         PluginDatabaseUtils.savePluginToDatabase(pluginId, ikarosPluginManager,
        //             reactiveCustomClient))
        //     .collectList()
        //     .checkpoint("SaveAllPluginDatabase.")
        //     // Remove plugin records that manager none.
        //     .flatMapMany(plugins -> reactiveCustomClient.findAll(Plugin.class, null))
        return reactiveCustomClient.findAll(Plugin.class, null)
            .filter(plugin -> ikarosPluginManager.getPlugin(plugin.getName()) == null)
            .flatMap(reactiveCustomClient::delete)
            .doOnEach(pluginSignal -> {
                Plugin plugin = pluginSignal.get();
                if (plugin != null) {
                    log.debug("Remove plugin record that manager none for id: [{}]",
                        plugin.getName());
                }
            })
            .flatMap(plugin -> reactiveCustomClient.delete(ConfigMap.class, plugin.getName())
                .onErrorResume(NotFoundException.class, e -> Mono.empty()))
            .checkpoint("RemoveDatabasePluginThatManagerNone.")

            .thenMany(reactiveCustomClient.findAll(Plugin.class, null))
            .filter(plugin -> PluginState.DISABLED == plugin.getState())
            .map(plugin -> ikarosPluginManager.disablePlugin(plugin.getName()))
            .then();
    }

}
