package run.ikaros.server.plugin;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

/**
 * <p>The generic IOC container for plugins.</p>
 * <p>Load plugin classes by same plugin classloader.</p>
 * <p>All plugin application context parent is {@link SharedApplicationContext},
 * so plugin can access some beans that be shared by ikaros core.
 * </p>
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

    public PluginApplicationContext(String pluginId, DefaultListableBeanFactory beanFactory) {
        super(beanFactory);
        this.pluginId = pluginId;
    }

    public PluginApplicationContext(String pluginId, ApplicationContext parent) {
        super(parent);
        this.pluginId = pluginId;
    }

    public PluginApplicationContext(String pluginId, ApplicationContext parent,
                                    DefaultListableBeanFactory beanFactory) {
        super(beanFactory, parent);
        this.pluginId = pluginId;
    }

    public String getPluginId() {
        return pluginId;
    }


}
