package run.ikaros.server.plugin;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

/**
 * All plugin will extend this class to construct plugin or application.
 */
public class BasePlugin extends Plugin {
    public BasePlugin(PluginWrapper wrapper) {
        super(wrapper);
    }
}
