package run.ikaros.server.plugin.listener;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginWrapper;
import reactor.core.publisher.Mono;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.exception.NotFoundException;
import run.ikaros.api.plugin.custom.Plugin;
import run.ikaros.server.plugin.IkarosPluginManager;
import run.ikaros.server.plugin.resource.BundleResourceUtils;

@Slf4j
public class PluginDatabaseUtils {

    /**
     * Save plugin to database.
     *
     * @param pluginId      plugin name(id)
     * @param pluginManager ikaros plugin manager
     * @param customClient  reactive custom client
     * @return database plugin record
     */
    public static Mono<Plugin> savePluginToDatabase(String pluginId,
                                                    IkarosPluginManager pluginManager,
                                                    ReactiveCustomClient customClient) {
        PluginWrapper pluginWrapper = pluginManager.getPlugin(pluginId);
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
        setPluginStaticResourceIfExists(pluginId, plugin, pluginManager);
        return customClient.findOne(Plugin.class, pluginId)
            .onErrorResume(NotFoundException.class, e -> customClient.create(plugin)
                .doOnSuccess(p -> log.debug("Create new plugin record for name: [{}].", pluginId)));
    }

    private static void setPluginStaticResourceIfExists(String pluginId, Plugin plugin,
                                                        IkarosPluginManager pluginManager) {
        // Console bundle entry js.
        String jsBundlePath = BundleResourceUtils.getJsBundlePath(pluginManager, pluginId);
        plugin.setEntry(jsBundlePath);
        // Console bundle style css.
        String cssBundlePath =
            BundleResourceUtils.getCssBundlePath(pluginManager, plugin.getName());
        plugin.setStylesheet(cssBundlePath);
    }


}
