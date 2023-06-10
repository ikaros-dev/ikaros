package run.ikaros.server.plugin.listener;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.exception.NotFoundException;
import run.ikaros.api.plugin.custom.Plugin;
import run.ikaros.server.plugin.IkarosPluginManager;
import run.ikaros.server.plugin.PluginProperties;
import run.ikaros.server.plugin.resource.BundleResourceUtils;

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

        // Save all plugin db records after load all plugins.
        for (PluginWrapper pluginWrapper : ikarosPluginManager.getPlugins()) {
            String pluginId = pluginWrapper.getPluginId();
            savePluginDescToDatabase(pluginId);
        }

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
                .forEach(
                    pluginWrapper -> ikarosPluginManager.startPlugin(pluginWrapper.getPluginId()));
        }

        // Remove plugin records that manager none.
        return reactiveCustomClient.findAll(Plugin.class, null)
            .filter(plugin -> ikarosPluginManager.getPlugin(plugin.getName()) == null)
            .flatMap(reactiveCustomClient::delete)
            .then();
    }

    private void savePluginDescToDatabase(String pluginId) {
        PluginWrapper pluginWrapper = ikarosPluginManager.getPlugin(pluginId);
        PluginDescriptor pluginDescriptor = pluginWrapper.getDescriptor();
        Plugin plugin = new Plugin();
        plugin.setName(pluginId);
        plugin.setState(pluginWrapper.getPluginState());
        plugin.setDescription(pluginDescriptor.getPluginDescription());
        plugin.setVersion(pluginDescriptor.getVersion());
        plugin.setRequires(pluginDescriptor.getRequires());
        Plugin.Author author = new Plugin.Author();
        author.setName(pluginDescriptor.getProvider());
        plugin.setAuthor(author);
        plugin.setLicense(pluginDescriptor.getLicense());
        plugin.setLoadLocation(pluginWrapper.getPluginPath());
        setPluginStaticResourceIfExists(pluginId, plugin);
        reactiveCustomClient.findOne(Plugin.class, pluginId)
            .onErrorResume(NotFoundException.class, e -> reactiveCustomClient.create(plugin))
            .flatMap(reactiveCustomClient::update)
            .block();
    }


    private void setPluginStaticResourceIfExists(String pluginId, Plugin plugin) {
        // Console bundle entry js.
        String jsBundlePath = BundleResourceUtils.getJsBundlePath(ikarosPluginManager, pluginId);
        plugin.setEntry(jsBundlePath);
        // Console bundle style css.
        String cssBundlePath =
            BundleResourceUtils.getCssBundlePath(ikarosPluginManager, plugin.getName());
        plugin.setStylesheet(cssBundlePath);
    }


}
