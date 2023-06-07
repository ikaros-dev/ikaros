package run.ikaros.server.plugin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Plugin application context registrar.</p>
 * <p>It contain a map
 * that key is the plugin id and value is application context of plugin.</p>
 *
 * @author li-guohao
 */
public class PluginApplicationContextRegistry {
    private static final PluginApplicationContextRegistry INSTANCE =
        new PluginApplicationContextRegistry();

    /**
     * <p>an plugin application context will be registered when the plugin is enabled.</p>
     * <p>an plugin application context will be deleted when the plugin is disabled.</p>
     */
    private final Map<String, PluginApplicationContext> registry = new ConcurrentHashMap<>();

    public static PluginApplicationContextRegistry getInstance() {
        return INSTANCE;
    }

    public void register(String pluginId, PluginApplicationContext context) {
        registry.put(pluginId, context);
    }

    public PluginApplicationContext remove(String pluginId) {
        return registry.remove(pluginId);
    }

    /**
     * Gets plugin application context by plugin id from registry map.
     *
     * @param pluginId plugin id
     * @return plugin application context
     * @throws IllegalArgumentException if plugin id not found in registry
     */
    public PluginApplicationContext getByPluginId(String pluginId) {
        PluginApplicationContext context = registry.get(pluginId);
        if (context == null) {
            throw new IllegalArgumentException(
                String.format("The plugin [%s] can not be found.", pluginId));
        }
        return context;
    }

    public boolean containsContext(String pluginId) {
        return registry.containsKey(pluginId);
    }

    public List<PluginApplicationContext> getPluginApplicationContexts() {
        return List.copyOf(registry.values());
    }
}
