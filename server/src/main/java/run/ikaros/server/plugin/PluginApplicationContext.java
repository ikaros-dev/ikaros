package run.ikaros.server.plugin;

import org.springframework.context.support.GenericApplicationContext;

/**
 * The generic IOC container for plugins.
 * Load plugin classes by same plugin classloader.
 * All plugin application context parent is {@link SharedApplicationContext},
 * so plugin can access some beans that be shared by ikaros core.
 *
 *
 * @author li-guohao
 * @see PluginApplicationInitializer
 */
public class PluginApplicationContext extends GenericApplicationContext {
    private final String pluginId;

    public PluginApplicationContext(String pluginId) {
        super();
        this.pluginId = pluginId;
    }

    public String getPluginId() {
        return pluginId;
    }

}
