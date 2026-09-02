package run.ikaros.plugin;

import java.util.List;

public interface PluginExtensionRegistry {
    void register(PluginManifest manifest);
    void unregister(String pluginId);
    List<PluginExtension> find(String extensionPoint);
}
