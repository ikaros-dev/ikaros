package run.ikaros.server.plugin;

import jakarta.annotation.PostConstruct;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginWrapper;
import org.springframework.stereotype.Component;
import run.ikaros.server.core.custom.Plugin;
import run.ikaros.server.custom.ReactiveCustomClient;

@Component
public class PluginPropertiesEnablesInit {

    private final IkarosPluginManager ikarosPluginManager;
    private final PluginProperties pluginProperties;
    private final ReactiveCustomClient reactiveCustomClient;

    /**
     * Construct.
     */
    public PluginPropertiesEnablesInit(IkarosPluginManager ikarosPluginManager,
                                       PluginProperties pluginProperties,
                                       ReactiveCustomClient reactiveCustomClient) {
        this.ikarosPluginManager = ikarosPluginManager;
        this.pluginProperties = pluginProperties;
        this.reactiveCustomClient = reactiveCustomClient;
    }

    /**
     * init start enable plugin after construct.
     */
    @PostConstruct
    public void afterConstruct() {
        ikarosPluginManager.loadPlugins();
        for (String pluginId : pluginProperties.getEnabledPlugins()) {
            // start plugin
            ikarosPluginManager.startPlugin(pluginId);
            // read plugin.yml and save to database.
            savePluginDescToDatabase(pluginId);
        }
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
        reactiveCustomClient.findOne(Plugin.class, pluginId)
            .switchIfEmpty(reactiveCustomClient.create(plugin))
            .flatMap(reactiveCustomClient::update)
            .block();
    }

}
