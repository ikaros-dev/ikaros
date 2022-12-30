package run.ikaros.server.plguin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: li-guohao
 */
public class PluginContextRegistry {
    private static final PluginContextRegistry INSTANCE = new PluginContextRegistry();
    private final Map<String, PluginApplicationContext> registry = new ConcurrentHashMap<>();

    public static PluginContextRegistry getInstance() {
        return INSTANCE;
    }

    private PluginContextRegistry() {}

    public void register(String pluginId, PluginApplicationContext context) {
        registry.put(pluginId, context);
    }

    public PluginApplicationContext remove(String pluginId) {
        return registry.remove(pluginId);
    }

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

    public List<PluginApplicationContext> getPluginApplicationContext() {
        return new ArrayList<>(registry.values());
    }
}
